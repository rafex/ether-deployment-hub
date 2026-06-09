package dev.rafex.ether.http.openapi.model;

/*-
 * #%L
 * ether-http-openapi
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

import java.util.List;
import java.util.Map;

public record OpenApiSchema(String type, String format, String description, boolean nullable, Object example,
        String ref, Map<String, OpenApiSchema> properties, List<String> required, OpenApiSchema items) {

    public OpenApiSchema {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        required = required == null ? List.of() : List.copyOf(required);
    }

    public static OpenApiSchema stringSchema() {
        return new OpenApiSchema("string", null, null, false, null, null, Map.of(), List.of(), null);
    }

    public static OpenApiSchema integerSchema() {
        return new OpenApiSchema("integer", "int32", null, false, null, null, Map.of(), List.of(), null);
    }
}
