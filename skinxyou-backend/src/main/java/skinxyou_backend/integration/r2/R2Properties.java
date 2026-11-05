package skinxyou_backend.integration.r2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare R2 bucket for photos uploaded in the admin panel.
 *
 * @param publicUrl base URL the bucket is served from (r2.dev subdomain now, own domain later)
 */
@ConfigurationProperties("r2")
public record R2Properties(String endpoint, String bucket, String accessKeyId, String secretAccessKey,
		String publicUrl) {

	public boolean isConfigured() {
		return notBlank(endpoint) && notBlank(bucket) && notBlank(accessKeyId) && notBlank(secretAccessKey)
				&& notBlank(publicUrl);
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
