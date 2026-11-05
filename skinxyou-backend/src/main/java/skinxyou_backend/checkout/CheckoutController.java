package skinxyou_backend.checkout;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

	private final CheckoutPricing pricing;
	private final CheckoutProperties properties;

	public record Options(String currency, BigDecimal courierFee, BigDecimal freeCourierFrom, String pickupAddress) {
	}

	public record QuoteRequest(
			@NotEmpty @Size(max = 50) List<@Valid CheckoutItem> items,
			@NotNull DeliveryMethod deliveryMethod) {
	}

	public record QuoteResponse(
			List<Line> lines,
			List<Quote.Problem> problems,
			BigDecimal itemsTotal,
			BigDecimal deliveryFee,
			BigDecimal total,
			String currency) {

		public record Line(String slug, String name, String imageUrl, int quantity, BigDecimal unitPrice,
				BigDecimal lineTotal) {
		}
	}

	@GetMapping("/options")
	public Options options() {
		return new Options(properties.currency(), properties.courierFee(), properties.freeCourierFrom(),
				properties.pickupAddress());
	}

	/** Current prices and availability for the cart, so the browser never shows stale totals. */
	@PostMapping("/quote")
	@Transactional(readOnly = true)
	public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
		Quote quote = pricing.quote(request.items(), request.deliveryMethod());
		return new QuoteResponse(
				quote.lines().stream()
						.map(l -> new QuoteResponse.Line(l.product().getSlug(), l.product().getName(),
								mainImage(l.product()), l.quantity(), l.unitPrice(), l.lineTotal()))
						.toList(),
				quote.problems(), quote.itemsTotal(), quote.deliveryFee(), quote.total(), quote.currency());
	}

	private static String mainImage(Product product) {
		return product.orderedImages().stream().findFirst().map(ProductImage::getUrl).orElse(null);
	}
}
