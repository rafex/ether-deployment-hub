package dev.rafex.ether.observability.core.probe;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para agregar resultados de múltiples probes de verificación.
 * Calcula el estado agregado basado en los resultados individuales.
 */
public final class ProbeAggregator {

    private ProbeAggregator() {
        // Clase de utilidad - no instanciable
    }

    /**
     * Agrega los resultados de múltiples probes de verificación.
     * 
     * @param kind el tipo de probe
     * @param checks las verificaciones a ejecutar
     * @return el reporte con el estado agregado
     */
    public static ProbeReport aggregate(final ProbeKind kind, final List<ProbeCheck> checks) {
        final var results = new ArrayList<ProbeResult>();
        for (final var check : checks == null ? List.<ProbeCheck>of() : checks) {
            results.add(check.execute());
        }
        return new ProbeReport(kind, aggregateStatus(results), results);
    }

    /**
     * Calcula el estado agregado basado en los resultados individuales.
     * 
     * @param results lista de resultados de probes
     * @return el estado agregado (DOWN si hay algún DOWN, DEGRADED si hay algún DEGRADED, UP si todos UP)
     */
    private static ProbeStatus aggregateStatus(final List<ProbeResult> results) {
        boolean degraded = false;
        for (final var result : results) {
            if (result.status() == ProbeStatus.DOWN) {
                return ProbeStatus.DOWN;
            }
            if (result.status() == ProbeStatus.DEGRADED) {
                degraded = true;
            }
        }
        return degraded ? ProbeStatus.DEGRADED : ProbeStatus.UP;
    }
}
