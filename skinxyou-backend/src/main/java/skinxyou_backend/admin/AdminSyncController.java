package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.common.web.PageResponse;
import skinxyou_backend.integration.billz.sync.BillzCatalogSyncService;
import skinxyou_backend.integration.billz.sync.BillzSyncRun;
import skinxyou_backend.integration.billz.sync.BillzSyncRunRepository;
import skinxyou_backend.integration.billz.sync.SyncStatus;
import skinxyou_backend.integration.billz.sync.SyncType;

/** BILLZ sync monitoring for staff: health, run log and manual start. */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

	private static final int RECENT_RUNS = 30;
	private static final Duration STALE_AFTER = Duration.ofHours(1);

	private final Clock clock;
	private final BillzCatalogSyncService syncService;
	private final BillzSyncRunRepository runRepository;
	private final ProductRepository productRepository;

	public record Run(Long id, SyncType type, SyncStatus status, Instant startedAt, Instant finishedAt,
			Long durationMs, int productsFetched, int productsCreated, int productsUpdated, int productsDeactivated,
			String errorMessage) {

		static Run of(BillzSyncRun r) {
			return new Run(r.getId(), r.getType(), r.getStatus(), r.getStartedAt(), r.getFinishedAt(),
					r.getFinishedAt() == null ? null : Duration.between(r.getStartedAt(), r.getFinishedAt()).toMillis(),
					r.getProductsFetched(), r.getProductsCreated(), r.getProductsUpdated(),
					r.getProductsDeactivated(), r.getErrorMessage());
		}
	}

	/**
	 * @param onSite        visible on the storefront
	 * @param removedInBillz delisted because they disappeared from BILLZ
	 */
	public record CatalogStats(long total, long onSite, long inStock, long hidden, long removedInBillz) {
	}

	/**
	 * @param stale no successful sync for over an hour (delta runs every 15 minutes), so prices and stock
	 *              on the site may be outdated
	 */
	public record Status(boolean running, boolean stale, Instant lastSuccessAt, Run lastRun, CatalogStats catalog) {
	}

	public record StartRequest(@NotNull SyncType type) {
	}

	@GetMapping("/status")
	@Transactional(readOnly = true)
	public Status status() {
		Instant lastSuccessAt = runRepository.findFirstByStatusOrderByStartedAtDesc(SyncStatus.SUCCESS)
				.map(BillzSyncRun::getFinishedAt)
				.orElse(null);
		return new Status(
				syncService.isRunning(),
				lastSuccessAt == null || lastSuccessAt.isBefore(clock.instant().minus(STALE_AFTER)),
				lastSuccessAt,
				runRepository.findAllByOrderByIdDesc(PageRequest.of(0, 1)).stream().findFirst().map(Run::of).orElse(null),
				new CatalogStats(
						productRepository.count(),
						productRepository.countByAvailableInBillzTrueAndHiddenFalse(),
						productRepository.countByAvailableInBillzTrueAndHiddenFalseAndStockGreaterThan(BigDecimal.ZERO),
						productRepository.countByHiddenTrue(),
						productRepository.countByAvailableInBillzFalse()));
	}

	@GetMapping("/runs")
	@Transactional(readOnly = true)
	public PageResponse<Run> runs() {
		return PageResponse.of(runRepository.findAllByOrderByIdDesc(PageRequest.of(0, RECENT_RUNS)), Run::of);
	}

	/** 202: started in the background; 409: a sync is already running. */
	@PostMapping("/run")
	public ResponseEntity<Void> start(@RequestBody @Valid StartRequest request, @AuthenticationPrincipal Jwt jwt) {
		if (!syncService.startInBackground(request.type())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		log.info("Manual BILLZ {} sync started by {}", request.type(), jwt.getSubject());
		return ResponseEntity.accepted().build();
	}
}
