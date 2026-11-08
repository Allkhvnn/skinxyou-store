package skinxyou_backend.catalog.product;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
	private static final int RELATED_MIN = 4;
	private static final int HIT_COUNT = 8;
	private static final Duration HIT_PERIOD = Duration.ofDays(90);
	private static final Duration NEW_PERIOD = Duration.ofDays(30);
	/** Everything created by the very first BILLZ import is existing stock, not new arrivals. */
	private static final Duration FIRST_IMPORT_WINDOW = Duration.ofDays(1);

	private final ProductRepository productRepository;
	private final Clock clock;

	public PageResponse<ProductSummary> search(ProductFilter filter, ProductSort sort, int page, int size) {
		// Ordering comes from the specification (it sorts by a computed effective price).
		ProductResponses.Badges badges = badges();
		return PageResponse.of(
				productRepository.findAll(ProductSpecifications.catalog(filter, sort, badges.newSince()),
						PageRequest.of(page, size)),
				product -> ProductResponses.summary(product, badges));
	}

	public Optional<ProductDetails> findBySlug(String slug) {
		return productRepository.findBySlugAndHiddenFalseAndAvailableInBillzTrue(slug)
				.map(product -> ProductResponses.details(product, related(product), badges()));
	}

	private ProductResponses.Badges badges() {
		Instant now = clock.instant();
		Instant newSince = now.minus(NEW_PERIOD);
		Instant firstCreated = productRepository.firstCreatedAt();
		if (firstCreated != null && firstCreated.plus(FIRST_IMPORT_WINDOW).isAfter(newSince)) {
			newSince = firstCreated.plus(FIRST_IMPORT_WINDOW);
		}
		return new ProductResponses.Badges(newSince,
				Set.copyOf(productRepository.findBestSellerIds(now.minus(HIT_PERIOD), PageRequest.of(0, HIT_COUNT))));
	}

	/** Same brand or category first; many products have neither, so short lists are topped up. */
	private List<Product> related(Product product) {
		List<Product> related = new ArrayList<>(productRepository
				.findAll(ProductSpecifications.relatedTo(product), PageRequest.of(0, RELATED_LIMIT)).getContent());
		if (related.size() < RELATED_MIN) {
			Set<Long> excluded = new HashSet<>(Set.of(product.getId()));
			related.forEach(p -> excluded.add(p.getId()));
			related.addAll(productRepository.findAll(ProductSpecifications.alternativesTo(product, excluded),
					PageRequest.of(0, RELATED_MIN - related.size())).getContent());
		}
		return related;
	}
}
