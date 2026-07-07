/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

public class CronException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CronException(final String message) {
        super(message);
    }

    public CronException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
