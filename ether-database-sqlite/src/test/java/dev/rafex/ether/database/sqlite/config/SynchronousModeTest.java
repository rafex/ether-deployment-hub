package dev.rafex.ether.database.sqlite.config;

/*-
 * #%L
 * ether-database-sqlite
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SynchronousModeTest {

    @Test
    void shouldHaveAllModes() {
        SynchronousMode[] modes = SynchronousMode.values();
        
        assertEquals(4, modes.length);
        assertEquals(SynchronousMode.OFF, modes[0]);
        assertEquals(SynchronousMode.NORMAL, modes[1]);
        assertEquals(SynchronousMode.FULL, modes[2]);
        assertEquals(SynchronousMode.EXTRA, modes[3]);
    }

    @Test
    void shouldHaveCorrectPragmaValues() {
        assertEquals(0, SynchronousMode.OFF.pragmaValue());
        assertEquals(1, SynchronousMode.NORMAL.pragmaValue());
        assertEquals(2, SynchronousMode.FULL.pragmaValue());
        assertEquals(3, SynchronousMode.EXTRA.pragmaValue());
    }

    @Test
    void shouldConvertToSql() {
        assertEquals("OFF", SynchronousMode.OFF.toSql());
        assertEquals("NORMAL", SynchronousMode.NORMAL.toSql());
        assertEquals("FULL", SynchronousMode.FULL.toSql());
        assertEquals("EXTRA", SynchronousMode.EXTRA.toSql());
    }

    @Test
    void shouldConvertToString() {
        assertEquals("OFF", SynchronousMode.OFF.name());
        assertEquals("NORMAL", SynchronousMode.NORMAL.name());
        assertEquals("FULL", SynchronousMode.FULL.name());
        assertEquals("EXTRA", SynchronousMode.EXTRA.name());
    }

    @Test
    void shouldParseFromString() {
        assertEquals(SynchronousMode.OFF, SynchronousMode.valueOf("OFF"));
        assertEquals(SynchronousMode.NORMAL, SynchronousMode.valueOf("NORMAL"));
        assertEquals(SynchronousMode.FULL, SynchronousMode.valueOf("FULL"));
        assertEquals(SynchronousMode.EXTRA, SynchronousMode.valueOf("EXTRA"));
    }

    @Test
    void shouldHaveDocumentation() {
        // Just verify that the enum values exist and have proper toString
        for (SynchronousMode mode : SynchronousMode.values()) {
            assertNotNull(mode.toString());
        }
    }
}