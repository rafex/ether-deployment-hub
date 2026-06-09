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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporte de resultados de probes de verificación.
 * Es un record inmutable que contiene el tipo de probe, el estado agregado
 * y los resultados individuales de cada verificación.
 * 
 * @param kind el tipo de probe
 * @param status el estado agregado
 * @param results los resultados individuales
 */
public record ProbeReport(ProbeKind kind, ProbeStatus status, List<ProbeResult> results) {

    public ProbeReport {
        if (kind == null || status == null) {
            throw new IllegalArgumentException("kind and status are required");
        }
        results = results == null ? List.of() : List.copyOf(results);
    }

    /**
     * Devuelve un mapa con el nombre de cada probe y su estado.
     * 
     * @return mapa de nombres a estados
     */
    public Map<String, ProbeStatus> byName() {
        final var out = new LinkedHashMap<String, ProbeStatus>();
        for (final var result : results) {
            out.put(result.name(), result.status());
        }
        return Map.copyOf(out);
    }
}
