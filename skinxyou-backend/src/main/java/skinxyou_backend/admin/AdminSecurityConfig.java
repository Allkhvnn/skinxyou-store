package skinxyou_backend.admin;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/** Signed (HS256) admin tokens: issued at login, validated on every /api/admin request. */
@Slf4j
@Configuration
class AdminSecurityConfig {

	static final String ROLE_CLAIM = "role";
	static final String NAME_CLAIM = "name";

	private static final int MIN_SECRET_LENGTH = 32;

	@Bean
	SecretKey adminJwtKey(AdminAuthProperties properties) {
		String secret = properties.jwtSecret();
		if (secret == null || secret.isBlank()) {
			// Development only: production config requires ADMIN_JWT_SECRET (no default).
			log.warn("ADMIN_JWT_SECRET is not set; using a random key, admin logins reset on restart");
			byte[] random = new byte[MIN_SECRET_LENGTH];
			new SecureRandom().nextBytes(random);
			return new SecretKeySpec(random, "HmacSHA256");
		}
		if (secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException("ADMIN_JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder adminJwtEncoder(SecretKey adminJwtKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(adminJwtKey));
	}

	@Bean
	JwtDecoder adminJwtDecoder(SecretKey adminJwtKey) {
		return NimbusJwtDecoder.withSecretKey(adminJwtKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Maps the "role" claim to ROLE_ADMIN / ROLE_MANAGER authorities. */
	@Bean
	JwtAuthenticationConverter adminJwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(ROLE_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
