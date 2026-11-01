package skinxyou_backend.order;

/** Published inside the placing transaction; listeners run after it commits. */
public record OrderPlacedEvent(Long orderId) {
}
