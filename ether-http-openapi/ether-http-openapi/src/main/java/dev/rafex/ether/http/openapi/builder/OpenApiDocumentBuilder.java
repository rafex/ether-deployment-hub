package dev.rafex.ether.http.openapi.builder;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.rafex.ether.http.openapi.model.OpenApiComponents;
import dev.rafex.ether.http.openapi.model.OpenApiDocument;
import dev.rafex.ether.http.openapi.model.OpenApiInfo;
import dev.rafex.ether.http.openapi.model.OpenApiOperation;
import dev.rafex.ether.http.openapi.model.OpenApiPathItem;
import dev.rafex.ether.http.openapi.model.OpenApiResponse;
import dev.rafex.ether.http.openapi.model.OpenApiSchema;
import dev.rafex.ether.http.openapi.model.OpenApiServer;

public final class OpenApiDocumentBuilder {

    private String openapi = "3.1.0";
    private OpenApiInfo info = new OpenApiInfo("ether-api", "0.0.0", null, null, null);
    private final List<OpenApiServer> servers = new ArrayList<>();
    private final Map<String, OpenApiPathItem> paths = new LinkedHashMap<>();
    private final Map<String, OpenApiSchema> schemas = new LinkedHashMap<>();
    private final Map<String, OpenApiResponse> responses = new LinkedHashMap<>();

    public static OpenApiDocumentBuilder create() {
        return new OpenApiDocumentBuilder();
    }

    public OpenApiDocumentBuilder openapi(final String openapi) {
        this.openapi = Objects.requireNonNull(openapi, "openapi");
        return this;
    }

    public OpenApiDocumentBuilder info(final OpenApiInfo info) {
        this.info = Objects.requireNonNull(info, "info");
        return this;
    }

    public OpenApiDocumentBuilder addServer(final OpenApiServer server) {
        servers.add(Objects.requireNonNull(server, "server"));
        return this;
    }

    public OpenApiDocumentBuilder addSchema(final String name, final OpenApiSchema schema) {
        schemas.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(schema, "schema"));
        return this;
    }

    public OpenApiDocumentBuilder addResponse(final String name, final OpenApiResponse response) {
        responses.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(response, "response"));
        return this;
    }

    public OpenApiDocumentBuilder addOperation(final String path, final String method,
            final OpenApiOperation operation) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(operation, "operation");
        paths.compute(path,
                (k, current) -> (current == null ? OpenApiPathItem.empty() : current).withOperation(method, operation));
        return this;
    }

    public OpenApiDocument build() {
        return new OpenApiDocument(openapi, info, servers, paths, new OpenApiComponents(schemas, responses));
    }
}
