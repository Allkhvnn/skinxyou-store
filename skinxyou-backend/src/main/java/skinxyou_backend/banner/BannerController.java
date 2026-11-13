package skinxyou_backend.banner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Public slides under /api/catalog (open), management under /api/admin (staff only). */
@RestController
@RequiredArgsConstructor
public class BannerController {

	private final BannerService service;

	public record BannerRequest(
			@Size(max = 120) String title,
			@Size(max = 500) String linkUrl,
			boolean active,
			LocalDate startsOn,
			LocalDate endsOn) {

		BannerService.Fields fields() {
			return new BannerService.Fields(title, linkUrl, active, startsOn, endsOn);
		}
	}

	@GetMapping("/api/catalog/banners")
	public List<BannerService.PublicBanner> shownNow() {
		return service.shownNow();
	}

	@GetMapping("/api/admin/banners")
	public List<BannerService.AdminBanner> list() {
		return service.list();
	}

	@PostMapping("/api/admin/banners")
	@ResponseStatus(HttpStatus.CREATED)
	public BannerService.AdminBanner create(@Valid @RequestBody BannerRequest request, @AuthenticationPrincipal Jwt jwt) {
		return service.create(request.fields(), jwt.getSubject());
	}

	@PutMapping("/api/admin/banners/{id}")
	public BannerService.AdminBanner update(@PathVariable Long id, @Valid @RequestBody BannerRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.update(id, request.fields(), jwt.getSubject());
	}

	@PostMapping(path = "/api/admin/banners/{id}/images/{variant}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public BannerService.AdminBanner uploadImage(@PathVariable Long id, @PathVariable BannerService.Variant variant,
			@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
		return service.uploadImage(id, variant, file.getBytes(), jwt.getSubject());
	}

	@DeleteMapping("/api/admin/banners/{id}/images/MOBILE")
	public BannerService.AdminBanner removeMobileImage(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		return service.removeMobileImage(id, jwt.getSubject());
	}

	@PostMapping("/api/admin/banners/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void move(@PathVariable Long id, @RequestParam BannerService.Direction direction,
			@AuthenticationPrincipal Jwt jwt) {
		service.move(id, direction, jwt.getSubject());
	}

	@DeleteMapping("/api/admin/banners/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		service.delete(id, jwt.getSubject());
	}
}
