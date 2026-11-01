package skinxyou_backend.integration.billz.dto;

import java.util.List;

public record BillzShopPage(int count, List<Shop> shops) {

	public List<Shop> shops() {
		return shops == null ? List.of() : shops;
	}

	public record Shop(String id, String name, String address, Long deletedAt) {
	}
}
