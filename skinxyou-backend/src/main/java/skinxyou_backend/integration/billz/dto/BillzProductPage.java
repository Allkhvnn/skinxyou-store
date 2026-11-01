package skinxyou_backend.integration.billz.dto;

import java.util.List;

public record BillzProductPage(int count, List<BillzProduct> products) {

	public List<BillzProduct> products() {
		return products == null ? List.of() : products;
	}
}
