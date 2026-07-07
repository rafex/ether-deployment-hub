/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

public final class CronSchedulers {

    private CronSchedulers() {
    }

    public static JdkCronScheduler singleThread() {
        return new JdkCronScheduler();
    }

    public static JdkCronScheduler fixedThreads(final int threads) {
        return new JdkCronScheduler(threads);
    }
}
