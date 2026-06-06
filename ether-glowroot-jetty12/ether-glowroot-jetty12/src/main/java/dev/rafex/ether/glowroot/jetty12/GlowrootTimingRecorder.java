package dev.rafex.ether.glowroot.jetty12;

/*-
 * #%L
 * ether-glowroot-jetty12
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Objects;

import org.glowroot.agent.api.Glowroot;

import dev.rafex.ether.observability.core.timing.TimingRecorder;
import dev.rafex.ether.observability.core.timing.TimingSample;

/**
 * {@link TimingRecorder} implementation that publishes timing samples into
 * Glowroot transaction attributes.
 */
public final class GlowrootTimingRecorder implements TimingRecorder {

    @Override
    public void record(final TimingSample sample) {
        Objects.requireNonNull(sample, "sample");
        final var key = sanitize(sample.name());
        final var durationMs = Long.toString(sample.duration().toMillis());
        try {
            Glowroot.addTransactionAttribute("timing.last_name", sample.name());
            Glowroot.addTransactionAttribute("timing." + key + ".ms", durationMs);
            Glowroot.addTransactionAttribute("timing." + key + ".started_at", sample.startedAt().toString());
            Glowroot.addTransactionAttribute("timing." + key + ".finished_at", sample.finishedAt().toString());
        } catch (final Throwable ignore) {
            // Glowroot agent not present; do not affect request processing
        }
    }

    private static String sanitize(final String name) {
        return name == null || name.isBlank() ? "unnamed" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
