package skinxyou_backend.catalog.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import skinxyou_backend.catalog.product.ProductResponses.ProductDetails;
import skinxyou_backend.catalog.product.ProductResponses.ProductSummary;
import skinxyou_backend.common.web.PageResponse;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductCatalogService catalogService;

	@GetMapping
	public PageResponse<ProductSummary> list(
			@RequestParam(required = false) @Size(max = 200) String brand,
			@RequestParam(required = false) @Size(max = 200) String category,
			@RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
			@RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
			@RequestParam(defaultValue = "false") boolean inStock,
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(defaultValue = "false") boolean onlyNew,
			@RequestParam(defaultValue = "newest") String sort,
			@RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
			@RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
		ProductSort productSort = ProductSort.fromParam(sort)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sort: " + sort));
		ProductFilter filter = new ProductFilter(blankToNull(brand), blankToNull(category), minPrice, maxPrice,
				inStock, blankToNull(q), onlyNew);
		return catalogService.search(filter, productSort, page, size);
	}

	@GetMapping("/{slug}")
	public ProductDetails get(@PathVariable @Size(max = 255) String slug) {
		return catalogService.findBySlug(slug)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
