package skinxyou_backend.checkout;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Store delivery rules; changed through configuration, not code.
 *
 * @param courierFee      courier delivery price within Almaty
 * @param freeCourierFrom items total from which courier delivery is free
 * @param pickupAddress   shown to customers choosing pickup
 */
@ConfigurationProperties("shop.checkout")
public record CheckoutProperties(
		String currency,
		BigDecimal courierFee,
		BigDecimal freeCourierFrom,
		String pickupAddress) {
}
