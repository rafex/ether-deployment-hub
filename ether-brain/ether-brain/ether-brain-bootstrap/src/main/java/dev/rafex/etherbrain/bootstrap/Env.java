package dev.rafex.etherbrain.bootstrap;

/**
 * Resolves configuration values from the environment.
 *
 * <p>Lookup order: OS environment variable first, then JVM system property,
 * falling back to the supplied default. Blank values are treated as absent so
 * an empty override never masks a meaningful default.
 */
public final class Env {

    private Env() {
    }

    /**
     * Returns the value for {@code name}, or {@code defaultValue} if unset/blank.
     *
     * @param name         the environment variable / system property name
     * @param defaultValue the fallback when neither source provides a value
     * @return the resolved value, or {@code defaultValue}
     */
    public static String get(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getProperty(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
