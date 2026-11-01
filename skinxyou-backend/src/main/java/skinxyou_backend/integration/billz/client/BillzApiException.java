package skinxyou_backend.integration.billz.client;

public class BillzApiException extends RuntimeException {

	private final int status;

	public BillzApiException(int status, String message) {
		super(message);
		this.status = status;
	}

	public BillzApiException(String message, Throwable cause) {
		super(message, cause);
		this.status = 0;
	}

	/** HTTP status, or 0 when no response was received. */
	public int getStatus() {
		return status;
	}
}
