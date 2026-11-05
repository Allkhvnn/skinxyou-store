package skinxyou_backend.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import skinxyou_backend.catalog.product.ImageSource;
import skinxyou_backend.catalog.product.Product;
import skinxyou_backend.catalog.product.ProductImage;
import skinxyou_backend.catalog.product.ProductRepository;
import skinxyou_backend.common.web.PageResponse;
import skinxyou_backend.integration.r2.ImageStorage;

/** Site-owned product content (description, SEO, visibility). BILLZ-owned fields are read-only here. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

	static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
	private static final int MAX_SITE_IMAGES = 10;

	private final ProductRepository productRepository;
	private final ImageStorage imageStorage;

	public record Image(Long id, String url, ImageSource source) {
	}

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
			boolean hidden, List<Image> images) {
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

	/** Uploads a site photo to R2; it is appended after existing site photos. */
	@Transactional
	public Details addImage(Long id, byte[] bytes, String actor) {
		if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Фото должно быть не больше 5 МБ");
		}
		ImageFormat format = ImageFormat.detect(bytes)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Поддерживаются JPEG, PNG и WebP"));
		Product product = find(id);
		List<ProductImage> siteImages = siteImages(product);
		if (siteImages.size() >= MAX_SITE_IMAGES) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Не больше " + MAX_SITE_IMAGES + " своих фото у товара");
		}
		String key = "products/" + product.getSlug() + "/" + UUID.randomUUID() + "." + format.extension;
		String url = imageStorage.upload(key, bytes, format.contentType);
		int sequence = siteImages.stream().mapToInt(ProductImage::getSequence).max().orElse(-1) + 1;
		product.getImages().add(new ProductImage(product, url, sequence, false, ImageSource.SITE));
		try {
			Product saved = productRepository.saveAndFlush(product);
			log.info("Photo added to product {} by {}", product.getSlug(), actor);
			return details(saved);
		}
		catch (RuntimeException e) {
			deleteQuietly(key);
			throw e;
		}
	}

	/** Only site photos can be removed here; BILLZ photos are managed in BILLZ. */
	@Transactional
	public Details deleteImage(Long id, Long imageId, String actor) {
		Product product = find(id);
		ProductImage image = siteImage(product, imageId);
		product.getImages().remove(image);
		Product saved = productRepository.saveAndFlush(product);
		String key = imageStorage.keyOf(image.getUrl());
		if (key != null) {
			deleteQuietly(key);
		}
		log.info("Photo removed from product {} by {}", product.getSlug(), actor);
		return details(saved);
	}

	/** Moves a site photo to the front; the first site photo is the product's main photo. */
	@Transactional
	public Details makeMain(Long id, Long imageId, String actor) {
		Product product = find(id);
		ProductImage target = siteImage(product, imageId);
		List<ProductImage> ordered = new ArrayList<>(siteImages(product));
		ordered.remove(target);
		ordered.addFirst(target);
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).setSequence(i);
		}
		log.info("Main photo of product {} changed by {}", product.getSlug(), actor);
		return details(productRepository.saveAndFlush(product));
	}

	private static List<ProductImage> siteImages(Product product) {
		return product.orderedImages().stream().filter(i -> i.getSource() == ImageSource.SITE).toList();
	}

	private static ProductImage siteImage(Product product, Long imageId) {
		ProductImage image = product.getImages().stream()
				.filter(i -> i.getId().equals(imageId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фото не найдено"));
		if (image.getSource() != ImageSource.SITE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Фото из BILLZ меняются в BILLZ");
		}
		return image;
	}

	private void deleteQuietly(String key) {
		try {
			imageStorage.delete(key);
		}
		catch (RuntimeException e) {
			log.warn("Could not delete {} from storage: {}", key, e.getMessage());
		}
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
				p.getDescription(), p.getSeoTitle(), p.getSeoDescription(), p.isHidden(),
				p.orderedImages().stream().map(i -> new Image(i.getId(), i.getUrl(), i.getSource())).toList());
	}

	private static BigDecimal effectivePrice(Product p) {
		return p.getPromoPrice() != null ? p.getPromoPrice() : p.getPrice();
	}

	private static String mainImage(Product p) {
		return p.orderedImages().stream().findFirst().map(ProductImage::getUrl).orElse(null);
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
