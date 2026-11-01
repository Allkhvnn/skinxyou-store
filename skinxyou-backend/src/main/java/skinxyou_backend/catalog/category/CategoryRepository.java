package skinxyou_backend.catalog.category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByBillzId(String billzId);

	boolean existsBySlug(String slug);
}
