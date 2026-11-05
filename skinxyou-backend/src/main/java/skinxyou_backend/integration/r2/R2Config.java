package skinxyou_backend.integration.r2;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
class R2Config {

	@Bean
	ImageStorage imageStorage(R2Properties properties) {
		return new R2ImageStorage(properties);
	}
}
