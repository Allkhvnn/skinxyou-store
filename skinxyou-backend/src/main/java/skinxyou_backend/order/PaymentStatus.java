package skinxyou_backend.order;

/** Tracked separately from {@link OrderStatus}: an order can be confirmed while still unpaid. */
public enum PaymentStatus {
	PENDING,
	PAID,
	CANCELLED
}
