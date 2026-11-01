package skinxyou_backend.integration.billz.sync;

/** Sync went from working to failing ({@code healthy=false}) or recovered ({@code healthy=true}). */
public record SyncHealthChangedEvent(boolean healthy, SyncType type, String errorMessage) {
}
