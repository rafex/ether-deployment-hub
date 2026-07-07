/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

public interface ScheduledTask {

    String name();

    boolean cancel();

    boolean isCancelled();

    boolean isDone();
}
