package skinxyou_backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import skinxyou_backend.checkout.DeliveryMethod;

/** What the customer sees after checkout. Phone and address are echoed back only to the submitter. */
public record OrderResponse(
		String number,
		OrderStatus status,
		PaymentStatus paymentStatus,
		PaymentMethod paymentMethod,
		DeliveryMethod deliveryMethod,
		String deliveryAddress,
		List<Item> items,
		BigDecimal itemsTotal,
		BigDecimal deliveryFee,
		BigDecimal total,
		String currency,
		Instant createdAt) {

	public record Item(String slug, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	static OrderResponse of(Order order) {
		return new OrderResponse(order.getNumber(), order.getStatus(), order.getPaymentStatus(),
				order.getPaymentMethod(), order.getDeliveryMethod(), order.getDeliveryAddress(),
				order.getItems().stream()
						.map(i -> new Item(i.getProductSlug(), i.getProductName(), i.getQuantity(), i.getUnitPrice(),
								i.getLineTotal()))
						.toList(),
				order.getItemsTotal(), order.getDeliveryFee(), order.getTotal(), order.getCurrency(),
				order.getCreatedAt());
	}
}
