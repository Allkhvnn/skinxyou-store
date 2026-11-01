package skinxyou_backend.catalog.brand;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

	Optional<Brand> findByBillzId(String billzId);

	boolean existsBySlug(String slug);
}
