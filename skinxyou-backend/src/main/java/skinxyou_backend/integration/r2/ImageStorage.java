package skinxyou_backend.integration.r2;

/** Object storage for product photos. */
public interface ImageStorage {

	/** Stores the bytes under {@code key} and returns the public URL. */
	String upload(String key, byte[] bytes, String contentType);

	void delete(String key);

	/** The storage key for a URL this storage returned, or null for foreign URLs (e.g. the BILLZ CDN). */
	String keyOf(String url);
}
