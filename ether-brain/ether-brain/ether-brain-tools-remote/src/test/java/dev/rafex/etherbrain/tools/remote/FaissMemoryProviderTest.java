package dev.rafex.etherbrain.tools.remote;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.rafex.etherbrain.ports.auth.TokenProvider;
import org.junit.jupiter.api.Test;

class FaissMemoryProviderTest {

    private static final TokenProvider STATIC_TOKEN = () -> "test-api-key";

    private static FaissMemoryProvider provider() {
        // Points to an unreachable port so createFaissSession() fails gracefully
        // (getOrCreateSession swallows the exception and returns null).
        return new FaissMemoryProvider("https://127.0.0.1:1/", "docs", STATIC_TOKEN, true, 60);
    }

    @Test
    void buildsWithSkipTlsVerify() {
        assertDoesNotThrow(FaissMemoryProviderTest::provider);
        assertNotNull(provider());
    }

    @Test
    void buildsWithoutSkipTlsVerify() {
        assertDoesNotThrow(
                () -> new FaissMemoryProvider("https://vps:8443", "docs", STATIC_TOKEN, false, 30));
    }

    @Test
    void getOrCreateSessionReturnsNullWhenServiceUnreachable() {
        // The remote service is unreachable, so no faiss session can be created.
        assertNull(provider().getOrCreateSession("session-1"));
    }
}
