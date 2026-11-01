package skinxyou_backend.integration.billz.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product as returned by {@code GET /v2/products}. Only fields the storefront needs are mapped;
 * supply (cost) prices are intentionally omitted.
 */
public record BillzProduct(
		String id,
		String parentId,
		String name,
		String sku,
		String barcode,
		String description,
		String brandId,
		String brandName,
		boolean isVariative,
		String updatedAt,
		String mainImageUrl,
		String mainImageUrlFull,
		List<Category> categories,
		List<ShopStock> shopMeasurementValues,
		List<ShopPrice> shopPrices,
		List<Photo> photos,
		List<Attribute> productAttributes,
		List<CustomField> customFields) {

	public record Category(String id, String name, String parentId) {
	}

	public record ShopStock(String shopId, String shopName, BigDecimal activeMeasurementValue) {
	}

	public record ShopPrice(
			String shopId,
			String shopName,
			BigDecimal retailPrice,
			String retailCurrency,
			BigDecimal promoPrice,
			List<Promo> promos) {
	}

	public record Promo(String id, String name, String discountType, BigDecimal discountValue) {
	}

	public record Photo(String photoUrl, Integer sequence, boolean isMain) {
	}

	public record Attribute(String attributeId, String attributeName, String attributeValueId, String attributeValue) {
	}

	public record CustomField(String customFieldId, String customFieldName, String customFieldValue) {
	}
}
