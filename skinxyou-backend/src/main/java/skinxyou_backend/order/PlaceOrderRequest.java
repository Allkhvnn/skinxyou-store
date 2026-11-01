package skinxyou_backend.order;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import skinxyou_backend.checkout.CheckoutItem;
import skinxyou_backend.checkout.DeliveryMethod;

/**
 * @param idempotencyKey generated once per checkout attempt in the browser and reused on retries
 * @param consent        agreement to personal data processing, required by Kazakhstan law
 */
public record PlaceOrderRequest(
		@NotNull UUID idempotencyKey,
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 30) String phone,
		@NotNull DeliveryMethod deliveryMethod,
		@Size(max = 500) String address,
		@Size(max = 1000) String comment,
		@NotEmpty @Size(max = 50) List<@Valid CheckoutItem> items,
		@AssertTrue(message = "Нужно согласие на обработку персональных данных") boolean consent) {

	@AssertTrue(message = "Укажите адрес доставки")
	boolean isAddressPresentForCourier() {
		return deliveryMethod != DeliveryMethod.COURIER || (address != null && !address.isBlank());
	}
}
