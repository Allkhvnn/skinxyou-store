package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.telegram.TelegramClient;
import skinxyou_backend.order.OrderRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminAnalyticsApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	AdminUserRepository adminUserRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoBean
	TelegramClient telegramClient;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		if (adminUserRepository.findByUsername("analyst").isEmpty()) {
			AdminUser user = new AdminUser();
			user.setUsername("analyst");
			user.setDisplayName("Analyst");
			user.setPasswordHash(passwordEncoder.encode("analyst-password"));
			user.setRole(AdminRole.MANAGER);
			user.setCreatedAt(Instant.now());
			adminUserRepository.save(user);
		}
		productRepository.save(product("p-1", "Toner", "toner", "10000"));
		productRepository.save(product("p-2", "Mask", "mask", "2000"));
	}

	@Test
	void reportsRevenueFromPaidOrdersOnlyAndExcludesCancelled() throws Exception {
		String token = bearer();
		String paid = place("toner", 2, "PICKUP");      // 20 000, will be paid
		place("mask", 1, "COURIER");                     // 2 000 + 1 000 delivery, awaiting payment
		String cancelled = place("toner", 5, "PICKUP");  // cancelled: not counted anywhere but "cancelled"

		send(token, "/api/admin/orders/" + paid + "/payment-status", "{\"paymentStatus\":\"PAID\",\"version\":0}");
		send(token, "/api/admin/orders/" + cancelled + "/status", "{\"status\":\"CANCELLED\",\"version\":0}");

		mockMvc.perform(get("/api/admin/analytics").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.orders").value(2))
				.andExpect(jsonPath("$.summary.paidOrders").value(1))
				.andExpect(jsonPath("$.summary.cancelledOrders").value(1))
				.andExpect(jsonPath("$.summary.revenue").value(20000))
				.andExpect(jsonPath("$.summary.awaitingPayment").value(3000))
				.andExpect(jsonPath("$.summary.averageCheck").value(20000))
				.andExpect(jsonPath("$.summary.courierOrders").value(1))
				.andExpect(jsonPath("$.summary.pickupOrders").value(1))
				.andExpect(jsonPath("$.days.length()").value(30))
				.andExpect(jsonPath("$.days[29].orders").value(2))
				.andExpect(jsonPath("$.days[29].revenue").value(20000))
				.andExpect(jsonPath("$.topProducts[0].slug").value("toner"))
				.andExpect(jsonPath("$.topProducts[0].quantity").value(2))
				.andExpect(jsonPath("$.topProducts[1].slug").value("mask"));
	}

	@Test
	void validatesPeriodAndRequiresStaff() throws Exception {
		mockMvc.perform(get("/api/admin/analytics")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/analytics").param("from", "2026-05-01").param("to", "2026-04-01")
				.header(HttpHeaders.AUTHORIZATION, bearer())).andExpect(status().isBadRequest());
	}

	private String place(String slug, int quantity, String delivery) throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
						{"idempotencyKey":"%s","name":"Test","phone":"87011112233","deliveryMethod":"%s",
						 "address":"Абая 1","consent":true,"items":[{"slug":"%s","quantity":%d}]}
						""".formatted(UUID.randomUUID(), delivery, slug, quantity)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.number");
	}

	private void send(String token, String path, String json) throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
				.header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());
	}

	private static Product product(String billzId, String name, String slug, String price) {
		Product p = new Product();
		p.setBillzId(billzId);
		p.setName(name);
		p.setSlug(slug);
		p.setPrice(new BigDecimal(price));
		p.setCurrency("KZT");
		p.setStock(new BigDecimal("10"));
		p.setSyncedAt(Instant.now());
		return p;
	}

	private String bearer() throws Exception {
		String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"analyst\",\"password\":\"analyst-password\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}
}
