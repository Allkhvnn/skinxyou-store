package skinxyou_backend.banner;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
public class Banner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Shown to screen readers as the image's alt text. */
	private String title;

	/** A site path ("/catalog?...") or an https URL; null makes the slide not clickable. */
	private String linkUrl;

	private String desktopImageUrl;

	/** Optional square version for phones; the desktop image is used when missing. */
	private String mobileImageUrl;

	private int position;

	private boolean active;

	private LocalDate startsOn;

	private LocalDate endsOn;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	boolean isShownOn(LocalDate day) {
		return active && desktopImageUrl != null
				&& (startsOn == null || !day.isBefore(startsOn))
				&& (endsOn == null || !day.isAfter(endsOn));
	}
}
