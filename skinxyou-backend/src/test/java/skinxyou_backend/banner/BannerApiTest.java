package skinxyou_backend.banner;

import java.time.Instant;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import skinxyou_backend.TestcontainersConfiguration;
import skinxyou_backend.admin.AdminRole;
import skinxyou_backend.admin.AdminUser;
import skinxyou_backend.admin.AdminUserRepository;
import skinxyou_backend.integration.r2.ImageStorage;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BannerApiTest {

	private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};

	@Autowired
	MockMvc mockMvc;

	@Autowired
	BannerRepository bannerRepository;

	@Autowired
	AdminUserRepository adminUserRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoBean
	ImageStorage imageStorage;

	@BeforeEach
	void seed() {
		bannerRepository.deleteAll();
		if (adminUserRepository.findByUsername("marketer").isEmpty()) {
			AdminUser user = new AdminUser();
			user.setUsername("marketer");
			user.setDisplayName("Marketer");
			user.setPasswordHash(passwordEncoder.encode("marketer-password"));
			user.setRole(AdminRole.MANAGER);
			user.setCreatedAt(Instant.now());
			adminUserRepository.save(user);
		}
		doAnswer(inv -> "https://r2.test/" + inv.getArgument(0)).when(imageStorage).upload(anyString(), any(), anyString());
		doAnswer(inv -> inv.<String>getArgument(0).replace("https://r2.test/", "")).when(imageStorage).keyOf(anyString());
	}

	@Test
	void showsOnlyActiveCurrentBannersWithAnImageInOrder() throws Exception {
		String token = bearer();
		long promo = create(token, "Promo", "/catalog?sort=popular", true, null, null);
		long noImage = create(token, "No image", null, true, null, null);
		long inactive = create(token, "Off", null, false, null, null);
		long expired = create(token, "Old", null, true, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
		long second = create(token, "Second", "https://www.instagram.com/skinxyou/", true, null, null);
		for (long id : new long[] {promo, inactive, expired, second}) {
			upload(token, id, "DESKTOP");
		}
		upload(token, promo, "MOBILE");

		mockMvc.perform(post("/api/admin/banners/" + second + "/move").param("direction", "UP")
						.header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/catalog/banners"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title").value(contains("Promo", "Second")))
				.andExpect(jsonPath("$[0].linkUrl").value("/catalog?sort=popular"))
				.andExpect(jsonPath("$[0].mobileImageUrl").value(org.hamcrest.Matchers.startsWith("https://r2.test/banners/")))
				.andExpect(jsonPath("$[1].mobileImageUrl").doesNotExist());

		mockMvc.perform(get("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(jsonPath("$[*].title").value(contains("Promo", "No image", "Off", "Second", "Old")))
				.andExpect(jsonPath("$[1].shownNow").value(false));
		assert noImage > 0;
	}

	@Test
	void replacingAndDeletingRemovesOldImagesFromStorage() throws Exception {
		String token = bearer();
		long id = create(token, "Promo", null, true, null, null);
		String first = upload(token, id, "DESKTOP");
		upload(token, id, "DESKTOP");
		verify(imageStorage).delete(first.replace("https://r2.test/", ""));

		mockMvc.perform(delete("/api/admin/banners/" + id).header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/catalog/banners")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void rejectsUnsafeLinksBadDatesNonImagesAndAnonymousStaffCalls() throws Exception {
		String token = bearer();
		for (String link : new String[] {"javascript:alert(1)", "//evil.example", "http://plain.example"}) {
			mockMvc.perform(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, token)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"title\":\"X\",\"linkUrl\":\"%s\",\"active\":true}".formatted(link)))
					.andExpect(status().isBadRequest());
		}
		mockMvc.perform(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"X\",\"active\":true,\"startsOn\":\"2026-05-10\",\"endsOn\":\"2026-05-01\"}"))
				.andExpect(status().isBadRequest());

		long id = create(token, "Promo", null, true, null, null);
		mockMvc.perform(multipart("/api/admin/banners/" + id + "/images/DESKTOP")
						.file(new MockMultipartFile("file", "a.txt", "image/jpeg", "not an image".getBytes()))
						.header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/admin/banners")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/admin/banners/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	private long create(String token, String title, String link, boolean active, LocalDate from, LocalDate to)
			throws Exception {
		String body = "{\"title\":\"%s\",\"linkUrl\":%s,\"active\":%s,\"startsOn\":%s,\"endsOn\":%s}".formatted(title,
				link == null ? "null" : "\"" + link + "\"", active,
				from == null ? "null" : "\"" + from + "\"", to == null ? "null" : "\"" + to + "\"");
		String response = mockMvc.perform(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private String upload(String token, long id, String variant) throws Exception {
		String response = mockMvc.perform(multipart("/api/admin/banners/" + id + "/images/" + variant)
						.file(new MockMultipartFile("file", "b.jpg", "image/jpeg", JPEG))
						.header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, variant.equals("DESKTOP") ? "$.desktopImageUrl" : "$.mobileImageUrl");
	}

	private String bearer() throws Exception {
		String body = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"marketer\",\"password\":\"marketer-password\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}
}
