package skinxyou_backend.catalog.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByBillzId(String billzId);

	boolean existsBySlug(String slug);

	/** Categories with visible products; parent resolved through the BILLZ parent id. */
	@Query("""
			select new skinxyou_backend.catalog.category.CategoryResponse(c.slug, c.name, parent.slug, count(p))
			from Product p join p.categories c
			left join Category parent on parent.billzId = c.parentBillzId
			where p.hidden = false and p.availableInBillz = true
			group by c.id, c.slug, c.name, parent.slug
			order by lower(c.name)
			""")
	List<CategoryResponse> findVisibleWithProductCounts();
}
