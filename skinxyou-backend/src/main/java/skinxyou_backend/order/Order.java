package skinxyou_backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skinxyou_backend.checkout.DeliveryMethod;
import skinxyou_backend.checkout.Quote;

@Entity
@Table(name = "customer_order")
@Getter
@NoArgsConstructor
public class Order {

	static final String SYSTEM = "system";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String number;

	@Column(nullable = false, unique = true, updatable = false)
	private UUID idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus paymentStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeliveryMethod deliveryMethod;

	@Column(nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String customerPhone;

	private String deliveryAddress;

	private String comment;

	@Column(nullable = false)
	private BigDecimal itemsTotal;

	@Column(nullable = false)
	private BigDecimal deliveryFee;

	@Column(nullable = false)
	private BigDecimal total;

	@Column(nullable = false)
	private String currency;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt, id")
	private List<OrderStatusHistory> history = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	/**
	 * Order workflow: NEW → CONFIRMED → SHIPPED (courier) / READY_FOR_PICKUP (pickup) → COMPLETED.
	 * Any non-final order can be cancelled; COMPLETED and CANCELLED are final.
	 */
	public List<OrderStatus> allowedNextStatuses() {
		return switch (status) {
			case NEW -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
			case CONFIRMED -> List.of(deliveryMethod == DeliveryMethod.COURIER ? OrderStatus.SHIPPED
					: OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED);
			case SHIPPED, READY_FOR_PICKUP -> List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
			case COMPLETED, CANCELLED -> List.of();
		};
	}

	/** Payment is set by hand after the Kaspi invoice; PAID can be reverted to fix a mistake. */
	public List<PaymentStatus> allowedNextPaymentStatuses() {
		return switch (paymentStatus) {
			case PENDING -> List.of(PaymentStatus.PAID, PaymentStatus.CANCELLED);
			case PAID, CANCELLED -> List.of(PaymentStatus.PENDING);
		};
	}

	public void changeStatus(OrderStatus target, String changedBy, String note, Instant now) {
		if (!allowedNextStatuses().contains(target)) {
			throw new IllegalStatusTransitionException("Нельзя перевести заказ из «" + status + "» в «" + target + "»");
		}
		history.add(new OrderStatusHistory(this, OrderStatusHistory.Type.ORDER, status.name(), target.name(), changedBy,
				note, now));
		status = target;
	}

	public void changePaymentStatus(PaymentStatus target, String changedBy, String note, Instant now) {
		if (!allowedNextPaymentStatuses().contains(target)) {
			throw new IllegalStatusTransitionException(
					"Нельзя изменить оплату с «" + paymentStatus + "» на «" + target + "»");
		}
		history.add(new OrderStatusHistory(this, OrderStatusHistory.Type.PAYMENT, paymentStatus.name(), target.name(),
				changedBy, note, now));
		paymentStatus = target;
	}

	/** Creates a new order from a quote that has already been checked to be orderable. */
	static Order place(String number, UUID idempotencyKey, Quote quote, DeliveryMethod deliveryMethod,
			String customerName, String customerPhone, String deliveryAddress, String comment, Instant now) {
		Order order = new Order();
		order.number = number;
		order.idempotencyKey = idempotencyKey;
		order.status = OrderStatus.NEW;
		order.paymentStatus = PaymentStatus.PENDING;
		order.paymentMethod = PaymentMethod.KASPI_INVOICE;
		order.deliveryMethod = deliveryMethod;
		order.customerName = customerName;
		order.customerPhone = customerPhone;
		order.deliveryAddress = deliveryMethod == DeliveryMethod.COURIER ? deliveryAddress : null;
		order.comment = comment;
		order.itemsTotal = quote.itemsTotal();
		order.deliveryFee = quote.deliveryFee();
		order.total = quote.total();
		order.currency = quote.currency();
		quote.lines().forEach(line -> order.items.add(
				new OrderItem(order, line.product(), line.quantity(), line.unitPrice(), line.lineTotal())));
		order.history.add(new OrderStatusHistory(order, OrderStatusHistory.Type.ORDER, null, OrderStatus.NEW.name(),
				SYSTEM, "Заказ оформлен на сайте", now));
		order.history.add(new OrderStatusHistory(order, OrderStatusHistory.Type.PAYMENT, null,
				PaymentStatus.PENDING.name(), SYSTEM, null, now));
		return order;
	}
}
