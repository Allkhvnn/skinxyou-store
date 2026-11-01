package skinxyou_backend.integration.billz.sync;

public enum SyncType {
	/** Only products changed since the last successful run; cannot detect removals. */
	DELTA,
	/** Whole catalog; products missing from the export become unavailable. */
	FULL
}
