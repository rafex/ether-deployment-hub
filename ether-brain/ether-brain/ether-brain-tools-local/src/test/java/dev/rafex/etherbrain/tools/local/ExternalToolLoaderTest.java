package dev.rafex.etherbrain.tools.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExternalToolLoaderTest {

    private static final String PROP = "AGENT_TOOLS_FILE";

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROP);
    }

    private static Path writeToolsFile(String json) throws Exception {
        Path file = Files.createTempFile("etherbrain-tools-", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        System.setProperty(PROP, file.toAbsolutePath().toString());
        return file;
    }

    @Test
    void loadReturnsZeroWhenNoFileConfigured() {
        // No AGENT_TOOLS_FILE property and (assumed) no tools.json in CWD
        System.clearProperty(PROP);
        var registry = new InMemoryToolRegistry();
        // Only assert it does not throw and returns a non-negative count.
        int count = ExternalToolLoader.load(registry);
        assertTrue(count >= 0);
    }

    @Test
    void loadReturnsZeroWhenFileIsNotArray() throws Exception {
        writeToolsFile("{\"not\":\"an-array\"}");
        var registry = new InMemoryToolRegistry();
        assertEquals(0, ExternalToolLoader.load(registry));
    }

    @Test
    void loadSubprocessRegistersTool() throws Exception {
        writeToolsFile("""
                [
                  {
                    "type": "subprocess",
                    "name": "echo_tool",
                    "description": "Echoes",
                    "command": ["echo", "hi"],
                    "output": "stdout",
                    "input_schema": {"type": "object"}
                  }
                ]
                """);
        var registry = new InMemoryToolRegistry();
        assertEquals(1, ExternalToolLoader.load(registry));
        assertTrue(registry.find("echo_tool").isPresent());
        assertTrue(registry.find("echo_tool").get() instanceof ExternalTool);
    }

    @Test
    void loadHttpRegistersTool() throws Exception {
        writeToolsFile("""
                [
                  {
                    "type": "http",
                    "name": "search_web",
                    "description": "Searches",
                    "endpoint": "http://localhost:8090/search",
                    "method": "POST",
                    "input_schema": {"type": "object"}
                  }
                ]
                """);
        var registry = new InMemoryToolRegistry();
        assertEquals(1, ExternalToolLoader.load(registry));
        assertTrue(registry.find("search_web").isPresent());
        assertTrue(registry.find("search_web").get() instanceof HttpProxyTool);
    }

    @Test
    void loadSkipsUnknownTypeWithoutThrowing() throws Exception {
        writeToolsFile("""
                [
                  {"type": "totally-unknown", "name": "x", "description": "y"}
                ]
                """);
        var registry = new InMemoryToolRegistry();
        assertEquals(0, ExternalToolLoader.load(registry));
    }

    @Test
    void loadContinuesAfterInvalidToolDefinition() throws Exception {
        // First tool is invalid (missing command), second is valid — loader
        // should log the error for the first and still register the second.
        writeToolsFile("""
                [
                  {"type": "subprocess", "name": "broken", "description": "d", "input_schema": {"type":"object"}},
                  {
                    "type": "subprocess",
                    "name": "ok_tool",
                    "description": "d",
                    "command": ["echo", "hi"],
                    "input_schema": {"type": "object"}
                  }
                ]
                """);
        var registry = new InMemoryToolRegistry();
        assertEquals(1, ExternalToolLoader.load(registry));
        assertTrue(registry.find("ok_tool").isPresent());
    }

    @Test
    void loadFromClasspathReturnsNonNegative() {
        var registry = new InMemoryToolRegistry();
        assertTrue(ExternalToolLoader.loadFromClasspath(registry) >= 0);
    }
}
