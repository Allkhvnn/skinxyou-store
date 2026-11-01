package skinxyou_backend.integration.billz.sync;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.catalog.brand.BrandRepository;
import skinxyou_backend.catalog.category.CategoryRepository;
import skinxyou_backend.catalog.product.ImageSource;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.billz.client.BillzApiException;
import skinxyou_backend.integration.billz.client.BillzClient;
import skinxyou_backend.integration.billz.dto.BillzProduct;
import skinxyou_backend.integration.billz.dto.BillzProductPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "billz.sync.shop-id=" + BillzCatalogSyncServiceTest.SHOP)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, BillzCatalogSyncServiceTest.ClockConfig.class})
class BillzCatalogSyncServiceTest {

	static final String SHOP = "shop-main";
	private static final String OTHER_SHOP = "shop-other";

	@MockitoBean
	BillzClient billzClient;

	@Autowired
	BillzCatalogSyncService syncService;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	BrandRepository brandRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	BillzSyncRunRepository runRepository;

	@Autowired
	TransactionTemplate tx;

	@Autowired
	MutableClock clock;

	@Autowired
	ApplicationEvents events;

	@BeforeEach
	void cleanDatabase() {
		productRepository.deleteAll();
		brandRepository.deleteAll();
		categoryRepository.deleteAll();
		runRepository.deleteAll();
	}

