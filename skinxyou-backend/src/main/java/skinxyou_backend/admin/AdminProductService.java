package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skinxyou_backend.catalog.category.Category;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.common.web.PageResponse;

/** Site-owned product content (description, SEO, visibility). BILLZ-owned fields are read-only here. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

	private final ProductRepository productRepository;

	public enum Filter {
		NO_DESCRIPTION, NO_PHOTO, HIDDEN, REMOVED_IN_BILLZ
	}

	public record Summary(Long id, String slug, String name, String sku, String brand, BigDecimal price,
			BigDecimal stock, boolean hidden, boolean availableInBillz, boolean hasDescription, int imageCount,
			String imageUrl) {
	}

	public record Details(Long id, String slug, String name, String sku, String barcode, String brand,
			List<String> categories, BigDecimal price, BigDecimal promoPrice, BigDecimal stock,
			boolean availableInBillz, String imageUrl, String description, String seoTitle, String seoDescription,
			boolean hidden) {
	}

	public record ContentUpdate(String description, String seoTitle, String seoDescription, boolean hidden) {
	}

	@Transactional(readOnly = true)
	public PageResponse<Summary> list(String query, Filter filter, int page, int size) {
		return PageResponse.of(productRepository.findAll(specification(query, filter),
				PageRequest.of(page, size, Sort.by("name", "id"))), AdminProductService::summary);
	}

	@Transactional(readOnly = true)
	public Details get(Long id) {
		return details(find(id));
	}

	@Transactional
	public Details updateContent(Long id, ContentUpdate update, String actor) {
		Product product = find(id);
		product.setDescription(trimToNull(update.description()));
		product.setSeoTitle(trimToNull(update.seoTitle()));
		product.setSeoDescription(trimToNull(update.seoDescription()));
		product.setHidden(update.hidden());
		log.info("Product {} content updated by {} (hidden={})", product.getSlug(), actor, update.hidden());
		return details(productRepository.saveAndFlush(product));
	}

	private Product find(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден"));
	}

	private static Summary summary(Product p) {
		return new Summary(p.getId(), p.getSlug(), p.getName(), p.getSku(),
				p.getBrand() == null ? null : p.getBrand().getName(), effectivePrice(p), p.getStock(), p.isHidden(),
				p.isAvailableInBillz(), p.getDescription() != null, p.getImages().size(), mainImage(p));
	}

	private static Details details(Product p) {
		return new Details(p.getId(), p.getSlug(), p.getName(), p.getSku(), p.getBarcode(),
				p.getBrand() == null ? null : p.getBrand().getName(),
				p.getCategories().stream().map(Category::getName).sorted().toList(),
				p.getPrice(), p.getPromoPrice(), p.getStock(), p.isAvailableInBillz(), mainImage(p),
				p.getDescription(), p.getSeoTitle(), p.getSeoDescription(), p.isHidden());
	}

	private static BigDecimal effectivePrice(Product p) {
		return p.getPromoPrice() != null ? p.getPromoPrice() : p.getPrice();
	}

	private static String mainImage(Product p) {
		return p.getImages().stream()
				.min(Comparator.comparing((ProductImage i) -> !i.isMain()).thenComparingInt(ProductImage::getSequence))
				.map(ProductImage::getUrl)
				.orElse(null);
	}

	private static Specification<Product> specification(String query, Filter filter) {
		return (root, q, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter != null) {
				predicates.add(switch (filter) {
					case NO_DESCRIPTION -> cb.isNull(root.get("description"));
					case NO_PHOTO -> cb.isEmpty(root.get("images"));
					case HIDDEN -> cb.isTrue(root.get("hidden"));
					case REMOVED_IN_BILLZ -> cb.isFalse(root.get("availableInBillz"));
				});
			}
			if (query != null && !query.isBlank()) {
				HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
				String text = "%" + query.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
				var pattern = cb.literal(text);
				var escape = cb.literal('!');
				var brand = root.join("brand", JoinType.LEFT);
				predicates.add(cb.or(
						hcb.ilike(root.get("name"), pattern, escape),
						hcb.ilike(root.get("sku"), pattern, escape),
						hcb.ilike(brand.get("name"), pattern, escape),
						cb.equal(root.get("barcode"), query.trim())));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
