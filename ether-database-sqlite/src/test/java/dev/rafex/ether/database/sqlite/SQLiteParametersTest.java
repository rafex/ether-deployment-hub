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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Types;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.database.sqlite.sql.SQLiteParameters;

class SQLiteParametersTest {

    @Test
    void shouldCreateBlobParameter() {
        final byte[] data = new byte[]{1, 2, 3};
        assertEquals(Types.BLOB, SQLiteParameters.blob(data).sqlType());
        assertNotNull(SQLiteParameters.blob(data).value());
        
        // Null case
        assertEquals(Types.BLOB, SQLiteParameters.blob(null).sqlType());
        assertNull(SQLiteParameters.blob(null).value());
    }

    @Test
    void shouldCreateJsonParameter() {
        assertEquals(Types.OTHER, SQLiteParameters.json("{}").sqlType());
        assertEquals("{}", SQLiteParameters.json("{}").value());
        
        // Null case
        assertEquals(Types.OTHER, SQLiteParameters.json(null).sqlType());
        assertNull(SQLiteParameters.json(null).value());
    }

    @Test
    void shouldCreateIntegerParameter() {
        assertEquals(Types.BIGINT, SQLiteParameters.integer(42L).sqlType());
        assertEquals(42L, SQLiteParameters.integer(42L).value());
        
        // Null case
        assertEquals(Types.BIGINT, SQLiteParameters.integer(null).sqlType());
        assertNull(SQLiteParameters.integer(null).value());
    }

    @Test
    void shouldCreateRealParameter() {
        assertEquals(Types.DOUBLE, SQLiteParameters.real(3.14).sqlType());
        assertEquals(3.14, SQLiteParameters.real(3.14).value());
        
        // Null case
        assertEquals(Types.DOUBLE, SQLiteParameters.real(null).sqlType());
        assertNull(SQLiteParameters.real(null).value());
    }

    @Test
    void shouldCreateTextParameter() {
        assertEquals(Types.VARCHAR, SQLiteParameters.text("hello").sqlType());
        assertEquals("hello", SQLiteParameters.text("hello").value());
        
        // Null case
        assertEquals(Types.VARCHAR, SQLiteParameters.text(null).sqlType());
        assertNull(SQLiteParameters.text(null).value());
    }
}
