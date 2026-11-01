package skinxyou_backend.integration.billz.client;

import java.time.Duration;

/**
 * Spaces out calls so the whole application stays under the BILLZ limit. The limit is shared by all
 * traffic of the integration, so a single instance must be used for every request, including login.
 */
class BillzRateLimiter {

	private final long minIntervalNanos;
	private long nextAllowedAt = System.nanoTime();

	BillzRateLimiter(Duration minInterval) {
		this.minIntervalNanos = minInterval.toNanos();
	}

	synchronized void acquire() {
		long waitNanos = nextAllowedAt - System.nanoTime();
		if (waitNanos > 0) {
			sleep(waitNanos);
		}
		nextAllowedAt = System.nanoTime() + minIntervalNanos;
	}

	private static void sleep(long nanos) {
		try {
			Thread.sleep(Duration.ofNanos(nanos));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BillzApiException("Interrupted while waiting for BILLZ rate limit", e);
		}
	}
}
