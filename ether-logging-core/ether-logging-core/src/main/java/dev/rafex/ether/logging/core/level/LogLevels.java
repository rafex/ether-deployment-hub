package dev.rafex.ether.logging.core.level;

/*-
 * #%L
 * ether-logging-core
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

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class LogLevels {

    private LogLevels() {
    }

    public static Level parse(final String rawLevel, final Level defaultLevel) {
        Objects.requireNonNull(defaultLevel, "defaultLevel");
        if (rawLevel == null || rawLevel.isBlank()) {
            return defaultLevel;
        }

        return switch (rawLevel.trim().toUpperCase(Locale.ROOT)) {
        case "TRACE" -> Level.FINER;
        case "DEBUG" -> Level.FINE;
        case "INFO" -> Level.INFO;
        case "WARN", "WARNING" -> Level.WARNING;
        case "ERROR" -> Level.SEVERE;
        default -> defaultLevel;
        };
    }

    public static boolean isSupported(final String rawLevel) {
        if (rawLevel == null || rawLevel.isBlank()) {
            return false;
        }
        return switch (rawLevel.trim().toUpperCase(Locale.ROOT)) {
        case "TRACE", "DEBUG", "INFO", "WARN", "WARNING", "ERROR" -> true;
        default -> false;
        };
    }
}
