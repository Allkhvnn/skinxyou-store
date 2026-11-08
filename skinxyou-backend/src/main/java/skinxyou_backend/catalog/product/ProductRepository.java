package skinxyou_backend.catalog.product;

import java.math.BigDecimal;
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

	@Query("select min(p.createdAt) from Product p")
	Instant firstCreatedAt();

	/** Best sellers since {@code since}, by units in orders that were not cancelled. */
	@Query("""
			select i.product.id from OrderItem i
			where i.order.status <> skinxyou_backend.order.OrderStatus.CANCELLED and i.order.createdAt >= :since
				and i.product.hidden = false and i.product.availableInBillz = true
			group by i.product.id
			order by sum(i.quantity) desc, i.product.id
			""")
	List<Long> findBestSellerIds(@org.springframework.data.repository.query.Param("since") Instant since,
			org.springframework.data.domain.Pageable pageable);

	long countByAvailableInBillzTrue();

	/** Shown on the storefront. */
	long countByAvailableInBillzTrueAndHiddenFalse();

	long countByAvailableInBillzTrueAndHiddenFalseAndStockGreaterThan(BigDecimal stock);

	long countByAvailableInBillzFalse();

	long countByHiddenTrue();

	Optional<Product> findBySlugAndHiddenFalseAndAvailableInBillzTrue(String slug);

	List<Product> findAllBySlugInAndHiddenFalseAndAvailableInBillzTrue(Collection<String> slugs);

	/** Marks products not seen by a completed full export (not synced since it started) as unavailable. */
	@Modifying
	@Query("""
			update Product p set p.availableInBillz = false, p.updatedAt = CURRENT_TIMESTAMP
			where p.availableInBillz = true and p.syncedAt < :fullSyncStartedAt
			""")
	int markUnavailableNotSyncedSince(Instant fullSyncStartedAt);
}
