package skinxyou_backend.admin;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skinxyou_backend.common.web.PageResponse;
import skinxyou_backend.order.Order;
import skinxyou_backend.order.OrderRepository;
import skinxyou_backend.order.OrderStatus;
import skinxyou_backend.order.PaymentStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

	private final OrderRepository orderRepository;
	private final Clock clock;

	public record Filter(OrderStatus status, PaymentStatus paymentStatus, String query) {
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminOrderResponses.Summary> list(Filter filter, int page, int size) {
		return PageResponse.of(orderRepository.findAll(specification(filter),
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))),
				AdminOrderResponses.Summary::of);
	}

	@Transactional(readOnly = true)
	public AdminOrderResponses.Details get(String number) {
		return AdminOrderResponses.Details.of(find(number));
	}

	@Transactional
	public AdminOrderResponses.Details changeStatus(String number, OrderStatus target, String note, long version,
			String actor) {
		Order order = findForUpdate(number, version);
		order.changeStatus(target, actor, note, clock.instant());
		log.info("Order {} status → {} by {}", number, target, actor);
		return AdminOrderResponses.Details.of(orderRepository.saveAndFlush(order));
	}

	@Transactional
	public AdminOrderResponses.Details changePaymentStatus(String number, PaymentStatus target, String note,
			long version, String actor) {
		Order order = findForUpdate(number, version);
		order.changePaymentStatus(target, actor, note, clock.instant());
		log.info("Order {} payment → {} by {}", number, target, actor);
		return AdminOrderResponses.Details.of(orderRepository.saveAndFlush(order));
	}

	private Order find(String number) {
		return orderRepository.findByNumber(number)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден"));
	}

	/** The version the manager saw must still be current; otherwise someone changed the order meanwhile. */
	private Order findForUpdate(String number, long version) {
		Order order = find(number);
		if (order.getVersion() != version) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Заказ уже изменил другой сотрудник. Обновите страницу.");
		}
		return order;
	}

	private static Specification<Order> specification(Filter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.status() != null) {
				predicates.add(cb.equal(root.get("status"), filter.status()));
			}
			if (filter.paymentStatus() != null) {
				predicates.add(cb.equal(root.get("paymentStatus"), filter.paymentStatus()));
			}
			if (filter.query() != null && !filter.query().isBlank()) {
				HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
				String text = filter.query().trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
				String digits = filter.query().replaceAll("\\D", "");
				List<Predicate> any = new ArrayList<>();
				any.add(hcb.ilike(root.get("number"), cb.literal("%" + text + "%"), cb.literal('!')));
				any.add(hcb.ilike(root.get("customerName"), cb.literal("%" + text + "%"), cb.literal('!')));
				if (digits.length() >= 4) {
					// Phones are stored as +77XXXXXXXXX; match the trailing digits the manager typed.
					String tail = digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
					any.add(cb.like(root.get("customerPhone"), "%" + tail + "%"));
				}
				predicates.add(cb.or(any.toArray(Predicate[]::new)));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
