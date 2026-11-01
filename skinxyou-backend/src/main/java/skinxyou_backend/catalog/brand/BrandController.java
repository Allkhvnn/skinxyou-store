package skinxyou_backend.catalog.brand;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog/brands")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandController {

	private final BrandRepository brandRepository;

	/** Brands that have at least one product visible on the storefront. */
	@GetMapping
	public List<BrandResponse> list() {
		return brandRepository.findVisibleWithProductCounts();
	}

	@GetMapping("/{slug}")
	public BrandResponse get(@PathVariable String slug) {
		return brandRepository.findVisibleWithProductCounts().stream()
				.filter(b -> b.slug().equals(slug))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
	}
}
