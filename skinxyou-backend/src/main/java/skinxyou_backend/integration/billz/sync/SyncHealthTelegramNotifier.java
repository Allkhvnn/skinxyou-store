package skinxyou_backend.integration.billz.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import skinxyou_backend.integration.telegram.TelegramClient;

/** Tells the team when prices and stock on the site stop (or resume) updating from BILLZ. */
@Component
@RequiredArgsConstructor
class SyncHealthTelegramNotifier {

	private static final int MAX_ERROR_LENGTH = 300;

	private final TelegramClient telegramClient;

	@Async
	@EventListener
	public void onHealthChanged(SyncHealthChangedEvent event) {
		telegramClient.send(event.healthy()
				? "✅ Синхронизация с BILLZ снова работает. Цены и остатки на сайте актуальны."
				: "⚠️ Синхронизация с BILLZ не удалась. Цены и остатки на сайте могут быть устаревшими.\n\n"
						+ "Ошибка: " + abbreviate(event.errorMessage()) + "\n\n"
						+ "Подробности — в админке, раздел «Синхронизация».");
	}

	private static String abbreviate(String message) {
		if (message == null) {
			return "неизвестна";
		}
		return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH) + "…";
	}
}
