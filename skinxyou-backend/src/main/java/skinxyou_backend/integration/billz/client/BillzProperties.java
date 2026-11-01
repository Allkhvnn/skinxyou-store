package skinxyou_backend.integration.billz.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secretToken  integration key from BILLZ (Settings → Company → Integration keys); never commit it
 * @param platformId   static platform identifier BILLZ requires for integrations
 * @param minRequestInterval BILLZ allows 2 requests/second per integration, so at least 500ms between calls
 */
@ConfigurationProperties("billz")
public record BillzProperties(
		String baseUrl,
		String secretToken,
		String platformId,
		Duration connectTimeout,
		Duration readTimeout,
		Duration minRequestInterval,
		int maxAttempts,
		Duration initialBackoff) {

	public boolean isConfigured() {
		return secretToken != null && !secretToken.isBlank();
	}
}
