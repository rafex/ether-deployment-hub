package dev.rafex.ether.http.problem.mapper;

/*-
 * #%L
 * ether-http-problem
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

import java.util.Objects;

import dev.rafex.ether.http.core.DefaultErrorMapper;
import dev.rafex.ether.http.core.ErrorMapper;
import dev.rafex.ether.http.core.HttpError;
import dev.rafex.ether.http.problem.exception.ProblemException;
import dev.rafex.ether.http.problem.model.ProblemDetails;

public final class ProblemHttpErrorMapper implements ErrorMapper {

    private final ErrorMapper fallback;

    public ProblemHttpErrorMapper() {
        this(new DefaultErrorMapper());
    }

    public ProblemHttpErrorMapper(final ErrorMapper fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public HttpError map(final Throwable error) {
        return switch (error) {
            case ProblemException pe when pe.problem() != null -> {
                final var problem = pe.problem();
                yield new HttpError(problem.status(), problemCode(problem), safe(problem.detail(), problem.title()));
            }
            default -> fallback.map(error);
        };
    }

    private static String problemCode(final ProblemDetails problem) {
        return switch (problem.properties().get("code")) {
            case String value when !value.isBlank() -> value;
            default -> "problem";
        };
    }

    private static String safe(final String detail, final String fallback) {
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return fallback == null || fallback.isBlank() ? "problem" : fallback;
    }
}
