package skinxyou_backend.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductRepository;

/** Computes cart totals from current database prices and stock; the single source of truth for money. */
@Service
@RequiredArgsConstructor
public class CheckoutPricing {

	private final ProductRepository productRepository;
	private final CheckoutProperties properties;

	@Transactional(readOnly = true)
	public Quote quote(List<CheckoutItem> items, DeliveryMethod deliveryMethod) {
		// The same product sent twice is merged into one line.
		Map<String, Integer> quantities = new LinkedHashMap<>();
		items.forEach(i -> quantities.merge(i.slug(), i.quantity(), Integer::sum));

		Map<String, Product> products = productRepository
				.findAllBySlugInAndHiddenFalseAndAvailableInBillzTrue(quantities.keySet()).stream()
				.collect(Collectors.toMap(Product::getSlug, Function.identity()));

		List<Quote.Line> lines = new ArrayList<>();
		List<Quote.Problem> problems = new ArrayList<>();
		BigDecimal itemsTotal = BigDecimal.ZERO;

		for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
			Product product = products.get(entry.getKey());
			BigDecimal unitPrice = product == null ? null : effectivePrice(product);
			if (product == null || unitPrice == null) {
				problems.add(new Quote.Problem(entry.getKey(), product == null ? null : product.getName(),
						Quote.Reason.NOT_AVAILABLE, 0));
				continue;
			}
			int available = product.getStock().intValue();
			int quantity = entry.getValue();
			if (available < quantity) {
				problems.add(new Quote.Problem(product.getSlug(), product.getName(), Quote.Reason.INSUFFICIENT_STOCK,
						Math.max(available, 0)));
			}
			BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
			lines.add(new Quote.Line(product, quantity, unitPrice, lineTotal));
			itemsTotal = itemsTotal.add(lineTotal);
		}

		BigDecimal deliveryFee = deliveryFee(deliveryMethod, itemsTotal);
		return new Quote(lines, problems, itemsTotal, deliveryFee, itemsTotal.add(deliveryFee), properties.currency());
	}

	BigDecimal deliveryFee(DeliveryMethod method, BigDecimal itemsTotal) {
		if (method == DeliveryMethod.PICKUP || itemsTotal.compareTo(properties.freeCourierFrom()) >= 0) {
			return BigDecimal.ZERO;
		}
		return properties.courierFee();
	}

	private static BigDecimal effectivePrice(Product product) {
		return product.getPromoPrice() != null ? product.getPromoPrice() : product.getPrice();
	}
}
