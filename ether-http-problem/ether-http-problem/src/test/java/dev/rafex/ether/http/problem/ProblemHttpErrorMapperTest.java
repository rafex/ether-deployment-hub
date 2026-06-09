package dev.rafex.ether.http.problem;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.http.problem.exception.ProblemException;
import dev.rafex.ether.http.problem.mapper.ProblemHttpErrorMapper;
import dev.rafex.ether.http.problem.model.ProblemDetails;

class ProblemHttpErrorMapperTest {

    @Test
    void shouldMapProblemExceptionToHttpError() {
        final var mapper = new ProblemHttpErrorMapper();
        final var problem = ProblemDetails.builder().status(422).title("Validation failed").detail("name is required")
                .property("code", "validation_failed").build();

        final var mapped = mapper.map(new ProblemException(problem));
        assertEquals(422, mapped.status());
        assertEquals("validation_failed", mapped.code());
        assertEquals("name is required", mapped.message());
    }
}
