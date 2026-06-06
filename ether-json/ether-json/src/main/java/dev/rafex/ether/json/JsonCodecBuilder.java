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

import java.util.Objects;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonCodecBuilder {

    private final ObjectMapper mapper;

    private JsonCodecBuilder(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static JsonCodecBuilder create() {
        return new JsonCodecBuilder(JacksonJsonCodec.defaultMapper());
    }

    public static JsonCodecBuilder strict() {
        return create().failOnUnknownProperties(true);
    }

    public static JsonCodecBuilder lenient() {
        return create().failOnUnknownProperties(false);
    }

    public JsonCodecBuilder configure(final Consumer<ObjectMapper> customizer) {
        Objects.requireNonNull(customizer, "customizer").accept(mapper);
        return this;
    }

    public JsonCodecBuilder failOnUnknownProperties(final boolean enabled) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, enabled);
        return this;
    }

    public JsonCodecBuilder writeDatesAsTimestamps(final boolean enabled) {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, enabled);
        return this;
    }

    public JsonCodecBuilder prettyPrint(final boolean enabled) {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, enabled);
        return this;
    }

    public JsonCodecBuilder namingStrategy(final PropertyNamingStrategy strategy) {
        mapper.setPropertyNamingStrategy(strategy);
        return this;
    }

    public JsonCodecBuilder include(final JsonInclude.Include include) {
        mapper.setSerializationInclusion(include);
        return this;
    }

    public JsonCodecBuilder registerModule(final Module module) {
        mapper.registerModule(Objects.requireNonNull(module, "module"));
        return this;
    }

    public JacksonJsonCodec build() {
        return new JacksonJsonCodec(mapper.copy());
    }
}
