package skinxyou_backend.common.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

	private Instant now = Instant.parse("2026-11-01T10:00:00Z");

	private final RateLimitFilter filter = new RateLimitFilter(
			List.of(new RateLimitFilter.Rule("POST", "/api/orders", 2, Duration.ofMinutes(10))),
			new Clock() {
				@Override
				public Instant instant() {
					return now;
				}

				@Override
				public java.time.ZoneId getZone() {
					return ZoneOffset.UTC;
				}

				@Override
				public Clock withZone(java.time.ZoneId zone) {
					return this;
				}
			});

	@Test
	void limitsPerIpWithinTheWindow() throws Exception {
		assertThat(call("POST", "/api/orders", "1.1.1.1")).isEqualTo(200);
		assertThat(call("POST", "/api/orders", "1.1.1.1")).isEqualTo(200);
		assertThat(call("POST", "/api/orders", "1.1.1.1")).isEqualTo(429);
		// Other clients and other endpoints are unaffected.
		assertThat(call("POST", "/api/orders", "2.2.2.2")).isEqualTo(200);
		assertThat(call("GET", "/api/orders", "1.1.1.1")).isEqualTo(200);

		now = now.plus(Duration.ofMinutes(11));
		assertThat(call("POST", "/api/orders", "1.1.1.1")).isEqualTo(200);
	}

	private int call(String method, String path, String ip) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRemoteAddr(ip);
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
