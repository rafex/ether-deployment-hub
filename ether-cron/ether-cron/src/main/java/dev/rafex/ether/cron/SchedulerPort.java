/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

import java.time.Duration;
import java.time.LocalTime;

public interface SchedulerPort extends AutoCloseable {

    ScheduledTask everySeconds(String name, long seconds, EtherJob job);

    ScheduledTask everyMinutes(String name, long minutes, EtherJob job);

    ScheduledTask everyHours(String name, long hours, EtherJob job);

    ScheduledTask dailyAt(String name, LocalTime time, EtherJob job);

    boolean awaitTermination(Duration timeout) throws InterruptedException;

    void shutdownNow();

    @Override
    void close();
}
