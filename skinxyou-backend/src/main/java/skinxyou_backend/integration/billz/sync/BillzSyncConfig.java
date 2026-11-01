package skinxyou_backend.integration.billz.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillzSyncProperties.class)
class BillzSyncConfig {
}
