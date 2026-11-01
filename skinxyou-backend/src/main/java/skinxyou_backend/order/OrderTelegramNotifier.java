package skinxyou_backend.order;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import skinxyou_backend.checkout.DeliveryMethod;
import skinxyou_backend.integration.telegram.TelegramClient;

/** Tells the manager about a new order. Runs after commit and off the request thread. */
@Component
@RequiredArgsConstructor
class OrderTelegramNotifier {

	private final OrderRepository orderRepository;
	private final TelegramClient telegramClient;

	@Async
	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onOrderPlaced(OrderPlacedEvent event) {
		orderRepository.findById(event.orderId()).ifPresent(order -> telegramClient.send(format(order)));
	}

	static String format(Order order) {
		StringBuilder text = new StringBuilder()
				.append("🛍 Новый заказ ").append(order.getNumber()).append('\n')
				.append('\n')
				.append("Клиент: ").append(order.getCustomerName()).append('\n')
				.append("Телефон: ").append(order.getCustomerPhone()).append('\n')
				.append(order.getDeliveryMethod() == DeliveryMethod.COURIER
						? "Доставка: курьер, " + order.getDeliveryAddress()
						: "Доставка: самовывоз")
				.append('\n')
				.append('\n');
		for (OrderItem item : order.getItems()) {
			text.append("• ").append(item.getProductName());
			if (item.getSku() != null) {
				text.append(" (").append(item.getSku()).append(')');
			}
			text.append(" × ").append(item.getQuantity()).append(" — ").append(money(item.getLineTotal())).append('\n');
		}
		text.append('\n')
				.append("Товары: ").append(money(order.getItemsTotal())).append('\n')
				.append("Доставка: ").append(money(order.getDeliveryFee())).append('\n')
				.append("Итого: ").append(money(order.getTotal())).append('\n');
		if (order.getComment() != null) {
			text.append('\n').append("Комментарий: ").append(order.getComment()).append('\n');
		}
		text.append('\n').append("Оплата: Kaspi Pay — отправьте клиенту счёт.");
		return text.toString();
	}

	private static String money(BigDecimal value) {
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
		symbols.setGroupingSeparator(' ');
		return new DecimalFormat("#,##0", symbols).format(value) + " ₸";
	}
}
