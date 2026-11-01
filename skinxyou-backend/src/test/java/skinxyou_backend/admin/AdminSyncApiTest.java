package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.billz.client.BillzClient;
import skinxyou_backend.integration.billz.dto.BillzProduct;
import skinxyou_backend.integration.billz.dto.BillzProductPage;
import skinxyou_backend.integration.billz.sync.BillzSyncRunRepository;
import skinxyou_backend.integration.telegram.TelegramClient;
import skinxyou_backend.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "billz.sync.shop-id=shop-main")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminSyncApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	AdminUserRepository adminUserRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	BillzSyncRunRepository runRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	OrderRepository orderRepository;

	@MockitoBean
	BillzClient billzClient;

	@MockitoBean
	TelegramClient telegramClient;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		runRepository.deleteAll();
		productRepository.deleteAll();
		if (adminUserRepository.findByUsername("manager").isEmpty()) {
			AdminUser user = new AdminUser();
			user.setUsername("manager");
			user.setDisplayName("Менеджер");
			user.setPasswordHash(passwordEncoder.encode("manager-password"));
			user.setRole(AdminRole.MANAGER);
			user.setCreatedAt(Instant.now());
			adminUserRepository.save(user);
		}
		BillzProduct product = new BillzProduct("p-1", "", "Toner", "SKU-1", "460", "", "", "", false,
				"2026-10-30 11:49:25", null, null, List.of(),
				List.of(new BillzProduct.ShopStock("shop-main", "Main", new BigDecimal("3"))),
				List.of(new BillzProduct.ShopPrice("shop-main", "Main", new BigDecimal("1000"), "KZT", null, null)),
				List.of(), List.of(), null);
		when(billzClient.getProducts(anyInt(), anyInt(), any())).thenAnswer(inv -> inv.<Integer>getArgument(0) == 1
				? new BillzProductPage(1, List.of(product))
				: new BillzProductPage(1, List.of()));
	}

	@Test
	void syncEndpointsRequireStaffToken() throws Exception {
		mockMvc.perform(get("/api/admin/sync/status")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/admin/sync/run").contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"FULL\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void manualFullSyncRunsInTheBackgroundAndShowsUpInStatusAndLog() throws Exception {
		String token = bearer();
		mockMvc.perform(get("/api/admin/sync/status").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.running").value(false))
				.andExpect(jsonPath("$.stale").value(true))
				.andExpect(jsonPath("$.lastRun").doesNotExist())
				.andExpect(jsonPath("$.catalog.total").value(0));

		mockMvc.perform(post("/api/admin/sync/run").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"FULL\"}"))
				.andExpect(status().isAccepted());

		long deadline = System.currentTimeMillis() + 10_000;
		while (runRepository.findAll().stream().noneMatch(r -> r.getFinishedAt() != null)
				&& System.currentTimeMillis() < deadline) {
			Thread.sleep(100);
		}

		mockMvc.perform(get("/api/admin/sync/status").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.lastRun.type").value("FULL"))
				.andExpect(jsonPath("$.lastRun.status").value("SUCCESS"))
				.andExpect(jsonPath("$.lastSuccessAt").exists())
				.andExpect(jsonPath("$.stale").value(false))
				.andExpect(jsonPath("$.catalog.total").value(1))
				.andExpect(jsonPath("$.catalog.onSite").value(1))
				.andExpect(jsonPath("$.catalog.inStock").value(1));
		mockMvc.perform(get("/api/admin/sync/runs").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[0].productsCreated").value(1))
				.andExpect(jsonPath("$.items[0].durationMs").isNumber());
		assertThat(runRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsUnknownSyncType() throws Exception {
		mockMvc.perform(post("/api/admin/sync/run").header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EVERYTHING\"}"))
				.andExpect(status().isBadRequest());
	}

	private String bearer() throws Exception {
		String body = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"manager\",\"password\":\"manager-password\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}
}
