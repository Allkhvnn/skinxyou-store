package skinxyou_backend.integration.telegram;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Sends plain-text messages through the Telegram Bot API ({@code sendMessage}). */
@Slf4j
public class TelegramClient {

	private static final int MAX_ATTEMPTS = 3;

	private final RestClient restClient;
	private final TelegramProperties properties;

	public TelegramClient(RestClient.Builder builder, TelegramProperties properties) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
		requestFactory.setReadTimeout(Duration.ofSeconds(15));
		this.restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
		this.properties = properties;
	}

	/** Returns false when not configured or all attempts failed; never throws. */
	public boolean send(String text) {
		if (!properties.isConfigured()) {
			log.info("Telegram is not configured (TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID), notification skipped");
			return false;
		}
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				restClient.post()
						.uri("/bot{token}/sendMessage", properties.botToken())
						.contentType(MediaType.APPLICATION_JSON)
						.body(Map.of("chat_id", properties.chatId(), "text", text, "disable_web_page_preview", true))
						.retrieve()
						.toBodilessEntity();
				return true;
			}
			catch (RestClientException e) {
				// The message text contains customer data and the URL contains the token: log neither.
				log.warn("Telegram sendMessage failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS,
						e.getClass().getSimpleName());
				sleep(attempt);
			}
		}
		return false;
	}

	private static void sleep(int attempt) {
		try {
			Thread.sleep(Duration.ofSeconds(attempt));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
