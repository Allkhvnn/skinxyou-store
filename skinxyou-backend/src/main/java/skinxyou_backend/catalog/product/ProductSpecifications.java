package skinxyou_backend.catalog.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import skinxyou_backend.order.OrderItem;
import skinxyou_backend.order.OrderStatus;
import skinxyou_backend.catalog.brand.Brand;
import skinxyou_backend.catalog.category.Category;

final class ProductSpecifications {

	private static final String LIKE_ESCAPE = "!";

	private ProductSpecifications() {
	}

	static Specification<Product> catalog(ProductFilter filter, ProductSort sort) {
		return (root, query, cb) -> {
			List<Predicate> predicates = visible(root, cb);
			Expression<BigDecimal> effectivePrice = effectivePrice(root, cb);

			if (filter.brand() != null) {
				predicates.add(cb.equal(root.join("brand").get("slug"), filter.brand()));
			}
			if (filter.category() != null) {
				predicates.add(cb.equal(root.join("categories").get("slug"), filter.category()));
			}
			if (filter.minPrice() != null) {
				predicates.add(cb.greaterThanOrEqualTo(effectivePrice, filter.minPrice()));
			}
			if (filter.maxPrice() != null) {
				predicates.add(cb.lessThanOrEqualTo(effectivePrice, filter.maxPrice()));
			}
			if (filter.inStock()) {
				predicates.add(cb.greaterThan(root.get("stock"), BigDecimal.ZERO));
			}
			if (filter.query() != null) {
				predicates.add(search(root, cb, filter.query()));
			}
			if (!isCountQuery(query)) {
				List<Order> orders = new ArrayList<>();
				orders.add(cb.asc(outOfStockFirst(root, cb)));
				orders.addAll(order(sort, root, query, cb, effectivePrice));
				query.orderBy(orders);
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	/** Same brand or a shared category; in-stock products first. */
	static Specification<Product> relatedTo(Product product) {
		return (root, query, cb) -> {
			List<Predicate> predicates = visible(root, cb);
			predicates.add(cb.notEqual(root.get("id"), product.getId()));

			List<Predicate> related = new ArrayList<>();
			if (product.getBrand() != null) {
				related.add(cb.equal(root.get("brand"), product.getBrand()));
			}
			Set<Category> categories = product.getCategories();
			if (!categories.isEmpty()) {
				Subquery<Long> inCategories = query.subquery(Long.class);
				Root<Product> other = inCategories.from(Product.class);
				Join<Product, Category> otherCategory = other.join("categories");
				inCategories.select(other.get("id")).where(otherCategory.in(categories));
				related.add(root.get("id").in(inCategories));
			}
			predicates.add(related.isEmpty() ? cb.disjunction() : cb.or(related.toArray(Predicate[]::new)));

			if (!isCountQuery(query)) {
				query.orderBy(cb.asc(outOfStockFirst(root, cb)), cb.desc(root.get("billzUpdatedAt")),
						cb.desc(root.get("id")));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	/** In-stock products outside {@code excludedIds}, closest in price first: tops up a short related list. */
	static Specification<Product> alternativesTo(Product product, Set<Long> excludedIds) {
		return (root, query, cb) -> {
			List<Predicate> predicates = visible(root, cb);
			predicates.add(cb.greaterThan(root.get("stock"), BigDecimal.ZERO));
			predicates.add(cb.not(root.get("id").in(excludedIds)));
			if (!isCountQuery(query)) {
				BigDecimal price = product.getPromoPrice() != null ? product.getPromoPrice() : product.getPrice();
				HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
				query.orderBy(price != null
						? List.of(hcb.asc(cb.abs(cb.diff(effectivePrice(root, cb), price)), false), cb.desc(root.get("id")))
						: List.of(cb.desc(root.get("id"))));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static List<Predicate> visible(Root<Product> root, CriteriaBuilder cb) {
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(cb.isFalse(root.get("hidden")));
		predicates.add(cb.isTrue(root.get("availableInBillz")));
		return predicates;
	}

	/** Case-insensitive match on name, SKU and brand; exact match on barcode. */
	private static Predicate search(Root<Product> root, CriteriaBuilder cb, String text) {
		HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
		// User input is matched literally: LIKE wildcards are escaped with an explicit escape character.
		Expression<String> pattern = cb.literal("%" + text.replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
				.replace("%", LIKE_ESCAPE + "%").replace("_", LIKE_ESCAPE + "_") + "%");
		Expression<Character> escape = cb.literal(LIKE_ESCAPE.charAt(0));
		Join<Product, Brand> brand = root.join("brand", JoinType.LEFT);
		return cb.or(
				hcb.ilike(root.get("name"), pattern, escape),
				hcb.ilike(root.get("sku"), pattern, escape),
				hcb.ilike(brand.get("name"), pattern, escape),
				cb.equal(root.get("barcode"), text));
	}

	private static Expression<BigDecimal> effectivePrice(Root<Product> root, CriteriaBuilder cb) {
		return cb.coalesce(root.get("promoPrice"), root.get("price"));
	}

	private static Expression<Integer> outOfStockFirst(Root<Product> root, CriteriaBuilder cb) {
		return cb.<Integer>selectCase().when(cb.greaterThan(root.get("stock"), BigDecimal.ZERO), 0).otherwise(1);
	}

	private static List<Order> order(ProductSort sort, Root<Product> root, CriteriaQuery<?> query,
			CriteriaBuilder cb, Expression<BigDecimal> effectivePrice) {
		HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
		return switch (sort) {
			case NEWEST -> List.of(hcb.desc(root.get("billzUpdatedAt"), false), cb.desc(root.get("id")));
			case POPULAR -> List.of(cb.desc(unitsSold(root, query, cb)),
					hcb.desc(root.get("billzUpdatedAt"), false), cb.desc(root.get("id")));
			case PRICE_ASC -> List.of(cb.asc(effectivePrice), cb.asc(root.get("id")));
			case PRICE_DESC -> List.of(cb.desc(effectivePrice), cb.desc(root.get("id")));
			case NAME -> List.of(cb.asc(root.get("name")), cb.asc(root.get("id")));
		};
	}

	/** Units of this product in orders that were not cancelled. */
	private static Expression<Integer> unitsSold(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		Subquery<Integer> sold = query.subquery(Integer.class);
		Root<OrderItem> item = sold.from(OrderItem.class);
		sold.select(cb.coalesce(cb.sum(item.<Integer>get("quantity")), 0))
				.where(cb.equal(item.get("product"), root),
						cb.notEqual(item.get("order").get("status"), OrderStatus.CANCELLED));
		return sold;
	}

	private static boolean isCountQuery(CriteriaQuery<?> query) {
		return query.getResultType() == Long.class || query.getResultType() == long.class;
	}
}
