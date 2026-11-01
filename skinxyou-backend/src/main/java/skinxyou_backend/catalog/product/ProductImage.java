package skinxyou_backend.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private String url;

	private int sequence;

	@Column(name = "is_main")
	private boolean main;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ImageSource source;

	public ProductImage(Product product, String url, int sequence, boolean main, ImageSource source) {
		this.product = product;
		this.url = url;
		this.sequence = sequence;
		this.main = main;
		this.source = source;
	}
}
