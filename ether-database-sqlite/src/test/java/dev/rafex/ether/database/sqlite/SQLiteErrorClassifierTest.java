package dev.rafex.ether.database.sqlite;

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
 * IMPLIED, INCLUDING BUT NOT LIMITED TO the WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorClassifier;
import dev.rafex.ether.database.sqlite.errors.SQLiteErrorCodes;

class SQLiteErrorClassifierTest {

    @Test
    void shouldClassifyKnownErrorCodes() {
        // SQLException constructor: (String reason, String sqlState, int vendorCode)
        SQLException uniqueViolation = new SQLException("duplicate", null, SQLiteErrorCodes.UNIQUE_VIOLATION);
        DatabaseAccessException result1 = SQLiteErrorClassifier.classify(uniqueViolation);
        assertNotNull(result1);
        assertEquals("duplicate", result1.getMessage());
        assertEquals(uniqueViolation, result1.getCause());
        
        SQLException locked = new SQLException("locked", null, SQLiteErrorCodes.LOCKED);
        DatabaseAccessException result2 = SQLiteErrorClassifier.classify(locked);
        assertNotNull(result2);
        assertEquals("locked", result2.getMessage());
        assertEquals(locked, result2.getCause());
        
        SQLException foreignKey = new SQLException("foreign key", null, SQLiteErrorCodes.FOREIGN_KEY_VIOLATION);
        DatabaseAccessException result3 = SQLiteErrorClassifier.classify(foreignKey);
        assertNotNull(result3);
        assertEquals("foreign key", result3.getMessage());
        assertEquals(foreignKey, result3.getCause());
        
        SQLException notNull = new SQLException("not null", null, SQLiteErrorCodes.NOT_NULL_VIOLATION);
        DatabaseAccessException result4 = SQLiteErrorClassifier.classify(notNull);
        assertNotNull(result4);
        assertEquals("not null", result4.getMessage());
        assertEquals(notNull, result4.getCause());
        
        SQLException check = new SQLException("check", null, SQLiteErrorCodes.CHECK_VIOLATION);
        DatabaseAccessException result5 = SQLiteErrorClassifier.classify(check);
        assertNotNull(result5);
        assertEquals("check", result5.getMessage());
        assertEquals(check, result5.getCause());
        
        SQLException busy = new SQLException("busy", null, SQLiteErrorCodes.BUSY);
        DatabaseAccessException result6 = SQLiteErrorClassifier.classify(busy);
        assertNotNull(result6);
        assertEquals("busy", result6.getMessage());
        assertEquals(busy, result6.getCause());
    }

    @Test
    void shouldClassifyWithCustomMessage() {
        SQLException exception = new SQLException("duplicate", null, SQLiteErrorCodes.UNIQUE_VIOLATION);
        DatabaseAccessException result = SQLiteErrorClassifier.classify("Custom message", exception);
        
        assertNotNull(result);
        assertEquals("Custom message", result.getMessage());
        assertEquals(exception, result.getCause());
    }

    @Test
    void shouldThrowNullPointerExceptionForNullException() {
        assertThrows(NullPointerException.class, () -> {
            SQLiteErrorClassifier.classify((SQLException) null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            SQLiteErrorClassifier.classify("message", null);
        });
    }
}