/*-
 * #%L
 * ether-cron
 * %%
 * Copyright (C) 2026 Raúl Eduardo González Argote
 * #L%
 */

package dev.rafex.ether.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class JdkCronSchedulerTest {

    @Test
    void shouldRunTaskAtFixedInterval() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        try (var scheduler = new JdkCronScheduler()) {
            final ScheduledTask task = scheduler.every("heartbeat", Duration.ofMillis(25), latch::countDown);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertFalse(task.isCancelled());
            assertTrue(task.cancel());
            assertTrue(task.isCancelled());
        }
    }

    @Test
    void shouldSupportExplicitInitialDelay() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        try (var scheduler = new JdkCronScheduler()) {
            scheduler.every("heartbeat", Duration.ZERO, Duration.ofMinutes(1), latch::countDown);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldKeepSchedulingAfterJobFailure() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger runs = new AtomicInteger();
        final List<JdkCronScheduler.CronFailure> failures = new ArrayList<>();

        try (var scheduler = new JdkCronScheduler(Executors.newSingleThreadScheduledExecutor(),
                Clock.systemDefaultZone(), failures::add)) {
            scheduler.every("unstable", Duration.ofMillis(25), () -> {
                latch.countDown();
                if (runs.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
            });

            assertTrue(latch.await(1, TimeUnit.SECONDS));
        }

        assertEquals(1, failures.size());
        assertEquals("unstable", failures.getFirst().taskName());
    }

    @Test
    void shouldKeepSchedulingWhenFailureHandlerFails() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        try (var scheduler = new JdkCronScheduler(Executors.newSingleThreadScheduledExecutor(),
                Clock.systemDefaultZone(), failure -> {
                    throw new IllegalStateException("handler failed");
                })) {
            scheduler.every("unstable-handler", Duration.ofMillis(25), () -> {
                latch.countDown();
                throw new IllegalStateException("boom");
            });

            assertTrue(latch.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldCalculateDailyDelayForFutureTimeToday() {
        final Clock clock = Clock.fixed(Instant.parse("2026-07-07T10:00:00Z"), ZoneId.of("UTC"));

        try (var scheduler = new JdkCronScheduler(Executors.newSingleThreadScheduledExecutor(), clock, failure -> {
        })) {
            final Duration delay = scheduler.delayUntilNextDailyRun(LocalTime.of(10, 30));

            assertEquals(Duration.ofMinutes(30), delay);
        }
    }

    @Test
    void shouldCalculateDailyDelayForTomorrowWhenTimeAlreadyPassed() {
        final Clock clock = Clock.fixed(Instant.parse("2026-07-07T10:30:00Z"), ZoneId.of("UTC"));

        try (var scheduler = new JdkCronScheduler(Executors.newSingleThreadScheduledExecutor(), clock, failure -> {
        })) {
            final Duration delay = scheduler.delayUntilNextDailyRun(LocalTime.of(10, 0));

            assertEquals(Duration.ofHours(23).plusMinutes(30), delay);
        }
    }

    @Test
    void shouldRescheduleDailyTaskAfterEachRun() throws Exception {
        final var executor = new ScheduledThreadPoolExecutor(1);
        final Clock clock = Clock.fixed(Instant.parse("2026-07-07T10:00:00Z"), ZoneId.of("UTC"));
        final CountDownLatch latch = new CountDownLatch(1);

        try (var scheduler = new JdkCronScheduler(executor, clock, failure -> {
        })) {
            final ScheduledTask task = scheduler.dailyAt("daily", LocalTime.of(10, 0, 0, 1_000_000), latch::countDown);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertFalse(task.isDone());
            assertTrue(task.cancel());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldAwaitTermination() throws Exception {
        final var scheduler = new JdkCronScheduler();

        scheduler.close();

        assertTrue(scheduler.awaitTermination(Duration.ofSeconds(1)));
    }

    @Test
    void shouldValidateInputs() {
        try (var scheduler = new JdkCronScheduler()) {
            assertThrows(IllegalArgumentException.class, () -> scheduler.everySeconds("bad", 0, () -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> scheduler.everyMinutes("bad", -1, () -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> scheduler.everyHours(" ", 1, () -> {
            }));
            assertThrows(IllegalArgumentException.class,
                    () -> scheduler.every("bad", Duration.ofMillis(-1), Duration.ofMillis(1), () -> {
                    }));
            assertThrows(NullPointerException.class, () -> scheduler.dailyAt("daily", null, () -> {
            }));
        }
    }
}
