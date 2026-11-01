package skinxyou_backend.common.security;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import skinxyou_backend.common.web.RateLimitFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter,
			JwtAuthenticationConverter adminJwtAuthenticationConverter) throws Exception {
		return http
				// Stateless REST API: no cookies (admin auth is a Bearer token), so CSRF tokens are not applicable.
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.addFilterBefore(rateLimitFilter, SecurityContextHolderFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/catalog/**", "/api/checkout/options").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/checkout/quote", "/api/orders").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
						.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers("/error").permitAll()
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(adminJwtAuthenticationConverter)))
				.build();
	}

	/**
	 * Not registered as a servlet filter on its own; it runs only inside the security chain.
	 */
	@Bean
	RateLimitFilter rateLimitFilter(Clock clock,
			@Value("${shop.rate-limit.orders-per-10-minutes}") int ordersPer10Minutes,
			@Value("${shop.rate-limit.quotes-per-minute}") int quotesPerMinute) {
		return new RateLimitFilter(List.of(
				new RateLimitFilter.Rule("POST", "/api/orders", ordersPer10Minutes, Duration.ofMinutes(10)),
				new RateLimitFilter.Rule("POST", "/api/checkout/quote", quotesPerMinute, Duration.ofMinutes(1)),
				// Password guessing protection.
				new RateLimitFilter.Rule("POST", "/api/admin/auth/login", 10, Duration.ofMinutes(10))),
				clock);
	}

	@Bean
	FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	/** The storefront calls checkout endpoints from the browser. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${shop.cors.allowed-origins}") List<String> origins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origins);
		config.setAllowedMethods(List.of("GET", "POST"));
		config.setAllowedHeaders(List.of("Content-Type", "Accept"));
		config.setMaxAge(Duration.ofHours(1));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}
}
