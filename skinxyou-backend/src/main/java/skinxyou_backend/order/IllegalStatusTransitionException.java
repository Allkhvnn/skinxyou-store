package skinxyou_backend.order;

public class IllegalStatusTransitionException extends RuntimeException {

	public IllegalStatusTransitionException(String message) {
		super(message);
	}
}
