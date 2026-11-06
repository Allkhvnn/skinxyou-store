package skinxyou_backend.catalog.product;

import java.util.Arrays;
import java.util.Optional;

public enum ProductSort {
	NEWEST("newest"),
	/** Most units sold in non-cancelled orders first. */
	POPULAR("popular"),
	PRICE_ASC("price_asc"),
	PRICE_DESC("price_desc"),
	NAME("name");

	private final String param;

	ProductSort(String param) {
		this.param = param;
	}

	public static Optional<ProductSort> fromParam(String value) {
		return Arrays.stream(values()).filter(s -> s.param.equalsIgnoreCase(value)).findFirst();
	}
}
