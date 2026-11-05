package skinxyou_backend.admin;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

	private static final int MAX_DAYS = 366;

	private final AdminAnalyticsService service;
	private final Clock clock;

	/** Without {@code from}: the last {@code days} days up to {@code to} (default today, Almaty time). */
	@GetMapping
	public AdminAnalyticsService.Report report(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "30") int days) {
		if (days < 1 || days > MAX_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Период должен быть от 1 до " + MAX_DAYS + " дней");
		}
		LocalDate end = to != null ? to : AdminAnalyticsService.today(clock.instant());
		LocalDate start = from != null ? from : end.minusDays(days - 1);
		if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Период должен быть от 1 до " + MAX_DAYS + " дней");
		}
		return service.report(start, end);
	}
}
