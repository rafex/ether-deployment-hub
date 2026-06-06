package dev.rafex.ether.observability.core.timing;

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

import java.time.Duration;
import java.time.Instant;

/**
 * Muestra de tiempo para medir la duración de una operación.
 * Es un record inmutable que contiene el nombre de la operación,
 * el momento de inicio y el momento de fin.
 * 
 * @param name el nombre de la operación
 * @param startedAt el instante de inicio
 * @param finishedAt el instante de fin
 */
public record TimingSample(String name, Instant startedAt, Instant finishedAt) {

    public TimingSample {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (startedAt == null || finishedAt == null) {
            throw new IllegalArgumentException("startedAt and finishedAt are required");
        }
    }

    /**
     * Calcula la duración entre el inicio y el fin.
     * 
     * @return la duración de la operación
     */
    public Duration duration() {
        return Duration.between(startedAt, finishedAt);
    }
}
