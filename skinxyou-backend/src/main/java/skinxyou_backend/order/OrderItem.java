package skinxyou_backend.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import skinxyou_backend.catalog.product.Product;

/** Snapshot of a product at order time; never recalculated afterwards. */
@Entity
@Getter
@NoArgsConstructor
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private String productBillzId;

	@Column(nullable = false)
	private String productSlug;

	@Column(nullable = false)
	private String productName;

	private String sku;

	@Column(nullable = false)
	private BigDecimal unitPrice;

	private int quantity;

	@Column(nullable = false)
	private BigDecimal lineTotal;

	OrderItem(Order order, Product product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
		this.order = order;
		this.product = product;
		this.productBillzId = product.getBillzId();
		this.productSlug = product.getSlug();
		this.productName = product.getName();
		this.sku = product.getSku();
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.lineTotal = lineTotal;
	}
}
