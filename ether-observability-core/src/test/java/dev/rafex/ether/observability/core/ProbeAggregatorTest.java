package dev.rafex.ether.observability.core;

/*-
 * #%L
 * ether-observability-core
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.observability.core.probe.ProbeAggregator;
import dev.rafex.ether.observability.core.probe.ProbeKind;
import dev.rafex.ether.observability.core.probe.ProbeResult;
import dev.rafex.ether.observability.core.probe.ProbeStatus;

class ProbeAggregatorTest {

    @Test
    void shouldAggregateToDownWhenAnyProbeIsDown() {
        final var report = ProbeAggregator.aggregate(ProbeKind.HEALTH,
                List.of(() -> new ProbeResult("db", ProbeKind.HEALTH, ProbeStatus.UP, ""),
                        () -> new ProbeResult("cache", ProbeKind.HEALTH, ProbeStatus.DOWN, "timeout")));

        assertEquals(ProbeStatus.DOWN, report.status());
        assertEquals(ProbeStatus.UP, report.byName().get("db"));
        assertEquals(ProbeStatus.DOWN, report.byName().get("cache"));
    }
}
