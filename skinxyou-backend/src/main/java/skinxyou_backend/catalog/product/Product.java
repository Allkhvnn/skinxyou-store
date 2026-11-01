package skinxyou_backend.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skinxyou_backend.catalog.brand.Brand;
import skinxyou_backend.catalog.category.Category;

/**
 * Catalog product. Fields up to {@code syncedAt} are owned by BILLZ and overwritten on sync;
 * the rest is SkinxYou content and is never touched by sync.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String billzId;

	// --- BILLZ-owned ---

	@Column(nullable = false)
	private String name;

	private String sku;

	private String barcode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand_id")
	private Brand brand;

	@ManyToMany
	@JoinTable(name = "product_category",
			joinColumns = @JoinColumn(name = "product_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<Category> categories = new HashSet<>();

	private BigDecimal price;

	private BigDecimal promoPrice;

	private String currency;

	@Column(nullable = false)
	private BigDecimal stock = BigDecimal.ZERO;

	private LocalDateTime billzUpdatedAt;

	/** False once the product disappears from a full BILLZ export. */
	private boolean availableInBillz = true;

	@Column(nullable = false)
	private Instant syncedAt;

	// --- SkinxYou-owned ---

	@Column(nullable = false, unique = true)
	private String slug;

	private String description;

	private String seoTitle;

	private String seoDescription;

	private boolean hidden;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<ProductImage> images = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;
}
