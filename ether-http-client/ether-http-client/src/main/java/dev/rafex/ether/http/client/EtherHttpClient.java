package dev.rafex.ether.http.client;

/*-
 * #%L
 * ether-http-client
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

import java.io.IOException;
import java.net.URI;

import com.fasterxml.jackson.core.type.TypeReference;

import dev.rafex.ether.http.client.model.HttpRequestSpec;
import dev.rafex.ether.http.client.model.HttpResponseSpec;

public interface EtherHttpClient extends AutoCloseable {

    HttpResponseSpec send(HttpRequestSpec request) throws IOException, InterruptedException;

    default HttpResponseSpec get(final URI uri) throws IOException, InterruptedException {
        return send(HttpRequestSpec.get(uri).build());
    }

    <T> T sendJson(HttpRequestSpec request, TypeReference<T> typeReference) throws IOException, InterruptedException;

    <T> T sendJson(HttpRequestSpec request, Class<T> type) throws IOException, InterruptedException;

    @Override
    default void close() throws Exception {
    }
}
