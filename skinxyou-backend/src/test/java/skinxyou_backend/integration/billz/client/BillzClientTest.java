package skinxyou_backend.integration.billz.client;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import skinxyou_backend.integration.billz.dto.BillzProduct;
import skinxyou_backend.integration.billz.dto.BillzProductPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BillzClientTest {

	private static final String BASE_URL = "https://billz.test";
	private static final String PLATFORM_ID = "platform-123";

	private static final String LOGIN_RESPONSE = """
			{"code":200,"message":"ok","error":null,
			 "data":{"access_token":"%s","refresh_token":"r","token_type":"Bearer","expires_in":1296000}}
			""";

	private static final String PRODUCTS_RESPONSE = """
			{"count":1,"products":[{
			  "id":"p-1","parent_id":"","name":"Cleanser","sku":"SKU-1","barcode":"4600000000001",
			  "brand_id":"b-1","brand_name":"COSRX","is_variative":false,
			  "categories":[{"id":"c-1","name":"Face","parent_id":""}],
			  "shop_measurement_values":[{"shop_id":"s-1","shop_name":"Main","active_measurement_value":7}],
			  "shop_prices":[{"shop_id":"s-1","shop_name":"Main","retail_price":8900,"retail_currency":"KZT",
			                  "promo_price":0,"promos":null,"supply_price":5000}],
			  "photos":[{"photo_url":"https://cdn/x.jpg","sequence":1,"is_main":true}],
			  "unknown_field":"ignored"
			}]}
			""";

	private final BillzProperties properties = properties("secret-key");

	@Test
	void logsInOnceAndReusesTokenForSubsequentRequests() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andExpect(header("Authorization", "Bearer token-1"))
				.andRespond(withSuccess("{\"count\":1,\"shops\":[{\"id\":\"s-1\",\"name\":\"Main\"}]}",
						MediaType.APPLICATION_JSON));
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=2&limit=10"))
				.andExpect(header("Authorization", "Bearer token-1"))
				.andRespond(withSuccess("{\"count\":1,\"shops\":[]}", MediaType.APPLICATION_JSON));

		assertThat(f.client.getShops(1, 10).shops()).extracting("id").containsExactly("s-1");
		assertThat(f.client.getShops(2, 10).shops()).isEmpty();
		f.server.verify();
	}

	@Test
	void mapsSnakeCaseProductFieldsAndSendsDeltaFilterInUtc() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		f.server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/products")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("platform-id", PLATFORM_ID))
				.andExpect(queryParam("page", "3"))
				.andExpect(queryParam("limit", "100"))
				.andExpect(queryParam("last_updated_date", "2026-08-14%2005:30:00"))
				.andRespond(withSuccess(PRODUCTS_RESPONSE, MediaType.APPLICATION_JSON));

		BillzProductPage page = f.client.getProducts(3, 100, Instant.parse("2026-08-14T05:30:00Z"));

		assertThat(page.count()).isEqualTo(1);
		BillzProduct product = page.products().getFirst();
		assertThat(product.id()).isEqualTo("p-1");
		assertThat(product.brandName()).isEqualTo("COSRX");
		assertThat(product.categories()).extracting("name").containsExactly("Face");
		assertThat(product.shopMeasurementValues().getFirst().activeMeasurementValue()).isEqualByComparingTo("7");
		assertThat(product.shopPrices().getFirst().retailPrice()).isEqualByComparingTo(new BigDecimal("8900"));
		assertThat(product.shopPrices().getFirst().retailCurrency()).isEqualTo("KZT");
		assertThat(product.photos().getFirst().isMain()).isTrue();
		f.server.verify();
	}

	@Test
	void logsInAgainOnceWhenTokenIsRejected() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andExpect(header("Authorization", "Bearer token-1"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));
		expectLogin(f.server, "token-2");
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andExpect(header("Authorization", "Bearer token-2"))
				.andRespond(withSuccess("{\"count\":0,\"shops\":[]}", MediaType.APPLICATION_JSON));

		assertThat(f.client.getShops(1, 10).count()).isZero();
		f.server.verify();
	}

	@Test
	void retriesRateLimitedAndServerErrors() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY));
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andRespond(withSuccess("{\"count\":0,\"shops\":[]}", MediaType.APPLICATION_JSON));

		assertThat(f.client.getShops(1, 10).count()).isZero();
		f.server.verify();
	}

	@Test
	void givesUpAfterMaxAttempts() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		for (int i = 0; i < properties.maxAttempts(); i++) {
			f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
					.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		}

		assertThatThrownBy(() -> f.client.getShops(1, 10))
				.isInstanceOf(BillzApiException.class)
				.extracting("status").isEqualTo(503);
		f.server.verify();
	}

	@Test
	void doesNotRetryClientErrors() {
		Fixture f = new Fixture(properties);
		expectLogin(f.server, "token-1");
		f.server.expect(once(), requestTo(BASE_URL + "/v1/shop?page=1&limit=10"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"message\":\"no permission\"}"));

		assertThatThrownBy(() -> f.client.getShops(1, 10))
				.isInstanceOf(BillzApiException.class)
				.hasMessageContaining("403")
				.extracting("status").isEqualTo(403);
		f.server.verify();
	}

	@Test
	void failsWithoutCallingBillzWhenKeyIsMissing() {
		Fixture f = new Fixture(properties(""));

		assertThatThrownBy(() -> f.client.getShops(1, 10))
				.isInstanceOf(BillzApiException.class)
				.hasMessageContaining("BILLZ_SECRET_TOKEN");
		f.server.verify();
	}

	private static void expectLogin(MockRestServiceServer server, String token) {
		server.expect(once(), requestTo(BASE_URL + "/v1/auth/login"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("platform-id", PLATFORM_ID))
				.andExpect(content().json("{\"secret_token\":\"secret-key\"}"))
				.andRespond(withSuccess(LOGIN_RESPONSE.formatted(token), MediaType.APPLICATION_JSON));
	}

	private static BillzProperties properties(String secretToken) {
		return new BillzProperties(BASE_URL, secretToken, PLATFORM_ID,
				Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ZERO, 4, Duration.ZERO);
	}

	private static final class Fixture {

		final MockRestServiceServer server;
		final BillzClient client;

		Fixture(BillzProperties properties) {
			RestClient.Builder builder = BillzClient.configure(RestClient.builder(), properties);
			server = MockRestServiceServer.bindTo(builder).build();
			client = new BillzClient(builder.build(), properties,
					Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC));
		}
	}
}
