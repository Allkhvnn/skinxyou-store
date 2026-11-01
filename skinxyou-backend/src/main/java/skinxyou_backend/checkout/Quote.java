package skinxyou_backend.checkout;

import java.math.BigDecimal;
import java.util.List;

import skinxyou_backend.catalog.product.Product;

/**
 * Server-side price calculation for a cart. {@code problems} lists lines that cannot be ordered
 * as requested; an order is only created when it is empty.
 */
public record Quote(
		List<Line> lines,
		List<Problem> problems,
		BigDecimal itemsTotal,
		BigDecimal deliveryFee,
		BigDecimal total,
		String currency) {

	public boolean isOrderable() {
		return problems.isEmpty() && !lines.isEmpty();
	}

	public record Line(Product product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	/**
	 * @param available whole units currently in stock (0 when unavailable)
	 */
	public record Problem(String slug, String name, Reason reason, int available) {
	}

	public enum Reason {
		/** Unknown, hidden or removed from BILLZ. */
		NOT_AVAILABLE,
		/** Requested more than is in stock. */
		INSUFFICIENT_STOCK
	}
}
