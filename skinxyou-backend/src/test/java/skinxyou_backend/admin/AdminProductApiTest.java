package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.time.Instant;

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
import org.springframework.test.web.servlet.MockMvc;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.product.ImageSource;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminProductApiTest {

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

	private Long tonerId;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		if (adminUserRepository.findByUsername("editor").isEmpty()) {
			AdminUser user = new AdminUser();
			user.setUsername("editor");
			user.setDisplayName("Editor");
			user.setPasswordHash(passwordEncoder.encode("editor-password"));
			user.setRole(AdminRole.MANAGER);
			user.setCreatedAt(Instant.now());
			adminUserRepository.save(user);
		}
		Product toner = product("p-1", "Toner", "toner");
		toner.getImages().add(new ProductImage(toner, "https://img/1.jpg", 0, true, ImageSource.BILLZ));
		tonerId = productRepository.save(toner).getId();
		Product cream = product("p-2", "Cream", "cream");
		cream.setDescription("Has text");
		productRepository.save(cream);
	}

	@Test
	void requiresStaffToken() throws Exception {
		mockMvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
	}

	@Test
	void listsAndFiltersProducts() throws Exception {
		String token = bearer();
		mockMvc.perform(get("/api/admin/products").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].name").value(contains("Cream", "Toner")));
		mockMvc.perform(get("/api/admin/products").param("filter", "NO_DESCRIPTION").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].name").value(contains("Toner")));
		mockMvc.perform(get("/api/admin/products").param("filter", "NO_PHOTO").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].name").value(contains("Cream")));
		mockMvc.perform(get("/api/admin/products").param("q", "SKU-p-1").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$.items[*].name").value(contains("Toner")));
	}

	@Test
	void savesSiteContentAndHidesProductFromStorefront() throws Exception {
		String token = bearer();
		mockMvc.perform(put("/api/admin/products/" + tonerId + "/content").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"description":"  Мягкий тонер для лица  ","seoTitle":"Тонер купить","seoDescription":" ","hidden":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Мягкий тонер для лица"))
				.andExpect(jsonPath("$.seoDescription").doesNotExist())
				.andExpect(jsonPath("$.hidden").value(true))
				.andExpect(jsonPath("$.name").value("Toner"));

		mockMvc.perform(get("/api/catalog/products/toner")).andExpect(status().isNotFound());
		assertThat(productRepository.findById(tonerId).orElseThrow().getPrice()).isEqualByComparingTo("5000");
	}

	@Test
	void rejectsTooLongContentAndUnknownProduct() throws Exception {
		String token = bearer();
		mockMvc.perform(put("/api/admin/products/" + tonerId + "/content").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"seoTitle\":\"" + "x".repeat(256) + "\",\"hidden\":false}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/products/999999").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isNotFound());
	}

	private static Product product(String billzId, String name, String slug) {
		Product p = new Product();
		p.setBillzId(billzId);
		p.setName(name);
		p.setSlug(slug);
		p.setSku("SKU-" + billzId);
		p.setPrice(new BigDecimal("5000"));
		p.setCurrency("KZT");
		p.setStock(new BigDecimal("2"));
		p.setSyncedAt(Instant.now());
		return p;
	}

	private String bearer() throws Exception {
		String body = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"editor\",\"password\":\"editor-password\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}
}
