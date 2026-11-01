package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import skinxyou_backend.checkout.DeliveryMethod;
import skinxyou_backend.order.Order;
import skinxyou_backend.order.OrderStatus;
import skinxyou_backend.order.PaymentMethod;
import skinxyou_backend.order.PaymentStatus;

/** Staff-only order views; unlike the public API they include customer contact data. */
public final class AdminOrderResponses {

	private AdminOrderResponses() {
	}

	public record Summary(
			String number,
			Instant createdAt,
			String customerName,
			String customerPhone,
			DeliveryMethod deliveryMethod,
			int itemCount,
			BigDecimal total,
			String currency,
			OrderStatus status,
			PaymentStatus paymentStatus) {

		static Summary of(Order o) {
			return new Summary(o.getNumber(), o.getCreatedAt(), o.getCustomerName(), o.getCustomerPhone(),
					o.getDeliveryMethod(), o.getItems().stream().mapToInt(i -> i.getQuantity()).sum(), o.getTotal(),
					o.getCurrency(), o.getStatus(), o.getPaymentStatus());
		}
	}

	public record Item(String slug, String name, String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	public record HistoryEntry(String type, String fromStatus, String toStatus, String changedBy, String note,
			Instant createdAt) {
	}

	/**
	 * @param version pass back when changing a status; a mismatch means someone else changed the order
	 */
	public record Details(
			String number,
			Instant createdAt,
			OrderStatus status,
			PaymentStatus paymentStatus,
			PaymentMethod paymentMethod,
			DeliveryMethod deliveryMethod,
			String customerName,
			String customerPhone,
			String deliveryAddress,
			String comment,
			List<Item> items,
			BigDecimal itemsTotal,
			BigDecimal deliveryFee,
			BigDecimal total,
			String currency,
			List<OrderStatus> allowedStatuses,
			List<PaymentStatus> allowedPaymentStatuses,
			List<HistoryEntry> history,
			long version) {

		static Details of(Order o) {
			return new Details(o.getNumber(), o.getCreatedAt(), o.getStatus(), o.getPaymentStatus(),
					o.getPaymentMethod(), o.getDeliveryMethod(), o.getCustomerName(), o.getCustomerPhone(),
					o.getDeliveryAddress(), o.getComment(),
					o.getItems().stream()
							.map(i -> new Item(i.getProductSlug(), i.getProductName(), i.getSku(), i.getQuantity(),
									i.getUnitPrice(), i.getLineTotal()))
							.toList(),
					o.getItemsTotal(), o.getDeliveryFee(), o.getTotal(), o.getCurrency(),
					o.allowedNextStatuses(), o.allowedNextPaymentStatuses(),
					o.getHistory().stream()
							.map(h -> new HistoryEntry(h.getType().name(), h.getFromStatus(), h.getToStatus(),
									h.getChangedBy(), h.getNote(), h.getCreatedAt()))
							.toList(),
					o.getVersion());
		}
	}
}
