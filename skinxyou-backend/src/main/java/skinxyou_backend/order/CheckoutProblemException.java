package skinxyou_backend.order;

import java.util.List;

import lombok.Getter;
import skinxyou_backend.checkout.Quote;

/** The cart cannot be ordered as is (unavailable products or not enough stock). */
@Getter
public class CheckoutProblemException extends RuntimeException {

	private final List<Quote.Problem> problems;

	public CheckoutProblemException(List<Quote.Problem> problems) {
		super("Cart contains unavailable items");
		this.problems = problems;
	}
}
