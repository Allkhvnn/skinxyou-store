package skinxyou_backend.integration.billz.sync;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled         turns on the scheduled sync jobs
 * @param shopId          BILLZ shop whose prices and stock the storefront shows
 * @param pageSize        products per BILLZ request (BILLZ max is 300)
 * @param deltaOverlap    how far back before the last successful run a delta re-reads, to absorb
 *                        clock skew; re-reading is harmless because import is idempotent
 * @param maxFullSyncDrop a full export smaller than this share of currently available products is
 *                        treated as broken and not applied (e.g. 0.5 = refuse to delist half the catalog)
 */
@ConfigurationProperties("billz.sync")
public record BillzSyncProperties(
		boolean enabled,
		String shopId,
		int pageSize,
		Duration deltaOverlap,
		double maxFullSyncDrop) {
}
