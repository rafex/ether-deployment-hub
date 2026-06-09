package dev.rafex.ether.glowroot.jetty12;

/*-
 * #%L
 * ether-glowroot-jetty12
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

import java.util.regex.Pattern;

/**
 * Clase utilitaria para normalizar rutas de HTTP eliminando valores dinámicos (IDs, UUIDs, etc).
 * <p>
 * Esta clase se utiliza en el contexto de Glowroot para que las rutas similares se agrupen
 * en el APM, evitando que cada ID diferente cree una métrica separada.
 * </p>
 */
final class PathNormalizer {

    private static final Pattern UUID_PATTERN = Pattern
            .compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern OBJECTID_PATTERN = Pattern.compile("/[0-9a-fA-F]{24}(?=/|$)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("/\\d{2,}(?=/|$)");
    private static final Pattern MULTIPLE_SLASHES_PATTERN = Pattern.compile("/{2,}");

    private PathNormalizer() {
    }

    static String normalize(final String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        String result = MULTIPLE_SLASHES_PATTERN.matcher(path).replaceAll("/");
        result = UUID_PATTERN.matcher(result).replaceAll("/:id");
        result = OBJECTID_PATTERN.matcher(result).replaceAll("/:id");
        return NUMBER_PATTERN.matcher(result).replaceAll("/:n");
    }
}
