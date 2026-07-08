/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class JdkCronScheduler implements SchedulerPort {

    private final ScheduledExecutorService executor;
    private final Clock clock;
    private final Consumer<CronFailure> failureHandler;

    public JdkCronScheduler() {
        this(Executors.newSingleThreadScheduledExecutor(defaultThreadFactory()), Clock.systemDefaultZone(),
                CronFailure::printToStandardError);
    }

    public JdkCronScheduler(final int threads) {
        this(Executors.newScheduledThreadPool(validateThreads(threads), defaultThreadFactory()), Clock.systemDefaultZone(),
                CronFailure::printToStandardError);
    }

    public JdkCronScheduler(final ScheduledExecutorService executor) {
        this(executor, Clock.systemDefaultZone(), CronFailure::printToStandardError);
    }

    public JdkCronScheduler(final ScheduledExecutorService executor, final Clock clock,
            final Consumer<CronFailure> failureHandler) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    @Override
    public ScheduledTask everySeconds(final String name, final long seconds, final EtherJob job) {
        return every(name, Duration.ofSeconds(seconds), job);
    }

    @Override
    public ScheduledTask everyMinutes(final String name, final long minutes, final EtherJob job) {
        return every(name, Duration.ofMinutes(minutes), job);
    }

    @Override
    public ScheduledTask everyHours(final String name, final long hours, final EtherJob job) {
        return every(name, Duration.ofHours(hours), job);
    }

    public ScheduledTask every(final String name, final Duration interval, final EtherJob job) {
        return every(name, interval, interval, job);
    }

    public ScheduledTask every(final String name, final Duration initialDelay, final Duration interval,
            final EtherJob job) {
        final var task = new CronTask(name, job);
        validateNonNegative(initialDelay, "initialDelay");
        validatePositive(interval, "interval");

        final long initialDelayMillis = initialDelay.toMillis();
        final long delayMillis = interval.toMillis();
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(wrap(task), initialDelayMillis, delayMillis,
                TimeUnit.MILLISECONDS);
        return new JdkScheduledTask(task.name(), future);
    }

    @Override
    public ScheduledTask dailyAt(final String name, final LocalTime time, final EtherJob job) {
        final var task = new CronTask(name, job);
        Objects.requireNonNull(time, "time");

        final var handle = new ReschedulingTask(task.name());
        scheduleNextDailyRun(handle, task, time);
        return handle;
    }

    Duration delayUntilNextDailyRun(final LocalTime time) {
        Objects.requireNonNull(time, "time");
        final LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime next = now.toLocalDate().atTime(time);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public boolean awaitTermination(final Duration timeout) throws InterruptedException {
        validateNonNegative(timeout, "timeout");
        return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
    }

    void scheduleNextDailyRun(final ReschedulingTask handle, final CronTask task, final LocalTime time) {
        if (handle.isCancelled()) {
            return;
        }

        final long delayMillis = millisUntilNextDailyRun(time);
        try {
            final ScheduledFuture<?> future = executor.schedule(() -> {
                if (handle.isCancelled()) {
                    return;
                }
                wrap(task).run();
                scheduleNextDailyRun(handle, task, time);
            }, delayMillis, TimeUnit.MILLISECONDS);
            handle.replaceFuture(future);
        } catch (final RejectedExecutionException ignored) {
            handle.cancel();
        }
    }

    private long millisUntilNextDailyRun(final LocalTime time) {
        final long millis = delayUntilNextDailyRun(time).toMillis();
        return Math.max(1L, millis);
    }

    private Runnable wrap(final CronTask task) {
        return () -> {
            try {
                task.job().run();
            } catch (final Exception e) {
                handleFailure(task.name(), e);
            }
        };
    }

    private void handleFailure(final String taskName, final Exception cause) {
        try {
            failureHandler.accept(new CronFailure(taskName, cause));
        } catch (final Exception ignored) {
            CronFailure.printToStandardError(new CronFailure(taskName, cause));
        }
    }

    private static ThreadFactory defaultThreadFactory() {
        final AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            final Thread thread = new Thread(runnable, "ether-cron-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static int validateThreads(final int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be greater than zero");
        }
        return threads;
    }

    private static void validatePositive(final Duration duration, final String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        if (duration.toMillis() < 1L) {
            throw new IllegalArgumentException(name + " must be at least 1 millisecond");
        }
    }

    private static void validateNonNegative(final Duration duration, final String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        if (!duration.isZero() && duration.toMillis() < 1L) {
            throw new IllegalArgumentException(name + " must be zero or at least 1 millisecond");
        }
    }

    public record CronFailure(String taskName, Exception cause) {

        public CronFailure {
            if (taskName == null || taskName.isBlank()) {
                throw new IllegalArgumentException("Task name must not be blank");
            }
            Objects.requireNonNull(cause, "cause");
        }

        static void printToStandardError(final CronFailure failure) {
            System.err.println("Job failed: " + failure.taskName() + " - " + failure.cause().getMessage());
        }
    }

    private record JdkScheduledTask(String name, ScheduledFuture<?> future) implements ScheduledTask {

        @Override
        public boolean cancel() {
            return cancel(false);
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }
    }

    static final class ReschedulingTask implements ScheduledTask {

        private final String name;
        private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        private volatile boolean cancelled;

        ReschedulingTask(final String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        void replaceFuture(final ScheduledFuture<?> nextFuture) {
            Objects.requireNonNull(nextFuture, "nextFuture");
            if (cancelled) {
                nextFuture.cancel(false);
                return;
            }
            future.set(nextFuture);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean cancel() {
            return cancel(false);
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            cancelled = true;
            final ScheduledFuture<?> current = future.get();
            return current == null || current.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            final ScheduledFuture<?> current = future.get();
            return cancelled || current != null && current.isCancelled();
        }

        @Override
        public boolean isDone() {
            final ScheduledFuture<?> current = future.get();
            return cancelled || current != null && current.isDone();
        }
    }
}
