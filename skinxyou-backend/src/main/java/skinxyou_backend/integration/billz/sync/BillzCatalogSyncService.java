package skinxyou_backend.integration.billz.sync;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.integration.billz.client.BillzClient;
import skinxyou_backend.integration.billz.dto.BillzProduct;
import skinxyou_backend.integration.billz.dto.BillzProductPage;

/**
 * Pulls the BILLZ catalog into PostgreSQL and records every run in {@code billz_sync_run}.
 *
 * <p>Delta runs import page by page. Full runs first download the whole export and verify it is
 * complete before writing anything, because a full run also delists products missing from BILLZ.
 */
@Slf4j
@Service
public class BillzCatalogSyncService {

	/** BILLZ rejects page numbers above this. */
	private static final int MAX_PAGES = 10_000;

	private final BillzClient billzClient;
	private final BillzProductImporter importer;
	private final ProductRepository productRepository;
	private final BillzSyncRunRepository runRepository;
	private final BillzSyncProperties properties;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;
	private final ApplicationEventPublisher events;
	private final TaskExecutor executor;
	private final AtomicBoolean running = new AtomicBoolean();

	public BillzCatalogSyncService(BillzClient billzClient, BillzProductImporter importer,
			ProductRepository productRepository, BillzSyncRunRepository runRepository,
			BillzSyncProperties properties, TransactionTemplate transactionTemplate, Clock clock,
			ApplicationEventPublisher events, @Qualifier("applicationTaskExecutor") TaskExecutor executor) {
		this.executor = executor;
		this.billzClient = billzClient;
		this.importer = importer;
		this.productRepository = productRepository;
		this.runRepository = runRepository;
		this.properties = properties;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
		this.events = events;
	}

	/** Imports changes since the last successful run; falls back to a full run if there was none. */
	public Optional<BillzSyncRun> runDelta() {
		return runExclusively(SyncType.DELTA);
	}

	public Optional<BillzSyncRun> runFull() {
		return runExclusively(SyncType.FULL);
	}

	/**
	 * For manual starts: claims the sync slot before returning, so callers immediately see
	 * {@link #isRunning()} as true, then does the work on the executor.
	 *
	 * @return false when a sync is already running
	 */
	public boolean startInBackground(SyncType type) {
		if (!running.compareAndSet(false, true)) {
			return false;
		}
		try {
			executor.execute(() -> {
				try {
					runClaimed(type);
				}
				finally {
					running.set(false);
				}
			});
		}
		catch (RuntimeException e) {
			running.set(false);
			throw e;
		}
		return true;
	}

	public boolean isRunning() {
		return running.get();
	}

	private Optional<BillzSyncRun> runExclusively(SyncType type) {
		if (!running.compareAndSet(false, true)) {
			log.info("BILLZ sync already in progress, skipping {} run", type);
			return Optional.empty();
		}
		try {
			return Optional.of(runClaimed(type));
		}
		finally {
			running.set(false);
		}
	}

	/** Runs with the sync slot already held by the caller. */
	private BillzSyncRun runClaimed(SyncType type) {
		if (type == SyncType.FULL) {
			return run(SyncType.FULL, null);
		}
		Optional<BillzSyncRun> lastSuccess = runRepository.findFirstByStatusOrderByStartedAtDesc(SyncStatus.SUCCESS);
		if (lastSuccess.isEmpty()) {
			log.info("No successful BILLZ sync yet, running full sync instead of delta");
			return run(SyncType.FULL, null);
		}
		return run(SyncType.DELTA, lastSuccess.get().getStartedAt().minus(properties.deltaOverlap()));
	}

