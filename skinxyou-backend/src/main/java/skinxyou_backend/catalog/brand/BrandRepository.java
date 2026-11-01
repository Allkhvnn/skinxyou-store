package skinxyou_backend.catalog.brand;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrandRepository extends JpaRepository<Brand, Long> {

	Optional<Brand> findByBillzId(String billzId);

	boolean existsBySlug(String slug);

	@Query("""
			select new skinxyou_backend.catalog.brand.BrandResponse(b.slug, b.name, count(p))
			from Product p join p.brand b
			where p.hidden = false and p.availableInBillz = true
			group by b.id, b.slug, b.name
			order by lower(b.name)
			""")
	List<BrandResponse> findVisibleWithProductCounts();
}
