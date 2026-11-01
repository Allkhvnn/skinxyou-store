package skinxyou_backend.order;

import java.time.Clock;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import skinxyou_backend.checkout.CheckoutPricing;
import skinxyou_backend.checkout.Quote;
import skinxyou_backend.common.util.PhoneNumbers;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final CheckoutPricing pricing;
	private final ApplicationEventPublisher events;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public record Placed(OrderResponse order, boolean created) {
	}

	/**
	 * Idempotent: repeating a request with the same key returns the order created by the first one.
	 * Totals are recalculated from the database; nothing price-related is taken from the client.
	 */
	public Placed place(PlaceOrderRequest request) {
		Optional<Placed> existing = findExisting(request);
		if (existing.isPresent()) {
			return existing.get();
		}
		String phone = PhoneNumbers.normalizeKz(request.phone())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Укажите номер телефона в формате +7 7XX XXX XX XX"));
		try {
			OrderResponse created = transactionTemplate.execute(status -> {
				Quote quote = pricing.quote(request.items(), request.deliveryMethod());
				if (!quote.isOrderable()) {
					throw new CheckoutProblemException(quote.problems());
				}
				String number = "SX-" + orderRepository.nextOrderNumber();
				Order order = orderRepository.save(Order.place(number, request.idempotencyKey(), quote,
						request.deliveryMethod(), request.name().trim(), phone, trimToNull(request.address()),
						trimToNull(request.comment()), clock.instant()));
				events.publishEvent(new OrderPlacedEvent(order.getId()));
				return OrderResponse.of(order);
			});
			log.info("Order {} placed: {} items, total {}", created.number(), created.items().size(), created.total());
			return new Placed(created, true);
		}
		catch (DataIntegrityViolationException e) {
			// A concurrent request with the same key won the race.
			return findExisting(request).orElseThrow(() -> e);
		}
	}

	private Optional<Placed> findExisting(PlaceOrderRequest request) {
		return transactionTemplate.execute(status -> orderRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(order -> new Placed(OrderResponse.of(order), false)));
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
