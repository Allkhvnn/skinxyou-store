package skinxyou_backend.integration.billz.sync;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinxyou_backend.catalog.brand.Brand;
import skinxyou_backend.catalog.brand.BrandRepository;
import skinxyou_backend.catalog.category.Category;
import skinxyou_backend.catalog.category.CategoryRepository;
import skinxyou_backend.catalog.product.ImageSource;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.common.util.Slugs;
import skinxyou_backend.integration.billz.dto.BillzProduct;

/**
 * Idempotently upserts a page of BILLZ products, matching on the BILLZ id. Only BILLZ-owned fields
 * are written; slug (set once on creation), descriptions, SEO, visibility and SITE images are kept.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BillzProductImporter {

	private static final DateTimeFormatter BILLZ_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;

	record Result(int created, int updated) {
	}

	@Transactional
	public Result importPage(List<BillzProduct> page, String shopId, Instant syncedAt) {
		List<BillzProduct> valid = page.stream()
				.filter(p -> !isBlank(p.id()) && !isBlank(p.name()))
				.toList();
		if (valid.size() < page.size()) {
			log.warn("Skipped {} BILLZ products without id or name", page.size() - valid.size());
		}

		Map<String, Product> existing = new HashMap<>();
		productRepository.findAllByBillzIdIn(valid.stream().map(BillzProduct::id).toList())
				.forEach(p -> existing.put(p.getBillzId(), p));

		PageContext context = new PageContext();
		int created = 0;
		for (BillzProduct source : valid) {
			Product product = existing.get(source.id());
			if (product == null) {
				product = new Product();
				product.setBillzId(source.id());
				product.setSlug(uniqueSlug(Slugs.slugify(source.name()), source, context.productSlugs,
						productRepository::existsBySlug));
				existing.put(source.id(), product);
				created++;
			}
			apply(source, product, shopId, syncedAt, context);
			productRepository.save(product);
		}
		return new Result(created, valid.size() - created);
	}

	private void apply(BillzProduct source, Product product, String shopId, Instant syncedAt, PageContext context) {
		product.setName(source.name().trim());
		product.setSku(blankToNull(source.sku()));
		product.setBarcode(blankToNull(source.barcode()));
		product.setBrand(resolveBrand(source, context));
		product.getCategories().clear();
		product.getCategories().addAll(resolveCategories(source, context));
		applyPrice(source, product, shopId);
		product.setStock(stockOf(source, shopId));
		product.setBillzUpdatedAt(parseDateTime(source.updatedAt()));
		product.setAvailableInBillz(true);
		product.setSyncedAt(syncedAt);
		syncImages(source, product);
	}

	private static void applyPrice(BillzProduct source, Product product, String shopId) {
		BillzProduct.ShopPrice price = nullToEmpty(source.shopPrices()).stream()
				.filter(p -> shopId.equals(p.shopId()))
				.findFirst()
				.orElse(null);
		if (price == null) {
			product.setPrice(null);
			product.setPromoPrice(null);
			product.setCurrency(null);
			return;
		}
		product.setPrice(price.retailPrice());
		product.setCurrency(blankToNull(price.retailCurrency()));
		// BILLZ fills promo_price only while a promo is active; 0 means "no promo".
		boolean promoActive = price.promos() != null && !price.promos().isEmpty()
				&& price.promoPrice() != null && price.promoPrice().signum() > 0;
		product.setPromoPrice(promoActive ? price.promoPrice() : null);
	}

	private static BigDecimal stockOf(BillzProduct source, String shopId) {
		return nullToEmpty(source.shopMeasurementValues()).stream()
				.filter(s -> shopId.equals(s.shopId()))
				.map(BillzProduct.ShopStock::activeMeasurementValue)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}

	/** Replaces BILLZ images only when the BILLZ photo list changed; SITE images are left alone. */
	private static void syncImages(BillzProduct source, Product product) {
		List<BillzProduct.Photo> photos = nullToEmpty(source.photos()).stream()
				.filter(p -> !isBlank(p.photoUrl()))
				.sorted(Comparator.comparing(p -> p.sequence() == null ? 0 : p.sequence()))
				.toList();
		if (photos.isEmpty() && !isBlank(source.mainImageUrlFull())) {
			photos = List.of(new BillzProduct.Photo(source.mainImageUrlFull(), 0, true));
		}

		List<String> wanted = photos.stream().map(BillzProduct.Photo::photoUrl).toList();
		List<String> current = product.getImages().stream()
				.filter(i -> i.getSource() == ImageSource.BILLZ)
				.map(ProductImage::getUrl)
				.toList();
		if (wanted.equals(current)) {
			return;
		}

		product.getImages().removeIf(i -> i.getSource() == ImageSource.BILLZ);
		for (int i = 0; i < photos.size(); i++) {
			BillzProduct.Photo photo = photos.get(i);
			product.getImages().add(new ProductImage(product, photo.photoUrl(), i, photo.isMain(), ImageSource.BILLZ));
		}
	}

	private Brand resolveBrand(BillzProduct source, PageContext context) {
		if (isBlank(source.brandId()) || isBlank(source.brandName())) {
			return null;
		}
		return context.brands.computeIfAbsent(source.brandId(), billzId -> {
			Brand brand = brandRepository.findByBillzId(billzId).orElseGet(() -> {
				Brand created = new Brand();
				created.setBillzId(billzId);
				created.setSlug(uniqueSlug(Slugs.slugify(source.brandName()), billzId, context.brandSlugs,
						brandRepository::existsBySlug));
				return created;
			});
			brand.setName(source.brandName().trim());
			return brandRepository.save(brand);
		});
	}

	private Set<Category> resolveCategories(BillzProduct source, PageContext context) {
		Set<Category> result = new LinkedHashSet<>();
		for (BillzProduct.Category c : nullToEmpty(source.categories())) {
			if (isBlank(c.id()) || isBlank(c.name())) {
				continue;
			}
			result.add(context.categories.computeIfAbsent(c.id(), billzId -> {
				Category category = categoryRepository.findByBillzId(billzId).orElseGet(() -> {
					Category created = new Category();
					created.setBillzId(billzId);
					created.setSlug(uniqueSlug(Slugs.slugify(c.name()), billzId, context.categorySlugs,
							categoryRepository::existsBySlug));
					return created;
				});
				category.setName(c.name().trim());
				category.setParentBillzId(blankToNull(c.parentId()));
				return categoryRepository.save(category);
			}));
		}
		return result;
	}

	/** Prefers the plain name, then name + SKU, so duplicates still get readable URLs. */
	private static String uniqueSlug(String base, BillzProduct source, Set<String> taken, Predicate<String> exists) {
		if (!isBlank(source.sku()) && !isFree(base, taken, exists)) {
			String withSku = base + "-" + Slugs.slugify(source.sku());
			if (isFree(withSku, taken, exists)) {
				return claim(withSku, taken);
			}
		}
		return uniqueSlug(base, source.id(), taken, exists);
	}

	/** Falls back to a suffix from the stable BILLZ id, which is unique by definition. */
	private static String uniqueSlug(String base, String billzId, Set<String> taken, Predicate<String> exists) {
		if (isFree(base, taken, exists)) {
			return claim(base, taken);
		}
		String suffix = Slugs.slugify(billzId);
		return claim(base + "-" + suffix.substring(0, Math.min(8, suffix.length())), taken);
	}

	private static boolean isFree(String slug, Set<String> taken, Predicate<String> exists) {
		return !taken.contains(slug) && !exists.test(slug);
	}

	private static String claim(String slug, Set<String> taken) {
		taken.add(slug);
		return slug;
	}

	private static LocalDateTime parseDateTime(String value) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return LocalDateTime.parse(value, BILLZ_DATE_TIME);
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String blankToNull(String value) {
		return isBlank(value) ? null : value.trim();
	}

	private static <T> List<T> nullToEmpty(List<T> list) {
		return list == null ? List.of() : list;
	}

	/** Per-page caches so a brand shared by many products is looked up once and new slugs don't collide. */
	private static final class PageContext {
		final Map<String, Brand> brands = new HashMap<>();
		final Map<String, Category> categories = new HashMap<>();
		final Set<String> productSlugs = new HashSet<>();
		final Set<String> brandSlugs = new HashSet<>();
		final Set<String> categorySlugs = new HashSet<>();
	}
}
