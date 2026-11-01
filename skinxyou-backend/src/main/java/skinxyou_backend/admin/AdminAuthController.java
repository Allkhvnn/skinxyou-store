package skinxyou_backend.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final AdminAuthProperties properties;
	private final Clock clock;

	/** Checked when the username is unknown, so response time does not reveal which usernames exist. */
	private volatile String dummyHash;

	public record LoginRequest(@NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 200) String password) {
	}

	public record LoginResponse(String token, Instant expiresAt, String username, String displayName, AdminRole role) {
	}

	@PostMapping("/login")
	@Transactional
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		String username = request.username().trim().toLowerCase(Locale.ROOT);
		Optional<AdminUser> user = repository.findByUsername(username).filter(AdminUser::isActive);
		boolean passwordMatches = passwordEncoder.matches(request.password(),
				user.map(AdminUser::getPasswordHash).orElseGet(this::dummyHash));
		if (user.isEmpty() || !passwordMatches) {
			log.warn("Failed admin login for '{}'", username);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
		}

		AdminUser admin = user.get();
		Instant now = clock.instant();
		admin.setLastLoginAt(now);
		Instant expiresAt = now.plus(properties.tokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(admin.getUsername())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim(AdminSecurityConfig.ROLE_CLAIM, admin.getRole().name())
				.claim(AdminSecurityConfig.NAME_CLAIM, admin.getDisplayName())
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		log.info("Admin '{}' logged in", admin.getUsername());
		return new LoginResponse(token, expiresAt, admin.getUsername(), admin.getDisplayName(), admin.getRole());
	}

	private String dummyHash() {
		if (dummyHash == null) {
			dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
		}
		return dummyHash;
	}
}
