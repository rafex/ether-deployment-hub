package dev.rafex.ether.http.openapi;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.http.openapi.builder.OpenApiDocumentBuilder;
import dev.rafex.ether.http.openapi.model.OpenApiInfo;
import dev.rafex.ether.http.openapi.model.OpenApiOperation;
import dev.rafex.ether.http.openapi.model.OpenApiResponse;
import dev.rafex.ether.http.openapi.model.OpenApiSchema;
import dev.rafex.ether.http.openapi.model.OpenApiServer;

class OpenApiDocumentBuilderTest {

    @Test
    void builderShouldAccumulateOperationsAndRenderJson() {
        final var doc = OpenApiDocumentBuilder.create().info(new OpenApiInfo("Ether API", "1.0.0", "Demo", null, null))
                .addServer(new OpenApiServer("https://api.example.com", "production"))
                .addSchema("User", OpenApiSchema.stringSchema())
                .addResponse("Ok", OpenApiResponse.ok("success", OpenApiSchema.stringSchema()))
                .addOperation("/users", "get", OpenApiOperation.of("listUsers", "List users")).build();

        assertEquals("3.1.0", doc.openapi());
        assertTrue(doc.paths().containsKey("/users"));
        assertEquals("List users", doc.paths().get("/users").operations().get("get").summary());
        assertTrue(doc.toJson().contains("\"openapi\":\"3.1.0\""));
    }

    @Test
    void schemaShouldPreserveRequiredAndPropertiesCopies() {
        final var schema = new OpenApiSchema("object", null, null, false, null, null,
                java.util.Map.of("id", OpenApiSchema.integerSchema()), java.util.List.of("id"), null);

        assertEquals(1, schema.properties().size());
        assertEquals(1, schema.required().size());
    }
}