	private BillzSyncRun run(SyncType type, Instant updatedSince) {
		BillzSyncRun run = runRepository.save(new BillzSyncRun(type, clock.instant(), updatedSince));
		try {
			if (type == SyncType.FULL) {
				full(run);
			}
			else {
				delta(run);
			}
			run.succeed(clock.instant());
			log.info("BILLZ {} sync finished: fetched={}, created={}, updated={}, deactivated={}", type,
					run.getProductsFetched(), run.getProductsCreated(), run.getProductsUpdated(),
					run.getProductsDeactivated());
		}
		catch (RuntimeException e) {
			run.fail(clock.instant(), e.getMessage());
			log.error("BILLZ {} sync failed", type, e);
		}
		BillzSyncRun saved = runRepository.save(run);
		publishHealthChange(saved);
		return saved;
	}

	/** Only a switch between working and broken is announced, not every failed 15-minute run. */
	private void publishHealthChange(BillzSyncRun run) {
		SyncStatus previous = runRepository
				.findFirstByStatusNotAndIdLessThanOrderByIdDesc(SyncStatus.RUNNING, run.getId())
				.map(BillzSyncRun::getStatus)
				.orElse(SyncStatus.SUCCESS);
		if (previous != run.getStatus()) {
			events.publishEvent(new SyncHealthChangedEvent(run.getStatus() == SyncStatus.SUCCESS, run.getType(),
					run.getErrorMessage()));
		}
	}

	private void delta(BillzSyncRun run) {
		int page = 1;
		int fetched = 0;
		BillzProductPage response;
		do {
			response = billzClient.getProducts(page, properties.pageSize(), run.getUpdatedSince());
			fetched += response.products().size();
			importPage(run, response.products());
		}
		while (hasMorePages(response, fetched, page++));
		run.setProductsFetched(fetched);
	}

	private void full(BillzSyncRun run) {
		// Keyed by id: offset pagination can return a product twice if the catalog shifts mid-export,
		// which would otherwise hide a skipped product behind a matching count.
		Map<String, BillzProduct> byId = new LinkedHashMap<>();
		int fetched = 0;
		int page = 1;
		BillzProductPage response;
		do {
			response = billzClient.getProducts(page, properties.pageSize(), null);
			fetched += response.products().size();
			response.products().forEach(p -> byId.put(p.id(), p));
		}
		while (hasMorePages(response, fetched, page++));
		List<BillzProduct> all = new ArrayList<>(byId.values());
		run.setProductsFetched(all.size());

		verifyCompleteExport(response.count(), all.size());

		for (int from = 0; from < all.size(); from += properties.pageSize()) {
			importPage(run, all.subList(from, Math.min(from + properties.pageSize(), all.size())));
		}
		Integer deactivated = transactionTemplate.execute(
				status -> productRepository.markUnavailableNotSyncedSince(run.getStartedAt()));
		run.setProductsDeactivated(deactivated == null ? 0 : deactivated);
	}

	/** Guards against delisting the catalog because of a truncated or empty BILLZ response. */
	private void verifyCompleteExport(int reportedCount, int fetched) {
		if (fetched != reportedCount) {
			throw new IllegalStateException(
					"Incomplete BILLZ export: reported " + reportedCount + " products, fetched " + fetched);
		}
		long available = productRepository.countByAvailableInBillzTrue();
		if (available > 0 && fetched < available * (1 - properties.maxFullSyncDrop())) {
			throw new IllegalStateException("BILLZ export has " + fetched + " products but " + available
					+ " are currently available; refusing to delist that many. Check BILLZ and run a full sync manually");
		}
	}

	private void importPage(BillzSyncRun run, List<BillzProduct> products) {
		if (products.isEmpty()) {
			return;
		}
		BillzProductImporter.Result result = importer.importPage(products, properties.shopId(), clock.instant());
		run.setProductsCreated(run.getProductsCreated() + result.created());
		run.setProductsUpdated(run.getProductsUpdated() + result.updated());
	}

	private static boolean hasMorePages(BillzProductPage response, int fetched, int page) {
		return !response.products().isEmpty() && fetched < response.count() && page < MAX_PAGES;
	}
}
