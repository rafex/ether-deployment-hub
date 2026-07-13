package dev.rafex.ether.config;

/*-
 * #%L
 * ether-config
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rafex.ether.config.exceptions.ConfigValidationException;
import dev.rafex.ether.config.validation.ConfigValidator;
import dev.rafex.ether.config.validation.Max;
import dev.rafex.ether.config.validation.Min;
import dev.rafex.ether.config.validation.NotBlank;
import dev.rafex.ether.config.validation.Pattern;
import dev.rafex.ether.config.validation.Required;
import dev.rafex.ether.config.validation.Size;
import dev.rafex.ether.config.validation.Valid;

class ConfigValidatorTest {

    record RequiredConfig(@Required String token) {
    }

    record NotBlankConfig(@NotBlank String host) {
    }

    record MinConfig(@Min(1) int port) {
    }

    record MaxConfig(@Max(100) int workers) {
    }

    record PatternConfig(@Pattern("^https?://.+") String endpoint) {
    }

    record BadPatternConfig(@Pattern("[invalid") String value) {
    }

    record SizeListConfig(@Size(min = 2) List<String> hosts) {
    }

    record SizeStringConfig(@Size(max = 5) String code) {
    }

    record Inner(@NotBlank String name) {
    }

    record OuterConfig(@Valid Inner inner) {
    }

    record ListWrapper(@Valid List<Inner> items) {
    }

    record MultiConfig(@Required String a, @Min(1) int b) {
    }

    static final class NotARecord {
        @SuppressWarnings("unused")
        String field = "";
    }

    // ── @Required ─────────────────────────────────────────────────────────────

    @Test
    void requiredFieldPresentPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new RequiredConfig("abc")));
    }

    @Test
    void requiredFieldNullThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new RequiredConfig(null)));
        assertTrue(ex.getMessage().contains("token"));
        assertTrue(ex.getMessage().contains("required"));
    }

    // ── @NotBlank ───────────────────────────────────────────────────────────

    @Test
    void notBlankWithValuePasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new NotBlankConfig("localhost")));
    }

    @Test
    void notBlankWhenBlankThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new NotBlankConfig("   ")));
        assertTrue(ex.getMessage().contains("must not be blank"));
    }

    // ── @Min ──────────────────────────────────────────────────────────────────

    @Test
    void minAtBoundaryPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new MinConfig(1)));
    }

    @Test
    void minBelowThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new MinConfig(0)));
        assertTrue(ex.getMessage().contains(">= 1"));
    }

    // ── @Max ──────────────────────────────────────────────────────────────────

    @Test
    void maxAtBoundaryPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new MaxConfig(100)));
    }

    @Test
    void maxAboveThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new MaxConfig(101)));
        assertTrue(ex.getMessage().contains("<= 100"));
    }

    // ── @Pattern ────────────────────────────────────────────────────────────

    @Test
    void patternMatchPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new PatternConfig("https://ok.example.com")));
    }

    @Test
    void patternMismatchThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new PatternConfig("ftp://bad")));
        assertTrue(ex.getMessage().contains("must match"));
    }

    @Test
    void invalidRegexThrowsIllegalArgument() {
        final var ex = assertThrows(IllegalArgumentException.class,
                () -> ConfigValidator.validate(new BadPatternConfig("anything")));
        assertTrue(ex.getMessage().contains("Invalid regex"));
    }

    // ── @Size ─────────────────────────────────────────────────────────────────

    @Test
    void sizeListBelowMinThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new SizeListConfig(List.of("only-one"))));
        assertTrue(ex.getMessage().contains("size must be between"));
    }

    @Test
    void sizeListAtMinPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new SizeListConfig(List.of("a", "b"))));
    }

    @Test
    void sizeStringAboveMaxThrows() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new SizeStringConfig("too-long")));
        assertTrue(ex.getMessage().contains("size must be between"));
    }

    // ── @Valid (recursive) ────────────────────────────────────────────────────

    @Test
    void validNestedRecordValidated() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new OuterConfig(new Inner(""))));
        assertTrue(ex.getMessage().contains("inner.name"));
    }

    @Test
    void validNestedRecordPasses() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new OuterConfig(new Inner("ok"))));
    }

    @Test
    void validListElementsValidated() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new ListWrapper(List.of(new Inner("ok"), new Inner("")))));
        assertTrue(ex.getMessage().contains("items[1].name"));
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    void nullInstanceThrowsNpe() {
        assertThrows(NullPointerException.class, () -> ConfigValidator.validate(null));
    }

    @Test
    void nonRecordInstanceIsSkipped() {
        assertDoesNotThrow(() -> ConfigValidator.validate(new NotARecord()));
    }

    @Test
    void multipleViolationsCollected() {
        final var ex = assertThrows(ConfigValidationException.class,
                () -> ConfigValidator.validate(new MultiConfig(null, 0)));
        assertEquals(2, ex.violations().size());
    }
}
