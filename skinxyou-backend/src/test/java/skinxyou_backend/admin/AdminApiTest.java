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
import org.springframework.test.web.servlet.ResultActions;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.telegram.TelegramClient;
import skinxyou_backend.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"admin.bootstrap-username=Owner",
		"admin.bootstrap-password=bootstrap-secret-1",
		"admin.bootstrap-display-name=Владелец"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminApiTest {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	AdminUserRepository adminUserRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoBean
	TelegramClient telegramClient;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		adminUserRepository.findByUsername("aigerim").ifPresent(adminUserRepository::delete);
		AdminUser manager = new AdminUser();
		manager.setUsername("aigerim");
		manager.setDisplayName("Айгерим");
		manager.setPasswordHash(passwordEncoder.encode(PASSWORD));
		manager.setRole(AdminRole.MANAGER);
		manager.setCreatedAt(Instant.now());
		adminUserRepository.save(manager);

		Product product = new Product();
		product.setBillzId("p-1");
		product.setName("Toner");
		product.setSlug("toner");
		product.setPrice(new BigDecimal("14000"));
		product.setCurrency("KZT");
		product.setStock(new BigDecimal("10"));
		product.setSyncedAt(Instant.now());
		productRepository.save(product);
	}

	@Test
	void bootstrapCreatesTheFirstAdminFromConfiguration() throws Exception {
		AdminUser owner = adminUserRepository.findByUsername("owner").orElseThrow();
		assertThat(owner.getRole()).isEqualTo(AdminRole.ADMIN);
		assertThat(owner.getPasswordHash()).doesNotContain("bootstrap-secret-1");
		mockMvc.perform(login("OWNER", "bootstrap-secret-1")).andExpect(status().isOk());
	}

	@Test
	void loginRejectsWrongPasswordUnknownUserAndInactiveAccount() throws Exception {
		mockMvc.perform(login("aigerim", "wrong")).andExpect(status().isUnauthorized());
		mockMvc.perform(login("nobody", PASSWORD)).andExpect(status().isUnauthorized());

		AdminUser manager = adminUserRepository.findByUsername("aigerim").orElseThrow();
		manager.setActive(false);
		adminUserRepository.save(manager);
		mockMvc.perform(login("aigerim", PASSWORD)).andExpect(status().isUnauthorized());
	}

	@Test
	void adminApiRequiresAValidToken() throws Exception {
		mockMvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/orders").header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/orders").header(HttpHeaders.AUTHORIZATION, bearer())).andExpect(status().isOk());
	}

	@Test
	void listsAndFiltersOrdersNewestFirstWithCustomerContacts() throws Exception {
		String first = placeOrder("Айгерим", "+7 701 111 22 33");
		String second = placeOrder("Данияр", "+7 702 444 55 66");
		String token = bearer();

		mockMvc.perform(get("/api/admin/orders").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].number").value(contains(second, first)))
				.andExpect(jsonPath("$.items[0].customerPhone").value("+77024445566"))
				.andExpect(jsonPath("$.items[0].status").value("NEW"));
		mockMvc.perform(get("/api/admin/orders").param("q", "4445566").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].number").value(contains(second)));
		mockMvc.perform(get("/api/admin/orders").param("q", "айгерим").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].number").value(contains(first)));
		mockMvc.perform(get("/api/admin/orders").param("status", "CONFIRMED").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items").value(hasSize(0)));
	}

	@Test
	void managerMovesOrderThroughTheWorkflowAndMarksItPaid() throws Exception {
		String number = placeOrder("Айгерим", "87011112233");
		String token = bearer();

		mockMvc.perform(get("/api/admin/orders/" + number).header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.allowedStatuses").value(contains("CONFIRMED", "CANCELLED")))
				.andExpect(jsonPath("$.allowedPaymentStatuses").value(contains("PAID", "CANCELLED")));

		long version = changeStatus(number, "CONFIRMED", 0, token).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				// Courier order: next step is SHIPPED, not READY_FOR_PICKUP.
				.andExpect(jsonPath("$.allowedStatuses").value(contains("SHIPPED", "CANCELLED")))
				.andReturn().getResponse().getContentAsString().transform(this::version);

		changePayment(number, "PAID", "Kaspi, чек 123", version, token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("PAID"))
				.andExpect(jsonPath("$.history[-1].type").value("PAYMENT"))
				.andExpect(jsonPath("$.history[-1].fromStatus").value("PENDING"))
				.andExpect(jsonPath("$.history[-1].toStatus").value("PAID"))
				.andExpect(jsonPath("$.history[-1].changedBy").value("Айгерим (aigerim)"))
				.andExpect(jsonPath("$.history[-1].note").value("Kaspi, чек 123"));
	}

	@Test
	void rejectsIllegalTransitionsAndStaleVersions() throws Exception {
		String number = placeOrder("Айгерим", "87011112233");
		String token = bearer();

		changeStatus(number, "COMPLETED", 0, token).andExpect(status().isConflict());
		changeStatus(number, "CANCELLED", 0, token).andExpect(status().isOk());
		// Cancelled is final.
		changeStatus(number, "CONFIRMED", 1, token).andExpect(status().isConflict());
		// Someone else already changed the order: version 0 is stale.
		changePayment(number, "PAID", null, 0, token)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("другой сотрудник")));
		mockMvc.perform(get("/api/admin/orders/SX-999999").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isNotFound());
	}

	private String placeOrder(String name, String phone) throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
						{"idempotencyKey":"%s","name":"%s","phone":"%s","deliveryMethod":"COURIER","address":"Абая 1",
						 "consent":true,"items":[{"slug":"toner","quantity":1}]}
						""".formatted(UUID.randomUUID(), name, phone)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.number");
	}

	private ResultActions changeStatus(String number, String target, long version, String token) throws Exception {
		return mockMvc.perform(post("/api/admin/orders/" + number + "/status")
				.header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"%s\",\"version\":%d}".formatted(target, version)));
	}

	private ResultActions changePayment(String number, String target, String note, long version, String token)
			throws Exception {
		return mockMvc.perform(post("/api/admin/orders/" + number + "/payment-status")
				.header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"paymentStatus\":\"%s\",\"note\":%s,\"version\":%d}"
						.formatted(target, note == null ? "null" : "\"" + note + "\"", version)));
	}

	private long version(String json) {
		return ((Number) JsonPath.read(json, "$.version")).longValue();
	}

	private String bearer() throws Exception {
		String body = mockMvc.perform(login("aigerim", PASSWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Айгерим"))
				.andExpect(jsonPath("$.role").value("MANAGER"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}

	private static org.springframework.test.web.servlet.RequestBuilder login(String username, String password) {
		return post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
	}
}
