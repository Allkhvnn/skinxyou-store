package skinxyou_backend.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.brand.Brand;
import skinxyou_backend.catalog.brand.BrandRepository;
import skinxyou_backend.catalog.category.Category;
import skinxyou_backend.catalog.category.CategoryRepository;
import skinxyou_backend.catalog.product.ImageSource;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.telegram.TelegramClient;
import skinxyou_backend.order.OrderRepository;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	BrandRepository brandRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	OrderRepository orderRepository;

	@MockitoBean
	TelegramClient telegramClient;

	private Brand cosrx;
	private Category face;
	private Category sun;

	@BeforeEach
	void seed() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		brandRepository.deleteAll();
		categoryRepository.deleteAll();

		cosrx = brandRepository.save(brand("b-1", "COSRX", "cosrx"));
		Brand anua = brandRepository.save(brand("b-2", "Anua", "anua"));
		face = categoryRepository.save(category("c-1", null, "Уход за лицом", "face"));
		sun = categoryRepository.save(category("c-2", "c-1", "Солнцезащита", "sun"));

		Product cleanser = product("p-1", "Snail Cleanser", "SKU-CLN", "8000", null, "5", cosrx, 1);
		cleanser.getCategories().add(face);
		cleanser.getImages().add(new ProductImage(cleanser, "https://img/second.jpg", 1, false, ImageSource.BILLZ));
		cleanser.getImages().add(new ProductImage(cleanser, "https://img/main.jpg", 0, true, ImageSource.BILLZ));
		productRepository.save(cleanser);

		Product sunCream = product("p-2", "Sun Cream SPF50", "SKU-SUN", "12000", "6000", "2", anua, 2);
		sunCream.getCategories().add(sun);
		productRepository.save(sunCream);

		Product outOfStock = product("p-3", "Toner 100% Pure", "SKU-TNR", "10000", null, "0", cosrx, 3);
		outOfStock.getCategories().add(face);
		productRepository.save(outOfStock);

		Product hidden = product("p-4", "Hidden Serum", "SKU-HID", "9000", null, "3", cosrx, 4);
		hidden.setHidden(true);
		productRepository.save(hidden);

		Product gone = product("p-5", "Removed Mask", "SKU-GONE", "9000", null, "3", cosrx, 5);
		gone.setAvailableInBillz(false);
		productRepository.save(gone);
	}

	@Test
	void listsOnlyVisibleProductsInStockFirstThenNewest() throws Exception {
		mockMvc.perform(get("/api/catalog/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug").value(contains("sun-cream-spf50", "snail-cleanser", "toner-100-pure")))
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.items[2].inStock").value(false))
				.andExpect(jsonPath("$.items[1].imageUrl").value("https://img/main.jpg"))
				.andExpect(jsonPath("$.items[1].brand.name").value("COSRX"))
				.andExpect(jsonPath("$.items[0].stock").doesNotExist())
				.andExpect(jsonPath("$.items[0].billzId").doesNotExist());
	}

	@Test
	void filtersByBrandCategoryAndStock() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("brand", "cosrx"))
				.andExpect(jsonPath("$.items[*].slug").value(containsInAnyOrder("snail-cleanser", "toner-100-pure")));
		mockMvc.perform(get("/api/catalog/products").param("category", "face").param("inStock", "true"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("snail-cleanser")));
	}

	@Test
	void priceFilterAndSortUseEffectivePromoPrice() throws Exception {
		// Sun cream costs 12000 but is on promo for 6000.
		mockMvc.perform(get("/api/catalog/products").param("maxPrice", "7000"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("sun-cream-spf50")));
		mockMvc.perform(get("/api/catalog/products").param("sort", "price_asc"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("sun-cream-spf50", "snail-cleanser", "toner-100-pure")));
		mockMvc.perform(get("/api/catalog/products").param("sort", "price_desc"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("snail-cleanser", "sun-cream-spf50", "toner-100-pure")));
	}

	@Test
	void sortsByUnitsSoldThenNewest() throws Exception {
		order("sun-cream-spf50", 1);
		order("snail-cleanser", 3);
		mockMvc.perform(get("/api/catalog/products").param("sort", "popular"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug").value(contains("snail-cleanser", "sun-cream-spf50", "toner-100-pure")));
	}

	private void order(String slug, int quantity) throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
						{"idempotencyKey":"%s","name":"Test","phone":"87011112233","deliveryMethod":"PICKUP",
						 "consent":true,"items":[{"slug":"%s","quantity":%d}]}
						""".formatted(UUID.randomUUID(), slug, quantity)))
				.andExpect(status().isCreated());
	}

	@Test
	void searchesByNameSkuBrandAndBarcodeCaseInsensitively() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("q", "snail"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("snail-cleanser")));
		mockMvc.perform(get("/api/catalog/products").param("q", "sku-sun"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("sun-cream-spf50")));
		mockMvc.perform(get("/api/catalog/products").param("q", "anua"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("sun-cream-spf50")));
		mockMvc.perform(get("/api/catalog/products").param("q", "460p-1"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("snail-cleanser")));
		// LIKE wildcards in user input are matched literally.
		mockMvc.perform(get("/api/catalog/products").param("q", "100%"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("toner-100-pure")));
		mockMvc.perform(get("/api/catalog/products").param("q", "%"))
				.andExpect(jsonPath("$.items[*].slug").value(contains("toner-100-pure")));
		mockMvc.perform(get("/api/catalog/products").param("q", "hidden"))
				.andExpect(jsonPath("$.items").value(hasSize(0)));
	}

	@Test
	void paginates() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("size", "2").param("page", "1"))
				.andExpect(jsonPath("$.items").value(hasSize(1)))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejectsInvalidParameters() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("size", "1000")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/catalog/products").param("sort", "random")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/catalog/products").param("minPrice", "abc")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/catalog/products").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
	}

	@Test
	void returnsProductDetailsWithMainImageFirstAndRelatedProducts() throws Exception {
		mockMvc.perform(get("/api/catalog/products/snail-cleanser"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Snail Cleanser"))
				.andExpect(jsonPath("$.sku").value("SKU-CLN"))
				.andExpect(jsonPath("$.price").value(8000))
				.andExpect(jsonPath("$.promoPrice").doesNotExist())
				.andExpect(jsonPath("$.inStock").value(true))
				.andExpect(jsonPath("$.categories[*].slug").value(contains("face")))
				.andExpect(jsonPath("$.images[*].url").value(contains("https://img/main.jpg", "https://img/second.jpg")))
				// Same brand and category first, then in-stock alternatives; hidden and removed products never appear.
				.andExpect(jsonPath("$.related[*].slug").value(contains("toner-100-pure", "sun-cream-spf50")));
	}

	@Test
	void hiddenRemovedAndUnknownProductsAreNotFound() throws Exception {
		mockMvc.perform(get("/api/catalog/products/hidden-serum")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/catalog/products/removed-mask")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/catalog/products/nope"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	void listsBrandsAndCategoriesWithVisibleProductCounts() throws Exception {
		mockMvc.perform(get("/api/catalog/brands"))
				.andExpect(jsonPath("$[*].slug").value(contains("anua", "cosrx")))
				.andExpect(jsonPath("$[1].productCount").value(2));
		mockMvc.perform(get("/api/catalog/brands/cosrx"))
				.andExpect(jsonPath("$.name").value("COSRX"));
		mockMvc.perform(get("/api/catalog/brands/unknown")).andExpect(status().isNotFound());

		mockMvc.perform(get("/api/catalog/categories"))
				.andExpect(jsonPath("$[*].slug").value(contains("sun", "face")))
				.andExpect(jsonPath("$[0].parentSlug").value("face"))
				.andExpect(jsonPath("$[1].productCount").value(2))
				.andExpect(jsonPath("$[*].slug").value(not(hasItem("hidden"))));
	}

	private static Brand brand(String billzId, String name, String slug) {
		Brand brand = new Brand();
		brand.setBillzId(billzId);
		brand.setName(name);
		brand.setSlug(slug);
		return brand;
	}

	private static Category category(String billzId, String parentBillzId, String name, String slug) {
		Category category = new Category();
		category.setBillzId(billzId);
		category.setParentBillzId(parentBillzId);
		category.setName(name);
		category.setSlug(slug);
		return category;
	}

	private static Product product(String billzId, String name, String sku, String price, String promoPrice,
			String stock, Brand brand, int updatedOrder) {
		Product p = new Product();
		p.setBillzId(billzId);
		p.setName(name);
		p.setSlug(skinxyou_backend.common.util.Slugs.slugify(name));
		p.setSku(sku);
		p.setBarcode("460" + billzId);
		p.setBrand(brand);
		p.setPrice(new BigDecimal(price));
		p.setPromoPrice(promoPrice == null ? null : new BigDecimal(promoPrice));
		p.setCurrency("KZT");
		p.setStock(new BigDecimal(stock));
		p.setBillzUpdatedAt(LocalDateTime.of(2026, 10, 1, 0, 0).plusDays(updatedOrder));
		p.setSyncedAt(Instant.now());
		return p;
	}
}
