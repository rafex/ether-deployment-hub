package dev.rafex.etherbrain.spi.model;

import java.util.List;
import java.util.Map;

/**
 * Minimal dependency-free JSON helpers shared by the SPI model clients.
 *
 * <p>Provides just enough to serialize request maps and extract a single string
 * field from a response without pulling in a JSON library. Used by the Ollama
 * and OpenAI SPI adapters, which previously carried identical private copies.
 *
 * <p>Not a general-purpose JSON parser: {@link #extractString} does a linear
 * scan for {@code "key": "value"} and unescapes the common sequences.
 */
public final class MiniJson {

    private MiniJson() {
    }

    /**
     * Serializes a map to a JSON object string.
     *
     * @param map the map to serialize (values may be String, Number, Boolean,
     *            Map, List or {@code null})
     * @return the JSON object representation
     */
    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            appendValue(sb, entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Extracts the string value for {@code key} from a JSON document.
     *
     * @param json the JSON text to scan
     * @param key  the field name (without surrounding quotes)
     * @return the unescaped string value, or {@code null} if the key or a valid
     *         string value is not found
     */
    public static String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) {
            return null;
        }

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) {
            return null;
        }

        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote < 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == startQuote + 1 || json.charAt(i - 1) != '\\')) {
                return sb.toString();
            }
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                sb.append(switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> next;
                });
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Appends a single value in JSON form to {@code sb}, recursing into maps and
     * lists.
     *
     * @param sb    the target builder
     * @param value the value to append
     */
    public static void appendValue(StringBuilder sb, Object value) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> sb.append("\"").append(s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")).append("\"");
            case Number n -> sb.append(n);
            case Boolean b -> sb.append(b);
            case Map<?, ?> m -> {
                sb.append("{");
                boolean first = true;
                for (var entry : m.entrySet()) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append("\"").append(entry.getKey()).append("\":");
                    appendValue(sb, entry.getValue());
                    first = false;
                }
                sb.append("}");
            }
            case List<?> list -> {
                sb.append("[");
                boolean first = true;
                for (Object item : list) {
                    if (!first) {
                        sb.append(",");
                    }
                    appendValue(sb, item);
                    first = false;
                }
                sb.append("]");
            }
            default -> sb.append("\"").append(value).append("\"");
        }
    }
}
