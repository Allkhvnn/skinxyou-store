package skinxyou_backend.common.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory sliding-window limit per client IP for write endpoints (abuse and bot protection).
 * Enough for a single backend instance; the IP comes from the reverse proxy in production
 * ({@code server.forward-headers-strategy}).
 */
public class RateLimitFilter extends OncePerRequestFilter {

	public record Rule(String method, String path, int limit, Duration window) {
	}

	private static final int CLEANUP_EVERY = 1000;

	private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
	private final AtomicInteger requestsSinceCleanup = new AtomicInteger();
	private final List<Rule> rules;
	private final Clock clock;

	public RateLimitFilter(List<Rule> rules, Clock clock) {
		this.rules = rules;
		this.clock = clock;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Rule rule = rules.stream()
				.filter(r -> r.method().equalsIgnoreCase(request.getMethod()) && r.path().equals(request.getRequestURI()))
				.findFirst()
				.orElse(null);
		if (rule != null && !allow(rule, request.getRemoteAddr())) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("""
					{"status":429,"title":"Too Many Requests","detail":"Слишком много запросов. Попробуйте позже."}""");
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean allow(Rule rule, String ip) {
		long now = clock.millis();
		long windowStart = now - rule.window().toMillis();
		Deque<Long> timestamps = hits.computeIfAbsent(rule.path() + "|" + ip, k -> new ArrayDeque<>());
		boolean allowed;
		synchronized (timestamps) {
			while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
				timestamps.pollFirst();
			}
			allowed = timestamps.size() < rule.limit();
			if (allowed) {
				timestamps.addLast(now);
			}
		}
		if (requestsSinceCleanup.incrementAndGet() >= CLEANUP_EVERY) {
			requestsSinceCleanup.set(0);
			removeIdleClients(now);
		}
		return allowed;
	}

	/** Drops idle clients so the map cannot grow without bound. */
	private void removeIdleClients(long now) {
		long maxWindow = rules.stream().mapToLong(r -> r.window().toMillis()).max().orElse(0);
		hits.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				Long last = e.getValue().peekLast();
				return last == null || last < now - maxWindow;
			}
		});
	}
}
