/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

import java.util.Objects;

public record CronTask(String name, EtherJob job) {

    public CronTask {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        Objects.requireNonNull(job, "job");
    }
}
