package dev.rafex.etherbrain.tools.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rafex.etherbrain.tools.local.ExternalTool.OutputMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalToolTest {

    private static final String SCHEMA = "{\"type\":\"object\"}";

    // ── accessors ──────────────────────────────────────────────────────────────

    @Test
    void exposesNameDescriptionAndSchema() {
        var tool = new ExternalTool("echo_tool", "Echoes input", SCHEMA,
                List.of("echo", "hi"), OutputMode.STDOUT);
        assertEquals("echo_tool", tool.name());
        assertEquals("Echoes input", tool.description());
        assertEquals(SCHEMA, tool.inputSchema());
    }

    // ── execute() — STDOUT mode ────────────────────────────────────────────────

    @Test
    void executeStdoutReturnsCapturedOutput() throws Exception {
        var tool = new ExternalTool("echo_tool", "d", SCHEMA,
                List.of("echo", "hello world"), OutputMode.STDOUT);
        var result = tool.execute("{}", null);
        assertTrue(result.success());
        assertEquals("hello world", result.content());
    }

    @Test
    void executeSubstitutesArgTemplate() throws Exception {
        var tool = new ExternalTool("echo_tool", "d", SCHEMA,
                List.of("echo", "${msg}"), OutputMode.STDOUT);
        var result = tool.execute("{\"msg\":\"from-arg\"}", null);
        assertTrue(result.success());
        assertEquals("from-arg", result.content());
    }

    @Test
    void executeNonZeroExitReturnsFailure() throws Exception {
        var tool = new ExternalTool("fail_tool", "d", SCHEMA,
                List.of("false"), OutputMode.STDOUT);
        var result = tool.execute("{}", null);
        assertFalse(result.success());
        assertTrue(result.content().contains("exited with code"));
    }

    @Test
    void executeEmptyOutputReturnsFailure() throws Exception {
        var tool = new ExternalTool("empty_tool", "d", SCHEMA,
                List.of("true"), OutputMode.STDOUT);
        var result = tool.execute("{}", null);
        assertFalse(result.success());
        assertTrue(result.content().contains("no output"));
    }

    @Test
    void executeNullArgumentsTreatedAsEmptyObject() throws Exception {
        var tool = new ExternalTool("echo_tool", "d", SCHEMA,
                List.of("echo", "constant"), OutputMode.STDOUT);
        var result = tool.execute(null, null);
        assertTrue(result.success());
        assertEquals("constant", result.content());
    }

    // ── execute() — FILE mode ──────────────────────────────────────────────────

    @Test
    void executeFileModeReadsOutputFile() throws Exception {
        // 'true' writes nothing to ${__output__}; the file exists but is empty
        // so the tool should report "no output". Use a shell to write into it.
        var tool = new ExternalTool("file_tool", "d", SCHEMA,
                List.of("sh", "-c", "printf 'file-content' > ${__output__}"), OutputMode.FILE);
        var result = tool.execute("{}", null);
        assertTrue(result.success());
        assertEquals("file-content", result.content());
    }
}
