package skinxyou_backend.banner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skinxyou_backend.admin.ImageFormat;
import skinxyou_backend.integration.r2.ImageStorage;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

	static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Almaty");

	private final BannerRepository repository;
	private final ImageStorage imageStorage;
	private final Clock clock;

	public enum Variant {
		DESKTOP, MOBILE
	}

	public enum Direction {
		UP, DOWN
	}

	public record PublicBanner(Long id, String title, String linkUrl, String desktopImageUrl, String mobileImageUrl) {
	}

	public record AdminBanner(Long id, String title, String linkUrl, String desktopImageUrl, String mobileImageUrl,
			int position, boolean active, LocalDate startsOn, LocalDate endsOn, boolean shownNow) {
	}

	public record Fields(String title, String linkUrl, boolean active, LocalDate startsOn, LocalDate endsOn) {
	}

	@Transactional(readOnly = true)
	public List<PublicBanner> shownNow() {
		LocalDate today = today();
		return repository.findAllByOrderByPositionAscIdAsc().stream()
				.filter(b -> b.isShownOn(today))
				.map(b -> new PublicBanner(b.getId(), b.getTitle(), b.getLinkUrl(), b.getDesktopImageUrl(),
						b.getMobileImageUrl()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminBanner> list() {
		LocalDate today = today();
		return repository.findAllByOrderByPositionAscIdAsc().stream().map(b -> view(b, today)).toList();
	}

	@Transactional
	public AdminBanner create(Fields fields, String actor) {
		Banner banner = new Banner();
		apply(banner, fields);
		banner.setPosition(repository.maxPosition() + 1);
		Banner saved = repository.save(banner);
		log.info("Banner {} created by {}", saved.getId(), actor);
		return view(saved, today());
	}

	@Transactional
	public AdminBanner update(Long id, Fields fields, String actor) {
		Banner banner = find(id);
		apply(banner, fields);
		log.info("Banner {} updated by {}", id, actor);
		return view(repository.saveAndFlush(banner), today());
	}

	@Transactional
	public AdminBanner uploadImage(Long id, Variant variant, byte[] bytes, String actor) {
		if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинка должна быть не больше 5 МБ");
		}
		ImageFormat format = ImageFormat.detect(bytes)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Поддерживаются JPEG, PNG и WebP"));
		Banner banner = find(id);
		String key = "banners/" + id + "/" + variant.name().toLowerCase() + "-" + UUID.randomUUID() + "." + format.extension;
		String url = imageStorage.upload(key, bytes, format.contentType);
		String previous = variant == Variant.DESKTOP ? banner.getDesktopImageUrl() : banner.getMobileImageUrl();
		if (variant == Variant.DESKTOP) {
			banner.setDesktopImageUrl(url);
		}
		else {
			banner.setMobileImageUrl(url);
		}
		Banner saved;
		try {
			saved = repository.saveAndFlush(banner);
		}
		catch (RuntimeException e) {
			deleteQuietly(url);
			throw e;
		}
		deleteQuietly(previous);
		log.info("Banner {} {} image replaced by {}", id, variant, actor);
		return view(saved, today());
	}

	@Transactional
	public AdminBanner removeMobileImage(Long id, String actor) {
		Banner banner = find(id);
		String previous = banner.getMobileImageUrl();
		banner.setMobileImageUrl(null);
		Banner saved = repository.saveAndFlush(banner);
		deleteQuietly(previous);
		log.info("Banner {} mobile image removed by {}", id, actor);
		return view(saved, today());
	}

	@Transactional
	public void move(Long id, Direction direction, String actor) {
		List<Banner> all = repository.findAllByOrderByPositionAscIdAsc();
		int index = all.indexOf(find(id));
		int other = direction == Direction.UP ? index - 1 : index + 1;
		if (other < 0 || other >= all.size()) {
			return;
		}
		Banner neighbour = all.get(other);
		all.set(other, all.get(index));
		all.set(index, neighbour);
		for (int i = 0; i < all.size(); i++) {
			all.get(i).setPosition(i);
		}
		repository.saveAll(all);
		log.info("Banner {} moved {} by {}", id, direction, actor);
	}

	@Transactional
	public void delete(Long id, String actor) {
		Banner banner = find(id);
		repository.delete(banner);
		repository.flush();
		deleteQuietly(banner.getDesktopImageUrl());
		deleteQuietly(banner.getMobileImageUrl());
		log.info("Banner {} deleted by {}", id, actor);
	}

	private void apply(Banner banner, Fields fields) {
		String title = fields.title() == null ? "" : fields.title().strip();
		if (title.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название баннера");
		}
		String link = fields.linkUrl() == null || fields.linkUrl().isBlank() ? null : fields.linkUrl().strip();
		if (link != null && !isAllowedLink(link)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Ссылка должна начинаться с «/» (страница сайта) или с «https://»");
		}
		if (fields.startsOn() != null && fields.endsOn() != null && fields.endsOn().isBefore(fields.startsOn())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата окончания раньше даты начала");
		}
		banner.setTitle(title);
		banner.setLinkUrl(link);
		banner.setActive(fields.active());
		banner.setStartsOn(fields.startsOn());
		banner.setEndsOn(fields.endsOn());
	}

	/** Site paths or https URLs only: no "//host", "javascript:" or other schemes. */
	static boolean isAllowedLink(String link) {
		return (link.startsWith("/") && !link.startsWith("//") && !link.contains("\\"))
				|| link.startsWith("https://");
	}

	private Banner find(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Баннер не найден"));
	}

	private void deleteQuietly(String url) {
		if (url == null) {
			return;
		}
		String key = imageStorage.keyOf(url);
		if (key == null) {
			return;
		}
		try {
			imageStorage.delete(key);
		}
		catch (RuntimeException e) {
			log.warn("Could not delete banner image {}: {}", key, e.getMessage());
		}
	}

	private LocalDate today() {
		return LocalDate.ofInstant(clock.instant(), STORE_ZONE);
	}

	private static AdminBanner view(Banner b, LocalDate today) {
		return new AdminBanner(b.getId(), b.getTitle(), b.getLinkUrl(), b.getDesktopImageUrl(), b.getMobileImageUrl(),
				b.getPosition(), b.isActive(), b.getStartsOn(), b.getEndsOn(), b.isShownOn(today));
	}
}
