package skinxyou_backend.catalog.product;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	List<Product> findAllByBillzIdIn(Collection<String> billzIds);

	boolean existsBySlug(String slug);

	long countByAvailableInBillzTrue();

	Optional<Product> findBySlugAndHiddenFalseAndAvailableInBillzTrue(String slug);

	/** Marks products not seen by a completed full export (not synced since it started) as unavailable. */
	@Modifying
	@Query("""
			update Product p set p.availableInBillz = false, p.updatedAt = CURRENT_TIMESTAMP
			where p.availableInBillz = true and p.syncedAt < :fullSyncStartedAt
			""")
	int markUnavailableNotSyncedSince(Instant fullSyncStartedAt);
}
