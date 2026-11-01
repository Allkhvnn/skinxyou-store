package skinxyou_backend.catalog.product;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinxyou_backend.catalog.product.ProductResponses.ProductDetails;
import skinxyou_backend.catalog.product.ProductResponses.ProductSummary;
import skinxyou_backend.common.web.PageResponse;

/** Storefront read model: only products that are not hidden and still exist in BILLZ. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCatalogService {

	private static final int RELATED_LIMIT = 8;

	private final ProductRepository productRepository;

	public PageResponse<ProductSummary> search(ProductFilter filter, ProductSort sort, int page, int size) {
		// Ordering comes from the specification (it sorts by a computed effective price).
		return PageResponse.of(
				productRepository.findAll(ProductSpecifications.catalog(filter, sort), PageRequest.of(page, size)),
				ProductResponses::summary);
	}

	public Optional<ProductDetails> findBySlug(String slug) {
		return productRepository.findBySlugAndHiddenFalseAndAvailableInBillzTrue(slug)
				.map(product -> ProductResponses.details(product, productRepository
						.findAll(ProductSpecifications.relatedTo(product), PageRequest.of(0, RELATED_LIMIT))
						.getContent()));
	}
}
