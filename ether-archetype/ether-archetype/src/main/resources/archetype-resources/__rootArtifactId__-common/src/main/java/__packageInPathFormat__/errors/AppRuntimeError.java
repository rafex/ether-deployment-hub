package ${package}.errors;

/**
 * Base unchecked exception for unrecoverable domain errors.
 */
public class AppRuntimeError extends RuntimeException {

    public AppRuntimeError(final String message) {
        super(message);
    }

    public AppRuntimeError(final String message, final Throwable cause) {
        super(message, cause);
    }
}
