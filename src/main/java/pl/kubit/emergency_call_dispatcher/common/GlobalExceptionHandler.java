package pl.kubit.emergency_call_dispatcher.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 * Turns exceptions into RFC 9457 {@link ProblemDetail} responses, in one place for the whole API.
 * <p>
 * Extending {@link ResponseEntityExceptionHandler} gives {@code ProblemDetail} responses for Spring MVC's own
 * errors for free (malformed JSON, wrong parameter type, unsupported method, ...). The methods below add
 * the application's exceptions:
 * <ul>
 *     <li>{@link ResourceNotFoundException} → 404</li>
 *     <li>{@link IllegalStatusTransitionException} → 409</li>
 *     <li>{@link ObjectOptimisticLockingFailureException} → 409 (someone else changed the same row)</li>
 *     <li>{@link IllegalArgumentException} → 400 (request rejected by a service, e.g. setting BUSY manually)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    ProblemDetail handleIllegalTransition(IllegalStatusTransitionException ex) {
        return problem(HttpStatus.CONFLICT, "Illegal status transition", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.debug("Optimistic locking conflict", ex);
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was modified by another request. Please retry.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    /**
     * Bean Validation errors on a {@code @Valid @RequestBody}. Keeps Spring's default response
     * and adds an {@code errors} map: field name → message, so the client knows what to fix.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new TreeMap<>(); // sorted, so the output is stable
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setDetail("Request body contains invalid fields");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
