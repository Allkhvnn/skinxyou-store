package skinxyou_backend.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skinxyou_backend.common.web.PageResponse;
import skinxyou_backend.order.OrderStatus;
import skinxyou_backend.order.PaymentStatus;

/** Requires a staff token (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

	private final AdminOrderService service;

	public record StatusChange(@NotNull OrderStatus status, @Size(max = 500) String note, long version) {
	}

	public record PaymentStatusChange(@NotNull PaymentStatus paymentStatus, @Size(max = 500) String note,
			long version) {
	}

	@GetMapping
	public PageResponse<AdminOrderResponses.Summary> list(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) PaymentStatus paymentStatus,
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.list(new AdminOrderService.Filter(status, paymentStatus, q), page, size);
	}

	@GetMapping("/{number}")
	public AdminOrderResponses.Details get(@PathVariable String number) {
		return service.get(number);
	}

	@PostMapping("/{number}/status")
	public AdminOrderResponses.Details changeStatus(@PathVariable String number,
			@Valid @RequestBody StatusChange change, @AuthenticationPrincipal Jwt jwt) {
		return service.changeStatus(number, change.status(), blankToNull(change.note()), change.version(), actor(jwt));
	}

	@PostMapping("/{number}/payment-status")
	public AdminOrderResponses.Details changePaymentStatus(@PathVariable String number,
			@Valid @RequestBody PaymentStatusChange change, @AuthenticationPrincipal Jwt jwt) {
		return service.changePaymentStatus(number, change.paymentStatus(), blankToNull(change.note()),
				change.version(), actor(jwt));
	}

	/** Recorded in the order history, e.g. "Алихан (alikhan)". */
	private static String actor(Jwt jwt) {
		String name = jwt.getClaimAsString(AdminSecurityConfig.NAME_CLAIM);
		return name == null || name.isBlank() ? jwt.getSubject() : name + " (" + jwt.getSubject() + ")";
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
