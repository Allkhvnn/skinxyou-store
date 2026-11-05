package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Store figures for a date range, in Almaty time. Revenue counts only paid, non-cancelled orders;
 * "orders" counts everything that was not cancelled.
 */
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

	static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
	private static final int TOP_PRODUCTS = 10;

	private final NamedParameterJdbcTemplate jdbc;

	public record Summary(long orders, long paidOrders, long cancelledOrders, BigDecimal revenue,
			BigDecimal awaitingPayment, BigDecimal averageCheck, long courierOrders, long pickupOrders) {
	}

	public record Day(LocalDate date, long orders, BigDecimal revenue) {
	}

	public record TopProduct(String slug, String name, long quantity, BigDecimal revenue) {
	}

	public record Report(LocalDate from, LocalDate to, Summary summary, List<Day> days, List<TopProduct> topProducts) {
	}

	/** Both dates inclusive. */
	@Transactional(readOnly = true)
	public Report report(LocalDate from, LocalDate to) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("from", from.atStartOfDay(ZONE).toInstant().atOffset(java.time.ZoneOffset.UTC))
				.addValue("to", to.plusDays(1).atStartOfDay(ZONE).toInstant().atOffset(java.time.ZoneOffset.UTC))
				.addValue("zone", ZONE.getId());

		Summary summary = jdbc.queryForObject("""
				select
				  count(*) filter (where status <> 'CANCELLED')                                   as orders,
				  count(*) filter (where status <> 'CANCELLED' and payment_status = 'PAID')        as paid,
				  count(*) filter (where status = 'CANCELLED')                                     as cancelled,
				  coalesce(sum(total) filter (where status <> 'CANCELLED' and payment_status = 'PAID'), 0)    as revenue,
				  coalesce(sum(total) filter (where status <> 'CANCELLED' and payment_status = 'PENDING'), 0) as awaiting,
				  count(*) filter (where status <> 'CANCELLED' and delivery_method = 'COURIER')    as courier,
				  count(*) filter (where status <> 'CANCELLED' and delivery_method = 'PICKUP')     as pickup
				from customer_order
				where created_at >= :from and created_at < :to
				""", params, (rs, i) -> {
			long paid = rs.getLong("paid");
			BigDecimal revenue = rs.getBigDecimal("revenue");
			return new Summary(rs.getLong("orders"), paid, rs.getLong("cancelled"), revenue,
					rs.getBigDecimal("awaiting"),
					paid == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(paid), 0, RoundingMode.HALF_UP),
					rs.getLong("courier"), rs.getLong("pickup"));
		});

		Map<LocalDate, Day> byDate = new LinkedHashMap<>();
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			byDate.put(d, new Day(d, 0, BigDecimal.ZERO));
		}
		jdbc.query("""
				select (created_at at time zone :zone)::date as day,
				       count(*) filter (where status <> 'CANCELLED') as orders,
				       coalesce(sum(total) filter (where status <> 'CANCELLED' and payment_status = 'PAID'), 0) as revenue
				from customer_order
				where created_at >= :from and created_at < :to
				group by day
				""", params, rs -> {
			LocalDate day = rs.getObject("day", Date.class).toLocalDate();
			byDate.put(day, new Day(day, rs.getLong("orders"), rs.getBigDecimal("revenue")));
		});

		List<TopProduct> top = jdbc.query("""
				select i.product_slug as slug, max(i.product_name) as name,
				       sum(i.quantity) as quantity, sum(i.line_total) as revenue
				from order_item i join customer_order o on o.id = i.order_id
				where o.created_at >= :from and o.created_at < :to and o.status <> 'CANCELLED'
				group by i.product_slug
				order by quantity desc, revenue desc
				limit %d
				""".formatted(TOP_PRODUCTS), params,
				(rs, i) -> new TopProduct(rs.getString("slug"), rs.getString("name"), rs.getLong("quantity"),
						rs.getBigDecimal("revenue")));

		return new Report(from, to, summary, new ArrayList<>(byDate.values()), top);
	}

	static LocalDate today(Instant now) {
		return now.atZone(ZONE).toLocalDate();
	}
}
