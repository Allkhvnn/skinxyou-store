package skinxyou_backend.common.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import skinxyou_backend.order.CheckoutProblemException;
import skinxyou_backend.order.IllegalStatusTransitionException;

/** RFC 9457 error bodies; adds machine-readable details the checkout form needs. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	public record FieldError(String field, String message) {
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте введённые данные");
		List<FieldError> errors = ex.getBindingResult().getAllErrors().stream()
				.map(e -> new FieldError(
						e instanceof org.springframework.validation.FieldError fe ? fe.getField() : e.getObjectName(),
						e.getDefaultMessage()))
				.toList();
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(IllegalStatusTransitionException.class)
	ResponseEntity<ProblemDetail> handleIllegalTransition(IllegalStatusTransitionException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
	}

	/** Two staff members saved the same order at the same moment. */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Заказ уже изменил другой сотрудник. Обновите страницу."));
	}

	@ExceptionHandler(CheckoutProblemException.class)
	ResponseEntity<ProblemDetail> handleCheckoutProblem(CheckoutProblemException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Некоторых товаров нет в нужном количестве");
		problem.setProperty("problems", ex.getProblems());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}
}
