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

import java.util.List;
import java.util.Objects;

import org.glowroot.agent.api.Glowroot;

import dev.rafex.ether.observability.core.probe.ProbeAggregator;
import dev.rafex.ether.observability.core.probe.ProbeCheck;
import dev.rafex.ether.observability.core.probe.ProbeKind;
import dev.rafex.ether.observability.core.probe.ProbeReport;

/**
 * Bridge utilities that record Ether probe reports into Glowroot transaction
 * attributes.
 */
public final class GlowrootProbeReporter {

    private GlowrootProbeReporter() {
    }

    public static ProbeReport capture(final ProbeKind kind, final List<ProbeCheck> checks) {
        final var report = ProbeAggregator.aggregate(kind, checks);
        record(report);
        return report;
    }

    public static void record(final ProbeReport report) {
        Objects.requireNonNull(report, "report");
        try {
            Glowroot.addTransactionAttribute("probe.kind", report.kind().name());
            Glowroot.addTransactionAttribute("probe.status", report.status().name());
            for (final var result : report.results()) {
                final var key = sanitize(result.name());
                Glowroot.addTransactionAttribute("probe." + key + ".status", result.status().name());
                if (!result.detail().isBlank()) {
                    Glowroot.addTransactionAttribute("probe." + key + ".detail", result.detail());
                }
            }
        } catch (final Throwable ignore) {
            // Glowroot agent not present; do not affect the caller
        }
    }

    private static String sanitize(final String name) {
        return name == null || name.isBlank() ? "unnamed" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
