package skinxyou_backend.order;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class OrderStatusHistory {

	public enum Type {
		ORDER,
		PAYMENT
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Type type;

	private String fromStatus;

	@Column(nullable = false)
	private String toStatus;

	@Column(nullable = false)
	private String changedBy;

	private String note;

	@Column(nullable = false)
	private Instant createdAt;

	OrderStatusHistory(Order order, Type type, String fromStatus, String toStatus, String changedBy, String note,
			Instant createdAt) {
		this.order = order;
		this.type = type;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedBy = changedBy;
		this.note = note;
		this.createdAt = createdAt;
	}
}
