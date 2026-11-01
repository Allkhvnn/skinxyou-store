package skinxyou_backend.integration.billz.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import skinxyou_backend.integration.billz.dto.BillzLoginResponse;
import skinxyou_backend.integration.billz.dto.BillzProductPage;
import skinxyou_backend.integration.billz.dto.BillzShopPage;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Read-only client for the BILLZ public API (https://docs.billz.io).
 *
 * <p>Auth: the integration key is exchanged for an access token via {@code POST /v1/auth/login}. We
 * deliberately do not use the refresh-token flow: BILLZ revokes the whole session when a refresh token
 * is reused, so on expiry or 401 we simply log in again with the key.
 *
 * <p>Retries: 429, 5xx and network errors are retried with exponential backoff; 401 triggers one
 * re-login. Other 4xx are request/permission errors and fail immediately.
 */
@Slf4j
public class BillzClient {

	static final DateTimeFormatter LAST_UPDATED_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

	private static final Duration TOKEN_RENEW_MARGIN = Duration.ofHours(1);
	private static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofDays(1);

	private final RestClient restClient;
	private final BillzProperties properties;
	private final BillzRateLimiter rateLimiter;
	private final Clock clock;

	private String accessToken;
	private Instant tokenRenewAt = Instant.MIN;

	public BillzClient(RestClient restClient, BillzProperties properties, Clock clock) {
		this.restClient = restClient;
		this.properties = properties;
		this.rateLimiter = new BillzRateLimiter(properties.minRequestInterval());
		this.clock = clock;
	}

	/** Applies base URL, headers, timeouts and snake_case JSON mapping to the builder. */
	public static RestClient.Builder configure(RestClient.Builder builder, BillzProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());

		JsonMapper mapper = JsonMapper.builder()
				.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
				.build();

		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader("platform-id", properties.platformId())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.configureMessageConverters(converters ->
						converters.withJsonConverter(new JacksonJsonHttpMessageConverter(mapper)));
	}

	/**
	 * Out-of-stock products are always included, so a full export never mistakes them for removed ones.
	 *
	 * @param updatedSince when set, returns only products changed since this moment (delta sync);
	 *                     deletions are not reported by BILLZ in delta mode
	 */
	public BillzProductPage getProducts(int page, int limit, Instant updatedSince) {
		return get(BillzProductPage.class, uri -> {
			uri.path("/v2/products")
					.queryParam("page", page)
					.queryParam("limit", limit)
					.queryParam("include_empty_stocks", true);
			if (updatedSince != null) {
				uri.queryParam("last_updated_date", LAST_UPDATED_FORMAT.format(updatedSince));
			}
			return uri.build();
		});
	}

	public BillzShopPage getShops(int page, int limit) {
		return get(BillzShopPage.class, uri -> uri.path("/v1/shop")
				.queryParam("page", page)
				.queryParam("limit", limit)
				.build());
	}

	private <T> T get(Class<T> type, Function<UriBuilder, URI> uri) {
		return execute(token -> restClient.get()
				.uri(uri)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.retrieve()
				.body(type), true);
	}

	private <T> T execute(Function<String, T> call, boolean authenticated) {
		boolean reloggedIn = false;
		int attempt = 1;
		while (true) {
			String token = authenticated ? token() : null;
			rateLimiter.acquire();
			try {
				return call.apply(token);
			}
			catch (RestClientResponseException e) {
				int status = e.getStatusCode().value();
				if (status == 401 && authenticated && !reloggedIn) {
					log.info("BILLZ returned 401, logging in again");
					invalidateToken();
					reloggedIn = true;
					continue;
				}
				if (!isRetryable(status) || attempt >= properties.maxAttempts()) {
					throw new BillzApiException(status, "BILLZ request failed with " + status + ": "
							+ abbreviate(e.getResponseBodyAsString()));
				}
				log.warn("BILLZ returned {}, retrying (attempt {}/{})", status, attempt, properties.maxAttempts());
			}
			catch (ResourceAccessException e) {
				if (attempt >= properties.maxAttempts()) {
					throw new BillzApiException("BILLZ is unreachable: " + e.getMessage(), e);
				}
				log.warn("BILLZ is unreachable, retrying (attempt {}/{}): {}", attempt, properties.maxAttempts(),
						e.getMessage());
			}
			backoff(attempt++);
		}
	}

	private synchronized String token() {
		if (accessToken != null && clock.instant().isBefore(tokenRenewAt)) {
			return accessToken;
		}
		if (!properties.isConfigured()) {
			throw new BillzApiException(0, "BILLZ integration key is not configured (BILLZ_SECRET_TOKEN)");
		}
		BillzLoginResponse response = execute(ignored -> restClient.post()
				.uri("/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("secret_token", properties.secretToken()))
				.retrieve()
				.body(BillzLoginResponse.class), false);

		if (response == null || response.data() == null || response.data().accessToken() == null) {
			throw new BillzApiException(0, "BILLZ login response contains no access token");
		}
		Duration lifetime = response.data().expiresIn() != null
				? Duration.ofSeconds(response.data().expiresIn())
				: DEFAULT_TOKEN_LIFETIME;
		accessToken = response.data().accessToken();
		tokenRenewAt = clock.instant().plus(lifetime).minus(TOKEN_RENEW_MARGIN);
		log.info("Obtained BILLZ access token, renewing at {}", tokenRenewAt);
		return accessToken;
	}

	private synchronized void invalidateToken() {
		accessToken = null;
	}

	private static boolean isRetryable(int status) {
		return status == 429 || status >= 500;
	}

	private void backoff(int attempt) {
		long millis = properties.initialBackoff().toMillis() << (attempt - 1);
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BillzApiException("Interrupted during BILLZ retry backoff", e);
		}
	}

	private static String abbreviate(String body) {
		return body.length() <= 300 ? body : body.substring(0, 300) + "...";
	}
}
