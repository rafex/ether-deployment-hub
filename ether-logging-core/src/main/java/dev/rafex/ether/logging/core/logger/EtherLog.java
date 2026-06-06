package dev.rafex.ether.logging.core.logger;

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

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.logging.core.format.LogMessageFormatter;

public final class EtherLog {

    private static final ClassValue<Logger> LOGGERS = new ClassValue<>() {
        @Override
        protected Logger computeValue(final Class<?> type) {
            return Logger.getLogger(type.getName());
        }
    };

    private EtherLog() {
    }

    public static Logger get(final Class<?> type) {
        return LOGGERS.get(type);
    }

    public static void info(final Class<?> type, final String message) {
        log(type, Level.INFO, null, message);
    }

    public static void info(final Class<?> type, final String message, final Object... args) {
        log(type, Level.INFO, null, LogMessageFormatter.format(message, args));
    }

    public static void info(final Class<?> type, final Throwable cause, final String message, final Object... args) {
        log(type, Level.INFO, cause, LogMessageFormatter.format(message, args));
    }

    public static void warn(final Class<?> type, final String message) {
        log(type, Level.WARNING, null, message);
    }

    public static void warn(final Class<?> type, final String message, final Object... args) {
        log(type, Level.WARNING, null, LogMessageFormatter.format(message, args));
    }

    public static void warn(final Class<?> type, final Throwable cause, final String message, final Object... args) {
        log(type, Level.WARNING, cause, LogMessageFormatter.format(message, args));
    }

    public static void error(final Class<?> type, final String message) {
        log(type, Level.SEVERE, null, message);
    }

    public static void error(final Class<?> type, final String message, final Object... args) {
        log(type, Level.SEVERE, null, LogMessageFormatter.format(message, args));
    }

    public static void error(final Class<?> type, final String message, final Throwable cause) {
        log(type, Level.SEVERE, cause, message);
    }

    public static void error(final Class<?> type, final Throwable cause, final String message, final Object... args) {
        log(type, Level.SEVERE, cause, LogMessageFormatter.format(message, args));
    }

    public static void debug(final Class<?> type, final String message) {
        log(type, Level.FINE, null, message);
    }

    public static void debug(final Class<?> type, final String message, final Object... args) {
        log(type, Level.FINE, null, LogMessageFormatter.format(message, args));
    }

    public static void debug(final Class<?> type, final Throwable cause, final String message, final Object... args) {
        log(type, Level.FINE, cause, LogMessageFormatter.format(message, args));
    }

    private static void log(final Class<?> type, final Level level, final Throwable cause, final String message) {
        final var logger = get(type);
        if (!logger.isLoggable(level)) {
            return;
        }
        if (cause == null) {
            logger.log(level, message);
            return;
        }
        logger.log(level, message, cause);
    }
}
