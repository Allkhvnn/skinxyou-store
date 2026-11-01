package skinxyou_backend.integration.telegram;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
@EnableConfigurationProperties(TelegramProperties.class)
class TelegramConfig {

	@Bean
	TelegramClient telegramClient(TelegramProperties properties) {
		return new TelegramClient(RestClient.builder(), properties);
	}
}
