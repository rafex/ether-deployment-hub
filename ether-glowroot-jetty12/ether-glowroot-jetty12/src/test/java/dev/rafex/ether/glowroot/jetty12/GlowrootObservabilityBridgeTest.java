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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.observability.core.probe.ProbeCheck;
import dev.rafex.ether.observability.core.probe.ProbeKind;
import dev.rafex.ether.observability.core.probe.ProbeReport;
import dev.rafex.ether.observability.core.probe.ProbeResult;
import dev.rafex.ether.observability.core.probe.ProbeStatus;
import dev.rafex.ether.observability.core.request.RequestIdGenerator;
import dev.rafex.ether.observability.core.timing.TimingSample;

class GlowrootObservabilityBridgeTest {

    @Test
    void requestIdGeneratorShouldReturnIdsWithoutThrowing() {
        final RequestIdGenerator generator = new GlowrootRequestIdGenerator();

        final var requestId = generator.nextId();

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
    }

    @Test
    void timingRecorderShouldAcceptSamples() {
        final var recorder = new GlowrootTimingRecorder();
        final var sample = new TimingSample("http.request", Instant.now(), Instant.now().plusMillis(12));

        assertDoesNotThrow(() -> recorder.record(sample));
    }

    @Test
    void probeReporterShouldCaptureProbeReports() {
        final ProbeCheck db = () -> new ProbeResult("db", ProbeKind.HEALTH, ProbeStatus.UP, "");
        final ProbeCheck cache = () -> new ProbeResult("cache", ProbeKind.HEALTH, ProbeStatus.DEGRADED, "warmup");

        final ProbeReport report = GlowrootProbeReporter.capture(ProbeKind.HEALTH, List.of(db, cache));

        assertNotNull(report);
        assertDoesNotThrow(() -> GlowrootProbeReporter.record(report));
    }
}
