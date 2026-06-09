package dev.rafex.ether.logging.core.config;

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

import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.logging.core.format.EtherLogFormatter;
import dev.rafex.ether.logging.core.logger.EtherLog;

public final class LoggingConfigurator {

    private LoggingConfigurator() {
    }

    public static Logger configureRootLogger(final Level level) {
        return configureRootLogger(level, new EtherLogFormatter());
    }

    public static Logger configureRootLogger(final Level level, final EtherLogFormatter formatter) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(formatter, "formatter");

        final Logger root = Logger.getLogger("");
        root.setLevel(level);
        for (final Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        final var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        consoleHandler.setFormatter(formatter);
        root.addHandler(consoleHandler);
        return root;
    }

    public static Logger getLogger(final Class<?> type) {
        Objects.requireNonNull(type, "type");
        return EtherLog.get(type);
    }
}
