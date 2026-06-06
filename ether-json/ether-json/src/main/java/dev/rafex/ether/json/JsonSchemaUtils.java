/*-
 * #%L
 * ether-json
 * %%
 * Copyright (C) 2025 Raúl Eduardo González Argote
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

package dev.rafex.ether.json;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

public final class JsonSchemaUtils {

    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private JsonSchemaUtils() {
    }

    public static List<String> validate(final JsonNode schemaNode, final JsonNode documentNode) {
        Objects.requireNonNull(schemaNode, "schemaNode");
        Objects.requireNonNull(documentNode, "documentNode");

        final JsonSchema schema = FACTORY.getSchema(schemaNode);
        final Set<com.networknt.schema.ValidationMessage> errors = schema.validate(documentNode);

        return errors.stream().map(com.networknt.schema.ValidationMessage::getMessage).sorted().toList();
    }

    public static List<String> validate(final String schemaJson, final String documentJson) {
        return validate(JsonUtils.parseTree(schemaJson), JsonUtils.parseTree(documentJson));
    }

    public static void validateOrThrow(final JsonNode schemaNode, final JsonNode documentNode) {
        final var violations = validate(schemaNode, documentNode);
        if (!violations.isEmpty()) {
            throw new JsonSchemaValidationException("El JSON no cumple el schema", violations);
        }
    }

    public static void validateOrThrow(final String schemaJson, final String documentJson) {
        validateOrThrow(JsonUtils.parseTree(schemaJson), JsonUtils.parseTree(documentJson));
    }

    public static <T> T parseAndValidateOrThrow(final String schemaJson, final String documentJson,
            final TypeReference<T> typeReference) {
        validateOrThrow(schemaJson, documentJson);
        return JsonUtils.fromJson(documentJson, typeReference);
    }

    public static String violationsAsText(final List<String> violations) {
        Objects.requireNonNull(violations, "violations");
        return violations.stream().collect(Collectors.joining("; "));
    }
}
