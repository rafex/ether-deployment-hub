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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SQLiteConfigTest {

    @Test
    void shouldCreateDefaultConfig() {
        SQLiteConfig config = SQLiteConfig.defaults();
        
        assertEquals(JournalMode.WAL, config.journalMode());
        assertEquals(SynchronousMode.NORMAL, config.synchronousMode());
        assertTrue(config.foreignKeys());
        assertEquals(5000, config.busyTimeout());
        assertFalse(config.caseSensitiveLike());
        assertFalse(config.recursiveTriggers());
        assertFalse(config.autoVacuum());
    }

    @Test
    void shouldCreateCustomConfig() {
        SQLiteConfig config = SQLiteConfig.builder()
            .journalMode(JournalMode.DELETE)
            .synchronousMode(SynchronousMode.FULL)
            .foreignKeys(false)
            .busyTimeout(10000)
            .caseSensitiveLike(true)
            .recursiveTriggers(true)
            .autoVacuum(true)
            .build();
        
        assertEquals(JournalMode.DELETE, config.journalMode());
        assertEquals(SynchronousMode.FULL, config.synchronousMode());
        assertFalse(config.foreignKeys());
        assertEquals(10000, config.busyTimeout());
        assertTrue(config.caseSensitiveLike());
        assertTrue(config.recursiveTriggers());
        assertTrue(config.autoVacuum());
    }

    @Test
    void shouldRejectNullJournalMode() {
        assertThrows(NullPointerException.class, () -> {
            SQLiteConfig.builder().journalMode(null);
        });
    }

    @Test
    void shouldRejectNullSynchronousMode() {
        assertThrows(NullPointerException.class, () -> {
            SQLiteConfig.builder().synchronousMode(null);
        });
    }

    @Test
    void shouldRejectNegativeBusyTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            SQLiteConfig.builder().busyTimeout(-1);
        });
    }

    @Test
    void shouldAllowZeroBusyTimeout() {
        SQLiteConfig config = SQLiteConfig.builder()
            .busyTimeout(0)
            .build();
        
        assertEquals(0, config.busyTimeout());
    }

    @Test
    void shouldHaveEqualsAndHashCode() {
        SQLiteConfig config1 = SQLiteConfig.builder()
            .journalMode(JournalMode.WAL)
            .synchronousMode(SynchronousMode.NORMAL)
            .foreignKeys(true)
            .busyTimeout(5000)
            .caseSensitiveLike(false)
            .recursiveTriggers(false)
            .autoVacuum(false)
            .build();
        
        SQLiteConfig config2 = SQLiteConfig.builder()
            .journalMode(JournalMode.WAL)
            .synchronousMode(SynchronousMode.NORMAL)
            .foreignKeys(true)
            .busyTimeout(5000)
            .caseSensitiveLike(false)
            .recursiveTriggers(false)
            .autoVacuum(false)
            .build();
        
        SQLiteConfig config3 = SQLiteConfig.builder()
            .journalMode(JournalMode.DELETE)
            .synchronousMode(SynchronousMode.FULL)
            .foreignKeys(false)
            .busyTimeout(10000)
            .caseSensitiveLike(true)
            .recursiveTriggers(true)
            .autoVacuum(true)
            .build();
        
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        
        assertFalse(config1.equals(config3));
        assertFalse(config1.hashCode() == config3.hashCode());
        
        // Self equality
        assertEquals(config1, config1);
        
        // Null equality
        assertFalse(config1.equals(null));
        
        // Different class
        assertFalse(config1.equals("not a config"));
    }

    @Test
    void shouldHaveToString() {
        SQLiteConfig config = SQLiteConfig.defaults();
        String str = config.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("SQLiteConfig"));
        assertTrue(str.contains("journalMode=WAL"));
        assertTrue(str.contains("synchronousMode=NORMAL"));
        assertTrue(str.contains("foreignKeys=true"));
        assertTrue(str.contains("busyTimeout=5000"));
        assertTrue(str.contains("caseSensitiveLike=false"));
        assertTrue(str.contains("recursiveTriggers=false"));
        assertTrue(str.contains("autoVacuum=false"));
    }

    @Test
    void shouldSupportAllJournalModes() {
        for (JournalMode mode : JournalMode.values()) {
            SQLiteConfig config = SQLiteConfig.builder()
                .journalMode(mode)
                .build();
            
            assertEquals(mode, config.journalMode());
        }
    }

    @Test
    void shouldSupportAllSynchronousModes() {
        for (SynchronousMode mode : SynchronousMode.values()) {
            SQLiteConfig config = SQLiteConfig.builder()
                .synchronousMode(mode)
                .build();
            
            assertEquals(mode, config.synchronousMode());
        }
    }
}