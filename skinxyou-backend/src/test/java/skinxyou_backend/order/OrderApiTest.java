package skinxyou_backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.telegram.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	TransactionTemplate tx;

	@MockitoBean
	TelegramClient telegramClient;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		productRepository.save(product("p-1", "Toner", "toner", "14000", null, "5"));
		productRepository.save(product("p-2", "Sun Cream", "sun-cream", "12000", "9000", "2"));
		productRepository.save(product("p-3", "Sold Out Mask", "sold-out-mask", "5000", null, "0"));
		Product hidden = product("p-4", "Hidden", "hidden", "5000", null, "3");
		hidden.setHidden(true);
		productRepository.save(hidden);
	}

	@Test
	void placesCourierOrderWithServerSideTotalsAndPaymentPending() throws Exception {
		place(order(UUID.randomUUID(), "COURIER", "ул. Абая, 1, кв. 2", item("toner", 2), item("sun-cream", 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.number").value(org.hamcrest.Matchers.startsWith("SX-")))
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.paymentStatus").value("PENDING"))
				.andExpect(jsonPath("$.paymentMethod").value("KASPI_INVOICE"))
				// 2 × 14 000 + promo 9 000 = 37 000 < 50 000 → courier costs 1 000
				.andExpect(jsonPath("$.itemsTotal").value(37000))
				.andExpect(jsonPath("$.deliveryFee").value(1000))
				.andExpect(jsonPath("$.total").value(38000))
				.andExpect(jsonPath("$.items[1].unitPrice").value(9000));

		tx.executeWithoutResult(s -> {
			Order order = orderRepository.findAll().getFirst();
			assertThat(order.getCustomerPhone()).isEqualTo("+77760050625");
			assertThat(order.getDeliveryAddress()).isEqualTo("ул. Абая, 1, кв. 2");
			assertThat(order.getHistory()).extracting(OrderStatusHistory::getToStatus).containsExactly("NEW", "PENDING");
		});
	}

	@Test
	void courierIsFreeFromThresholdAndPickupIsAlwaysFree() throws Exception {
		place(order(UUID.randomUUID(), "COURIER", "Абая 1", item("toner", 4)))
				.andExpect(jsonPath("$.itemsTotal").value(56000))
				.andExpect(jsonPath("$.deliveryFee").value(0));
		place(order(UUID.randomUUID(), "PICKUP", null, item("toner", 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.deliveryFee").value(0))
				.andExpect(jsonPath("$.deliveryAddress").doesNotExist());
	}

	@Test
	void sameIdempotencyKeyReturnsTheExistingOrderInsteadOfADuplicate() throws Exception {
		UUID key = UUID.randomUUID();
		String body = order(key, "PICKUP", null, item("toner", 1));
		String number = com.jayway.jsonpath.JsonPath.read(
				place(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.number");

		place(body)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.number").value(number))
				.andExpect(jsonPath("$.total").value(14000));
		assertThat(orderRepository.count()).isEqualTo(1);
	}

	@Test
	void pricePaidIsSnapshottedAndNotAffectedByLaterCatalogChanges() throws Exception {
		place(order(UUID.randomUUID(), "PICKUP", null, item("toner", 1))).andExpect(status().isCreated());

		tx.executeWithoutResult(s -> productRepository.findAllBySlugInAndHiddenFalseAndAvailableInBillzTrue(
				java.util.List.of("toner")).getFirst().setPrice(new BigDecimal("99999")));

		tx.executeWithoutResult(s -> {
			OrderItem item = orderRepository.findAll().getFirst().getItems().getFirst();
			assertThat(item.getUnitPrice()).isEqualByComparingTo("14000");
			assertThat(item.getProductName()).isEqualTo("Toner");
		});
	}

	@Test
	void rejectsInsufficientStockAndUnavailableProductsWithDetails() throws Exception {
		place(order(UUID.randomUUID(), "PICKUP", null, item("toner", 6), item("sold-out-mask", 1), item("hidden", 1),
				item("no-such-product", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.problems[?(@.slug == 'toner')].reason").value(hasItem("INSUFFICIENT_STOCK")))
				.andExpect(jsonPath("$.problems[?(@.slug == 'toner')].available").value(hasItem(5)))
				.andExpect(jsonPath("$.problems[?(@.slug == 'sold-out-mask')].available").value(hasItem(0)))
				.andExpect(jsonPath("$.problems[?(@.slug == 'hidden')].reason").value(hasItem("NOT_AVAILABLE")))
				.andExpect(jsonPath("$.problems[?(@.slug == 'no-such-product')].reason").value(hasItem("NOT_AVAILABLE")));

		assertThat(orderRepository.count()).isZero();
		verify(telegramClient, never()).send(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void validatesCustomerInput() throws Exception {
		place(order(UUID.randomUUID(), "COURIER", " ", item("toner", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("addressPresentForCourier")));
		place(order(UUID.randomUUID(), "PICKUP", null, false, "+7 776 005 06 25", item("toner", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(hasItem("consent")));
		place(order(UUID.randomUUID(), "PICKUP", null, true, "12345", item("toner", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("+7")));
		place("{\"items\":[]}").andExpect(status().isBadRequest());
		assertThat(orderRepository.count()).isZero();
	}

	@Test
	void notifiesManagerInTelegramAfterTheOrderIsSaved() throws Exception {
		place(order(UUID.randomUUID(), "COURIER", "Абая 1", item("sun-cream", 1))).andExpect(status().isCreated());

		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(telegramClient, timeout(5000)).send(message.capture());
		assertThat(message.getValue())
				.contains("Новый заказ SX-")
				.contains("+77760050625")
				.contains("Sun Cream (SKU-p-2) × 1 — 9 000 ₸")
				.contains("Доставка: курьер, Абая 1")
				.contains("Итого: 10 000 ₸");
	}

	@Test
	void quoteReturnsCurrentPricesAndProblemsWithoutCreatingAnOrder() throws Exception {
		mockMvc.perform(post("/api/checkout/quote").contentType(MediaType.APPLICATION_JSON).content("""
						{"deliveryMethod":"COURIER","items":[{"slug":"toner","quantity":1},{"slug":"sold-out-mask","quantity":1}]}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lines[0].name").value("Toner"))
				.andExpect(jsonPath("$.itemsTotal").value(19000))
				.andExpect(jsonPath("$.deliveryFee").value(1000))
				.andExpect(jsonPath("$.problems[0].slug").value("sold-out-mask"));
		assertThat(orderRepository.count()).isZero();

		mockMvc.perform(get("/api/checkout/options"))
				.andExpect(jsonPath("$.courierFee").value(1000))
				.andExpect(jsonPath("$.freeCourierFrom").value(50000))
				.andExpect(jsonPath("$.pickupAddress").value(org.hamcrest.Matchers.containsString("Курмангазы")));
	}

	@Test
	void allowsTheStorefrontOriginViaCors() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/orders")
						.header("Origin", "http://localhost:3000")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/orders")
						.header("Origin", "https://evil.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	private ResultActions place(String body) throws Exception {
		return mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String order(UUID key, String delivery, String address, String... items) {
		return order(key, delivery, address, true, "8 776 005 06 25", items);
	}

	private static String order(UUID key, String delivery, String address, boolean consent, String phone,
			String... items) {
		return """
				{"idempotencyKey":"%s","name":"Айгерим","phone":"%s","deliveryMethod":"%s",%s"consent":%s,
				 "items":[%s]}
				""".formatted(key, phone, delivery, address == null ? "" : "\"address\":\"" + address + "\",", consent,
				String.join(",", items));
	}

	private static String item(String slug, int quantity) {
		return "{\"slug\":\"%s\",\"quantity\":%d}".formatted(slug, quantity);
	}

	private static Product product(String billzId, String name, String slug, String price, String promo, String stock) {
		Product p = new Product();
		p.setBillzId(billzId);
		p.setName(name);
		p.setSlug(slug);
		p.setSku("SKU-" + billzId);
		p.setPrice(new BigDecimal(price));
		p.setPromoPrice(promo == null ? null : new BigDecimal(promo));
		p.setCurrency("KZT");
		p.setStock(new BigDecimal(stock));
		p.setSyncedAt(Instant.now());
		return p;
	}
}
