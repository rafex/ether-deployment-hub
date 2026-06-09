package ${package}.errors;

/**
 * Base checked exception for domain errors.
 */
public class AppError extends Exception {

    public AppError(final String message) {
        super(message);
    }

    public AppError(final String message, final Throwable cause) {
        super(message, cause);
    }
}
