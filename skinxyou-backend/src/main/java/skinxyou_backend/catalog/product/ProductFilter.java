package skinxyou_backend.catalog.product;

import java.math.BigDecimal;

/** Storefront catalog filter; every field is optional. Prices compare against the effective (promo) price. */
public record ProductFilter(
		String brand,
		String category,
		BigDecimal minPrice,
		BigDecimal maxPrice,
		boolean inStock,
		String query) {
}
