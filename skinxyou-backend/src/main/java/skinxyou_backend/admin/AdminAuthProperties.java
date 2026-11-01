package skinxyou_backend.admin;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret         HMAC key for admin tokens, at least 32 characters; required in production
 * @param tokenTtl          how long a login stays valid
 * @param bootstrapUsername first administrator, created on startup only while no staff accounts exist
 */
@ConfigurationProperties("admin")
public record AdminAuthProperties(
		String jwtSecret,
		Duration tokenTtl,
		String bootstrapUsername,
		String bootstrapPassword,
		String bootstrapDisplayName) {
}
