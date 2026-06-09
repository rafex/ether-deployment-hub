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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SQLitePragmasTest {

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Test
    void shouldApplyDefaultConfig() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        
        SQLitePragmas.applyDefaults(connection);
        
        verify(connection).createStatement();
        verify(statement, times(10)).execute(anyString());
        verify(statement).close();
    }

    @Test
    void shouldApplyCustomConfig() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        
        SQLiteConfig config = SQLiteConfig.builder()
            .journalMode(JournalMode.DELETE)
            .synchronousMode(SynchronousMode.FULL)
            .foreignKeys(false)
            .busyTimeout(10000)
            .caseSensitiveLike(true)
            .recursiveTriggers(true)
            .autoVacuum(true)
            .build();
        
        SQLitePragmas.apply(connection, config);
        
        verify(connection).createStatement();
        verify(statement, times(10)).execute(anyString());
        verify(statement).close();
    }

    @Test
    void shouldEnableWal() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        
        SQLitePragmas.enableWal(connection);
        
        verify(connection).createStatement();
        verify(statement).execute("PRAGMA journal_mode = WAL");
        verify(statement).close();
    }

    @Test
    void shouldDisableWal() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        
        SQLitePragmas.disableWal(connection);
        
        verify(connection).createStatement();
        verify(statement).execute("PRAGMA journal_mode = DELETE");
        verify(statement).close();
    }

    @Test
    void shouldSetSynchronousMode() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        
        SQLitePragmas.setSynchronousMode(connection, SynchronousMode.NORMAL);
        
        verify(connection).createStatement();
        verify(statement).execute("PRAGMA synchronous = NORMAL");
        verify(statement).close();
    }

    @Test
    void shouldRejectNullConnection() {
        SQLiteConfig config = SQLiteConfig.defaults();
        
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.apply(null, config);
        });
        
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.applyDefaults(null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.enableWal(null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.disableWal(null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.setSynchronousMode(null, SynchronousMode.NORMAL);
        });
    }

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.apply(connection, null);
        });
    }

    @Test
    void shouldRejectNullSynchronousMode() {
        assertThrows(NullPointerException.class, () -> {
            SQLitePragmas.setSynchronousMode(connection, null);
        });
    }

    @Test
    void shouldHandleSqlException() throws SQLException {
        when(connection.createStatement()).thenThrow(new SQLException("Connection failed"));
        
        assertThrows(SQLException.class, () -> {
            SQLitePragmas.applyDefaults(connection);
        });
    }

    @Test
    void shouldCloseStatementOnException() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("PRAGMA failed"));
        
        assertThrows(SQLException.class, () -> {
            SQLitePragmas.applyDefaults(connection);
        });
        
        verify(statement).close();
    }
}