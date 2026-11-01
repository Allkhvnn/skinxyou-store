package skinxyou_backend.integration.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param botToken token from @BotFather; secret, set via TELEGRAM_BOT_TOKEN
 * @param chatId   chat (person or group) that receives order notifications
 */
@ConfigurationProperties("telegram")
public record TelegramProperties(String baseUrl, String botToken, String chatId) {

	public boolean isConfigured() {
		return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
	}
}
