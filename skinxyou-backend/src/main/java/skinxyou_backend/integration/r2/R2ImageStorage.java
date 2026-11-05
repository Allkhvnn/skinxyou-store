package skinxyou_backend.integration.r2;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/** R2 through its S3-compatible API. Objects are immutable (unique keys), so they are cached for a year. */
@Slf4j
public class R2ImageStorage implements ImageStorage {

	private final R2Properties properties;
	private final S3Client client;

	public R2ImageStorage(R2Properties properties) {
		this.properties = properties;
		this.client = properties.isConfigured()
				? S3Client.builder()
						.endpointOverride(URI.create(properties.endpoint()))
						// R2 ignores the region but the SDK requires one; "auto" is what Cloudflare documents.
						.region(Region.of("auto"))
						.credentialsProvider(StaticCredentialsProvider.create(
								AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
						.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
						.build()
				: null;
		if (client == null) {
			log.warn("R2 is not configured (R2_*); photo uploads in the admin panel are disabled");
		}
	}

	@Override
	public String upload(String key, byte[] bytes, String contentType) {
		requireConfigured();
		client.putObject(b -> b.bucket(properties.bucket()).key(key).contentType(contentType)
				.cacheControl("public, max-age=31536000, immutable"), RequestBody.fromBytes(bytes));
		return publicBase() + "/" + key;
	}

	@Override
	public void delete(String key) {
		requireConfigured();
		client.deleteObject(b -> b.bucket(properties.bucket()).key(key));
	}

	@Override
	public String keyOf(String url) {
		if (url == null || !properties.isConfigured() || !url.startsWith(publicBase() + "/")) {
			return null;
		}
		return url.substring(publicBase().length() + 1);
	}

	private String publicBase() {
		String base = properties.publicUrl();
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private void requireConfigured() {
		if (client == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Хранилище фото не настроено");
		}
	}
}
