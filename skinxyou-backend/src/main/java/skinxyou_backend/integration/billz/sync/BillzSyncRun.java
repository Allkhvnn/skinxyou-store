package skinxyou_backend.integration.billz.sync;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Log entry of one BILLZ catalog sync, shown in the admin panel. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class BillzSyncRun {

	private static final int MAX_ERROR_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SyncType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SyncStatus status;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant finishedAt;

	private Instant updatedSince;

	private int productsFetched;

	private int productsCreated;

	private int productsUpdated;

	private int productsDeactivated;

	private String errorMessage;

	BillzSyncRun(SyncType type, Instant startedAt, Instant updatedSince) {
		this.type = type;
		this.status = SyncStatus.RUNNING;
		this.startedAt = startedAt;
		this.updatedSince = updatedSince;
	}

	void succeed(Instant finishedAt) {
		this.status = SyncStatus.SUCCESS;
		this.finishedAt = finishedAt;
	}

	void fail(Instant finishedAt, String message) {
		this.status = SyncStatus.FAILED;
		this.finishedAt = finishedAt;
		this.errorMessage = message == null || message.length() <= MAX_ERROR_LENGTH
				? message
				: message.substring(0, MAX_ERROR_LENGTH);
	}
}
