package skinxyou_backend.catalog.category;

/** @param parentSlug null for top-level categories or when the parent has not been synced */
public record CategoryResponse(String slug, String name, String parentSlug, long productCount) {
}
