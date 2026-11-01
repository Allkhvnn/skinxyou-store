package skinxyou_backend.integration.billz.client;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BillzProperties.class)
class BillzClientConfig {

	@Bean
	BillzClient billzClient(BillzProperties properties) {
		RestClient restClient = BillzClient.configure(RestClient.builder(), properties).build();
		return new BillzClient(restClient, properties, Clock.systemUTC());
	}
}