	@Test
	void fullSyncCreatesProductsWithBrandCategoryPriceStockAndImages() {
		billzReturns(
				product("p-1", "Крем для лица", "SKU-1", "8900", "7")
						.brand("b-1", "COSRX").category("c-1", "Уход за лицом").photo("https://cdn/1.jpg"),
				product("p-2", "Toner", "SKU-2", "5000", "0"));

		BillzSyncRun run = syncService.runFull().orElseThrow();

		assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);
		assertThat(run.getProductsFetched()).isEqualTo(2);
		assertThat(run.getProductsCreated()).isEqualTo(2);
		inTx(() -> {
			Product cream = byBillzId("p-1");
			assertThat(cream.getName()).isEqualTo("Крем для лица");
			assertThat(cream.getSlug()).isEqualTo("krem-dlya-litsa");
			assertThat(cream.getPrice()).isEqualByComparingTo("8900");
			assertThat(cream.getCurrency()).isEqualTo("KZT");
			assertThat(cream.getStock()).isEqualByComparingTo("7");
			assertThat(cream.getBrand().getName()).isEqualTo("COSRX");
			assertThat(cream.getBrand().getSlug()).isEqualTo("cosrx");
			assertThat(cream.getCategories()).extracting("name").containsExactly("Уход за лицом");
			assertThat(cream.getImages()).extracting(ProductImage::getUrl).containsExactly("https://cdn/1.jpg");

			Product toner = byBillzId("p-2");
			assertThat(toner.getBrand()).isNull();
			assertThat(toner.getCategories()).isEmpty();
			assertThat(toner.getStock()).isEqualByComparingTo("0");
		});
	}

	@Test
	void resyncUpdatesBillzFieldsWithoutDuplicatesAndKeepsSiteContent() {
		billzReturns(product("p-1", "Cleanser", "SKU-1", "8900", "7").photo("https://cdn/old.jpg"));
		syncService.runFull();

		inTx(() -> {
			Product p = byBillzId("p-1");
			p.setSlug("custom-slug");
			p.setDescription("Our description");
			p.setSeoTitle("SEO title");
			p.setHidden(true);
			p.getImages().add(new ProductImage(p, "https://r2/site.jpg", 5, false, ImageSource.SITE));
		});

		clock.advance(Duration.ofMinutes(15));
		billzReturns(product("p-1", "Cleanser 150ml", "SKU-1", "9900", "3").photo("https://cdn/new.jpg"));
		BillzSyncRun run = syncService.runDelta().orElseThrow();

		assertThat(run.getType()).isEqualTo(SyncType.DELTA);
		assertThat(run.getProductsCreated()).isZero();
		assertThat(run.getProductsUpdated()).isEqualTo(1);
		assertThat(productRepository.count()).isEqualTo(1);
		inTx(() -> {
			Product p = byBillzId("p-1");
			assertThat(p.getName()).isEqualTo("Cleanser 150ml");
			assertThat(p.getPrice()).isEqualByComparingTo("9900");
			assertThat(p.getStock()).isEqualByComparingTo("3");
			assertThat(p.getSlug()).isEqualTo("custom-slug");
			assertThat(p.getDescription()).isEqualTo("Our description");
			assertThat(p.getSeoTitle()).isEqualTo("SEO title");
			assertThat(p.isHidden()).isTrue();
			assertThat(p.getImages()).extracting(ProductImage::getUrl)
					.containsExactlyInAnyOrder("https://cdn/new.jpg", "https://r2/site.jpg");
		});
	}

	@Test
	void fullSyncDelistsProductsMissingFromBillzAndRelistsThemWhenTheyReturn() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"), product("p-2", "B", "SKU-2", "100", "1"));
		syncService.runFull();

		clock.advance(Duration.ofDays(1));
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));
		BillzSyncRun run = syncService.runFull().orElseThrow();

		assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);
		assertThat(run.getProductsDeactivated()).isEqualTo(1);
		assertThat(byBillzId("p-2").isAvailableInBillz()).isFalse();
		assertThat(byBillzId("p-1").isAvailableInBillz()).isTrue();

		clock.advance(Duration.ofMinutes(15));
		billzReturns(product("p-2", "B", "SKU-2", "100", "1"));
		syncService.runDelta();

		assertThat(byBillzId("p-2").isAvailableInBillz()).isTrue();
	}

	@Test
	void incompleteExportFailsAndDelistsNothing() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"), product("p-2", "B", "SKU-2", "100", "1"));
		syncService.runFull();

		clock.advance(Duration.ofDays(1));
		// BILLZ reports 2 products but the export only yields one.
		when(billzClient.getProducts(anyInt(), anyInt(), any()))
				.thenAnswer(inv -> inv.<Integer>getArgument(0) == 1
						? new BillzProductPage(2, List.of(product("p-1", "A", "SKU-1", "100", "1").build()))
						: new BillzProductPage(2, List.of()));
		BillzSyncRun run = syncService.runFull().orElseThrow();

		assertThat(run.getStatus()).isEqualTo(SyncStatus.FAILED);
		assertThat(run.getErrorMessage()).contains("Incomplete BILLZ export");
		assertThat(byBillzId("p-2").isAvailableInBillz()).isTrue();
	}

	@Test
	void refusesToDelistMostOfTheCatalog() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"), product("p-2", "B", "SKU-2", "100", "1"),
				product("p-3", "C", "SKU-3", "100", "1"));
		syncService.runFull();

		clock.advance(Duration.ofDays(1));
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));
		BillzSyncRun run = syncService.runFull().orElseThrow();

		assertThat(run.getStatus()).isEqualTo(SyncStatus.FAILED);
		assertThat(productRepository.countByAvailableInBillzTrue()).isEqualTo(3);
	}

	@Test
	void deltaRequestsChangesSinceLastSuccessfulRunMinusOverlap() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));
		Instant firstStart = clock.instant();
		syncService.runFull();

		clock.advance(Duration.ofMinutes(15));
		syncService.runDelta();

		verify(billzClient).getProducts(eq(1), anyInt(), eq(firstStart.minus(Duration.ofMinutes(2))));
	}

	@Test
	void deltaWithoutAnySuccessfulRunFallsBackToFullSync() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));

		BillzSyncRun run = syncService.runDelta().orElseThrow();

		assertThat(run.getType()).isEqualTo(SyncType.FULL);
		verify(billzClient).getProducts(eq(1), anyInt(), isNull());
	}

	@Test
	void billzErrorIsRecordedAsFailedRun() {
		when(billzClient.getProducts(anyInt(), anyInt(), any())).thenThrow(new BillzApiException(503, "down"));

		BillzSyncRun run = syncService.runFull().orElseThrow();

		assertThat(run.getStatus()).isEqualTo(SyncStatus.FAILED);
		assertThat(run.getErrorMessage()).isEqualTo("down");
		assertThat(run.getFinishedAt()).isNotNull();
	}

	@Test
	void backgroundStartIsVisibleImmediatelyAndRejectsAConcurrentStart() throws Exception {
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
		BillzProduct product = product("p-1", "A", "SKU-1", "100", "1").build();
		doAnswer(inv -> {
			release.await(10, java.util.concurrent.TimeUnit.SECONDS);
			return inv.<Integer>getArgument(0) == 1 ? new BillzProductPage(1, List.of(product))
					: new BillzProductPage(1, List.of());
		}).when(billzClient).getProducts(anyInt(), anyInt(), any());

		assertThat(syncService.startInBackground(SyncType.FULL)).isTrue();
		assertThat(syncService.isRunning()).isTrue();
		assertThat(syncService.startInBackground(SyncType.DELTA)).isFalse();
		assertThat(syncService.runFull()).isEmpty();

		release.countDown();
		long deadline = System.currentTimeMillis() + 10_000;
		while (syncService.isRunning() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(syncService.isRunning()).isFalse();
		assertThat(runRepository.findAll()).singleElement()
				.satisfies(run -> assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS));
	}

	@Test
	void announcesOnlyTransitionsBetweenWorkingAndFailing() {
		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));
		syncService.runFull();
		assertThat(events.stream(SyncHealthChangedEvent.class)).isEmpty();

		doThrow(new BillzApiException(503, "down")).when(billzClient).getProducts(anyInt(), anyInt(), any());
		clock.advance(Duration.ofMinutes(15));
		syncService.runDelta();
		clock.advance(Duration.ofMinutes(15));
		syncService.runDelta();
		assertThat(events.stream(SyncHealthChangedEvent.class))
				.singleElement()
				.satisfies(e -> {
					assertThat(e.healthy()).isFalse();
					assertThat(e.errorMessage()).isEqualTo("down");
				});

		billzReturns(product("p-1", "A", "SKU-1", "100", "1"));
		clock.advance(Duration.ofMinutes(15));
		syncService.runDelta();
		assertThat(events.stream(SyncHealthChangedEvent.class)).hasSize(2).last()
				.satisfies(e -> assertThat(e.healthy()).isTrue());
	}

	@Test
	void usesConfiguredShopAndPromoPriceOnlyWhileAPromoIsActive() {
		billzReturns(
				product("p-1", "With promo", "SKU-1", "10000", "4").promo("8000"),
				product("p-2", "Stale promo price", "SKU-2", "10000", "4").stalePromoPrice("8000"));

		syncService.runFull();

		Product withPromo = byBillzId("p-1");
		assertThat(withPromo.getPrice()).isEqualByComparingTo("10000");
		assertThat(withPromo.getPromoPrice()).isEqualByComparingTo("8000");
		assertThat(withPromo.getStock()).isEqualByComparingTo("4");
		assertThat(byBillzId("p-2").getPromoPrice()).isNull();
	}

	@Test
	void duplicateNamesGetDistinctReadableSlugs() {
		billzReturns(product("p-1", "Sun Cream", "SPF-50", "100", "1"),
				product("p-2", "Sun Cream", "SPF-30", "100", "1"));

		syncService.runFull();

		assertThat(byBillzId("p-1").getSlug()).isEqualTo("sun-cream");
		assertThat(byBillzId("p-2").getSlug()).isEqualTo("sun-cream-spf-30");
	}

	private void billzReturns(ProductBuilder... products) {
		List<BillzProduct> list = java.util.Arrays.stream(products).map(ProductBuilder::build).toList();
		// doAnswer form: re-stubbing must not invoke a method that may currently be stubbed to throw.
		doAnswer(inv -> inv.<Integer>getArgument(0) == 1
				? new BillzProductPage(list.size(), list)
				: new BillzProductPage(list.size(), List.of()))
				.when(billzClient).getProducts(anyInt(), anyInt(), any());
	}

	private Product byBillzId(String billzId) {
		return productRepository.findAllByBillzIdIn(List.of(billzId)).getFirst();
	}

	private void inTx(Runnable assertions) {
		tx.executeWithoutResult(status -> assertions.run());
	}

	private static ProductBuilder product(String id, String name, String sku, String price, String stock) {
		return new ProductBuilder(id, name, sku, new BigDecimal(price), new BigDecimal(stock));
	}

	private static final class ProductBuilder {

		private final String id;
		private final String name;
		private final String sku;
		private final BigDecimal price;
		private final BigDecimal stock;
		private String brandId = "";
		private String brandName = "";
		private List<BillzProduct.Category> categories = List.of();
		private List<BillzProduct.Photo> photos = List.of();
		private BigDecimal promoPrice = BigDecimal.ZERO;
		private List<BillzProduct.Promo> promos = null;

		ProductBuilder(String id, String name, String sku, BigDecimal price, BigDecimal stock) {
			this.id = id;
			this.name = name;
			this.sku = sku;
			this.price = price;
			this.stock = stock;
		}

		ProductBuilder brand(String id, String name) {
			this.brandId = id;
			this.brandName = name;
			return this;
		}

		ProductBuilder category(String id, String name) {
			this.categories = List.of(new BillzProduct.Category(id, name, ""));
			return this;
		}

		ProductBuilder photo(String url) {
			this.photos = List.of(new BillzProduct.Photo(url, 0, true));
			return this;
		}

		ProductBuilder promo(String promoPrice) {
			this.promoPrice = new BigDecimal(promoPrice);
			this.promos = List.of(new BillzProduct.Promo("promo-1", "Autumn", "PERCENTAGE", new BigDecimal("20")));
			return this;
		}

		ProductBuilder stalePromoPrice(String promoPrice) {
			this.promoPrice = new BigDecimal(promoPrice);
			this.promos = List.of();
			return this;
		}

		BillzProduct build() {
			return new BillzProduct(id, "", name, sku, "460" + id, "", brandId, brandName, false,
					"2026-10-30 11:49:25", null, null, categories,
					List.of(new BillzProduct.ShopStock(OTHER_SHOP, "Other", new BigDecimal("99")),
							new BillzProduct.ShopStock(SHOP, "Main", stock)),
					List.of(new BillzProduct.ShopPrice(OTHER_SHOP, "Other", new BigDecimal("1"), "KZT", null, null),
							new BillzProduct.ShopPrice(SHOP, "Main", price, "KZT", promoPrice, promos)),
					photos, List.of(), null);
		}
	}

	static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-11-01T00:00:00Z");

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfig {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}
	}
}
