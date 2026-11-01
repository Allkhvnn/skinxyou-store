package skinxyou_backend.admin;

import java.time.Clock;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator from ADMIN_BOOTSTRAP_* variables, only while there are no staff
 * accounts at all. Later changes to these variables have no effect on existing accounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminUserBootstrap implements ApplicationRunner {

	private static final int MIN_PASSWORD_LENGTH = 10;

	private final AdminUserRepository repository;
	private final AdminAuthProperties properties;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		String username = properties.bootstrapUsername();
		String password = properties.bootstrapPassword();
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			log.warn("No admin accounts exist; set ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD to create one");
			return;
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			log.error("ADMIN_BOOTSTRAP_PASSWORD must be at least {} characters; admin account not created",
					MIN_PASSWORD_LENGTH);
			return;
		}
		AdminUser admin = new AdminUser();
		admin.setUsername(username.trim().toLowerCase(Locale.ROOT));
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setDisplayName(properties.bootstrapDisplayName() == null || properties.bootstrapDisplayName().isBlank()
				? username.trim()
				: properties.bootstrapDisplayName().trim());
		admin.setRole(AdminRole.ADMIN);
		admin.setCreatedAt(clock.instant());
		repository.save(admin);
		log.info("Created initial admin account '{}'", admin.getUsername());
	}
}
