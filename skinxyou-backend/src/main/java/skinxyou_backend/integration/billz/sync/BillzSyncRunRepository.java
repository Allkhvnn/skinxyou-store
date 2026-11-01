package skinxyou_backend.integration.billz.sync;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillzSyncRunRepository extends JpaRepository<BillzSyncRun, Long> {

	Optional<BillzSyncRun> findFirstByStatusOrderByStartedAtDesc(SyncStatus status);
}
