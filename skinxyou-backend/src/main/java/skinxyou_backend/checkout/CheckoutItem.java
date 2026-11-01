package skinxyou_backend.checkout;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A cart line as sent by the browser: only what and how many; prices always come from the database. */
public record CheckoutItem(
		@NotBlank @Size(max = 255) String slug,
		@Min(1) @Max(99) int quantity) {
}
