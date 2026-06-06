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

class JournalModeTest {

    @Test
    void shouldHaveAllModes() {
        JournalMode[] modes = JournalMode.values();
        
        assertEquals(6, modes.length);
        assertEquals(JournalMode.WAL, modes[0]);
        assertEquals(JournalMode.DELETE, modes[1]);
        assertEquals(JournalMode.TRUNCATE, modes[2]);
        assertEquals(JournalMode.PERSIST, modes[3]);
        assertEquals(JournalMode.MEMORY, modes[4]);
        assertEquals(JournalMode.OFF, modes[5]);
    }

    @Test
    void shouldConvertToString() {
        assertEquals("WAL", JournalMode.WAL.name());
        assertEquals("DELETE", JournalMode.DELETE.name());
        assertEquals("TRUNCATE", JournalMode.TRUNCATE.name());
        assertEquals("PERSIST", JournalMode.PERSIST.name());
        assertEquals("MEMORY", JournalMode.MEMORY.name());
        assertEquals("OFF", JournalMode.OFF.name());
    }

    @Test
    void shouldParseFromString() {
        assertEquals(JournalMode.WAL, JournalMode.valueOf("WAL"));
        assertEquals(JournalMode.DELETE, JournalMode.valueOf("DELETE"));
        assertEquals(JournalMode.TRUNCATE, JournalMode.valueOf("TRUNCATE"));
        assertEquals(JournalMode.PERSIST, JournalMode.valueOf("PERSIST"));
        assertEquals(JournalMode.MEMORY, JournalMode.valueOf("MEMORY"));
        assertEquals(JournalMode.OFF, JournalMode.valueOf("OFF"));
    }

    @Test
    void shouldHaveDocumentation() {
        // Just verify that the enum values exist and have proper toString
        for (JournalMode mode : JournalMode.values()) {
            assertNotNull(mode.toString());
        }
    }
}