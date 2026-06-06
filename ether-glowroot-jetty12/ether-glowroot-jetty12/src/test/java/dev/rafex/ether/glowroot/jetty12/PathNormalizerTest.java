package dev.rafex.ether.glowroot.jetty12;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PathNormalizer")
class PathNormalizerTest {

    /* ── edge cases ── */

    @Test
    @DisplayName("null path → 'unknown'")
    void nullPath() {
        assertEquals("unknown", PathNormalizer.normalize(null));
    }

    @Test
    @DisplayName("empty path → 'unknown'")
    void emptyPath() {
        assertEquals("unknown", PathNormalizer.normalize(""));
    }

    /* ── no substitution needed ── */

    @Test
    @DisplayName("plain path is returned unchanged")
    void plainPath() {
        assertEquals("/users", PathNormalizer.normalize("/users"));
    }

    @Test
    @DisplayName("nested plain path is returned unchanged")
    void nestedPlainPath() {
        assertEquals("/api/v1/products", PathNormalizer.normalize("/api/v1/products"));
    }

    @Test
    @DisplayName("root path is returned unchanged")
    void rootPath() {
        assertEquals("/", PathNormalizer.normalize("/"));
    }

    /* ── UUID substitution ── */

    @Test
    @DisplayName("UUID at end of path → /:id")
    void uuidAtEnd() {
        assertEquals("/users/:id", PathNormalizer.normalize("/users/550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("UUID in the middle of path → /:id")
    void uuidInMiddle() {
        assertEquals("/users/:id/orders",
                PathNormalizer.normalize("/users/550e8400-e29b-41d4-a716-446655440000/orders"));
    }

    @Test
    @DisplayName("multiple UUIDs in path → multiple /:id")
    void multipleUuids() {
        assertEquals("/users/:id/orders/:id", PathNormalizer
                .normalize("/users/550e8400-e29b-41d4-a716-446655440000/orders/123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    @DisplayName("uppercase UUID → /:id")
    void uppercaseUuid() {
        assertEquals("/items/:id", PathNormalizer.normalize("/items/550E8400-E29B-41D4-A716-446655440000"));
    }

    /* ── ObjectId substitution (24-hex) ── */

    @Test
    @DisplayName("MongoDB ObjectId at end → /:id")
    void objectIdAtEnd() {
        assertEquals("/items/:id", PathNormalizer.normalize("/items/507f1f77bcf86cd799439011"));
    }

    @Test
    @DisplayName("ObjectId in middle → /:id")
    void objectIdInMiddle() {
        assertEquals("/items/:id/reviews", PathNormalizer.normalize("/items/507f1f77bcf86cd799439011/reviews"));
    }

    /* ── numeric substitution ── */

    @Test
    @DisplayName("two-digit number → /:n")
    void twoDigitNumber() {
        assertEquals("/page/:n", PathNormalizer.normalize("/page/42"));
    }

    @Test
    @DisplayName("large number → /:n")
    void largeNumber() {
        assertEquals("/orders/:n", PathNormalizer.normalize("/orders/1234567890"));
    }

    @Test
    @DisplayName("single-digit number is NOT replaced")
    void singleDigitNotReplaced() {
        assertEquals("/api/v1", PathNormalizer.normalize("/api/v1"));
    }

    @Test
    @DisplayName("version prefix with letter is NOT replaced")
    void versionWithLetterNotReplaced() {
        assertEquals("/api/v2/items", PathNormalizer.normalize("/api/v2/items"));
    }

    /* ── multiple slash collapsing ── */

    @Test
    @DisplayName("consecutive slashes are collapsed")
    void consecutiveSlashes() {
        assertEquals("/foo/bar", PathNormalizer.normalize("//foo///bar"));
    }

    @Test
    @DisplayName("trailing double slash is collapsed")
    void trailingDoubleSlash() {
        assertEquals("/foo/", PathNormalizer.normalize("/foo//"));
    }

    /* ── combined ── */

    @Test
    @DisplayName("UUID + number in same path")
    void uuidAndNumber() {
        assertEquals("/users/:id/page/:n",
                PathNormalizer.normalize("/users/550e8400-e29b-41d4-a716-446655440000/page/10"));
    }

    @Test
    @DisplayName("double slash before UUID is collapsed then replaced")
    void doubleSlashBeforeUuid() {
        assertEquals("/users/:id", PathNormalizer.normalize("//users/550e8400-e29b-41d4-a716-446655440000"));
    }
}
