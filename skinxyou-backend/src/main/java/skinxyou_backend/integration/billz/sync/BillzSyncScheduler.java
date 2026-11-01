package skinxyou_backend.integration.billz.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs only when {@code billz.sync.enabled=true}; a single backend instance is assumed. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "billz.sync.enabled", havingValue = "true")
@RequiredArgsConstructor
class BillzSyncScheduler {

	private final BillzCatalogSyncService syncService;

	@Scheduled(initialDelayString = "${billz.sync.initial-delay}", fixedDelayString = "${billz.sync.delta-interval}")
	void delta() {
		syncService.runDelta();
	}

	@Scheduled(cron = "${billz.sync.full-cron}", zone = "Asia/Almaty")
	void full() {
		syncService.runFull();
	}
}
