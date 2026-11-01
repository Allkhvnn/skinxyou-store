package skinxyou_backend.catalog.category;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryController {

	private final CategoryRepository categoryRepository;

	/** Categories that have at least one product visible on the storefront. */
	@GetMapping
	public List<CategoryResponse> list() {
		return categoryRepository.findVisibleWithProductCounts();
	}
}
