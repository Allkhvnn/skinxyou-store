package skinxyou_backend.catalog.product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import skinxyou_backend.catalog.brand.Brand;
import skinxyou_backend.catalog.category.Category;

/** Public catalog DTOs. Exact stock and BILLZ ids are deliberately not exposed. */
public final class ProductResponses {

	private ProductResponses() {
	}

	public record BrandRef(String slug, String name) {
	}

	public record CategoryRef(String slug, String name) {
	}

	public record Image(String url) {
	}

	/**
	 * @param price      regular price
	 * @param promoPrice discounted price while a BILLZ promo is active, otherwise null
	 */
	public record ProductSummary(
			String slug,
			String name,
			BrandRef brand,
			BigDecimal price,
			BigDecimal promoPrice,
			String currency,
			boolean inStock,
			String imageUrl) {
	}

	public record ProductDetails(
			String slug,
			String name,
			String sku,
			BrandRef brand,
			List<CategoryRef> categories,
			BigDecimal price,
			BigDecimal promoPrice,
			String currency,
			boolean inStock,
			List<Image> images,
			String description,
			String seoTitle,
			String seoDescription,
			List<ProductSummary> related) {
	}

	static ProductSummary summary(Product p) {
		List<ProductImage> images = orderedImages(p);
		return new ProductSummary(p.getSlug(), p.getName(), brandRef(p.getBrand()), p.getPrice(), p.getPromoPrice(),
				p.getCurrency(), isInStock(p), images.isEmpty() ? null : images.getFirst().getUrl());
	}

	static ProductDetails details(Product p, List<Product> related) {
		return new ProductDetails(p.getSlug(), p.getName(), p.getSku(), brandRef(p.getBrand()),
				p.getCategories().stream()
						.sorted(Comparator.comparing(Category::getName))
						.map(c -> new CategoryRef(c.getSlug(), c.getName()))
						.toList(),
				p.getPrice(), p.getPromoPrice(), p.getCurrency(), isInStock(p),
				orderedImages(p).stream().map(i -> new Image(i.getUrl())).toList(),
				p.getDescription(), p.getSeoTitle(), p.getSeoDescription(),
				related.stream().map(ProductResponses::summary).toList());
	}

	/** Main image first, then by sequence. */
	private static List<ProductImage> orderedImages(Product p) {
		return p.getImages().stream()
				.sorted(Comparator.comparing((ProductImage i) -> !i.isMain()).thenComparingInt(ProductImage::getSequence))
				.toList();
	}

	private static boolean isInStock(Product p) {
		return p.getStock() != null && p.getStock().signum() > 0;
	}

	private static BrandRef brandRef(Brand brand) {
		return brand == null ? null : new BrandRef(brand.getSlug(), brand.getName());
	}
}
