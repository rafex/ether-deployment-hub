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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonSchemaUtilsTest {

    private static final String SCHEMA = """
            {
              \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",
              \"type\": \"object\",
              \"required\": [\"name\", \"age\"],
              \"properties\": {
                \"name\": { \"type\": \"string\" },
                \"age\": { \"type\": \"integer\", \"minimum\": 0 }
              }
            }
            """;

    @Test
    void shouldReturnViolationsWhenInvalid() {
        final var violations = JsonSchemaUtils.validate(SCHEMA, "{\"name\":\"ana\",\"age\":-1}");

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldValidateWithoutViolationsWhenValid() {
        final var violations = JsonSchemaUtils.validate(SCHEMA, "{\"name\":\"ana\",\"age\":1}");

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldThrowTypedValidationException() {
        final var ex = assertThrows(JsonSchemaValidationException.class,
                () -> JsonSchemaUtils.validateOrThrow(SCHEMA, "{\"name\":\"ana\"}"));

        assertFalse(ex.violations().isEmpty());
        assertEquals("El JSON no cumple el schema", ex.getMessage());
    }
}
