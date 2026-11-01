package skinxyou_backend.integration.billz.sync;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillzSyncRunRepository extends JpaRepository<BillzSyncRun, Long> {

	Optional<BillzSyncRun> findFirstByStatusOrderByStartedAtDesc(SyncStatus status);

	/** The run that finished before the given one (skipping runs still in progress). */
	Optional<BillzSyncRun> findFirstByStatusNotAndIdLessThanOrderByIdDesc(SyncStatus status, Long id);

	Page<BillzSyncRun> findAllByOrderByIdDesc(Pageable pageable);
}
