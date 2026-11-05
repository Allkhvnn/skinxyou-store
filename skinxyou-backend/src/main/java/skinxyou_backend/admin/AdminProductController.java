package skinxyou_backend.admin;

import java.io.IOException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import skinxyou_backend.common.web.PageResponse;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final AdminProductService service;

	public record ContentRequest(
			@Size(max = 10_000) String description,
			@Size(max = 255) String seoTitle,
			@Size(max = 500) String seoDescription,
			boolean hidden) {
	}

	@GetMapping
	public PageResponse<AdminProductService.Summary> list(
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) AdminProductService.Filter filter,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
		return service.list(q, filter, page, size);
	}

	@GetMapping("/{id}")
	public AdminProductService.Details get(@PathVariable Long id) {
		return service.get(id);
	}

	@PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AdminProductService.Details addImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal Jwt jwt) throws IOException {
		return service.addImage(id, file.getBytes(), jwt.getSubject());
	}

	@DeleteMapping("/{id}/images/{imageId}")
	public AdminProductService.Details deleteImage(@PathVariable Long id, @PathVariable Long imageId,
			@AuthenticationPrincipal Jwt jwt) {
		return service.deleteImage(id, imageId, jwt.getSubject());
	}

	@PostMapping("/{id}/images/{imageId}/main")
	public AdminProductService.Details makeMain(@PathVariable Long id, @PathVariable Long imageId,
			@AuthenticationPrincipal Jwt jwt) {
		return service.makeMain(id, imageId, jwt.getSubject());
	}

	@PutMapping("/{id}/content")
	public AdminProductService.Details updateContent(@PathVariable Long id, @Valid @RequestBody ContentRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.updateContent(id, new AdminProductService.ContentUpdate(request.description(),
				request.seoTitle(), request.seoDescription(), request.hidden()), jwt.getSubject());
	}
}
