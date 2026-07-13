package dev.rafex.ether.config.validation;

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

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

import dev.rafex.ether.config.exceptions.ConfigValidationException;

public final class ConfigValidator {

    private ConfigValidator() {
    }

    private static final int MAX_DEPTH = 20;

    public static void validate(final Object instance) {
        Objects.requireNonNull(instance, "instance");
        final List<ConfigViolation> violations = new ArrayList<>();
        validateInstance(instance, "", violations, 0);
        if (!violations.isEmpty()) {
            throw new ConfigValidationException(violations);
        }
    }

    private static void validateInstance(final Object instance, final String path,
            final List<ConfigViolation> violations, final int depth) {
        if (instance == null || !instance.getClass().isRecord()) {
            return;
        }

        for (final RecordComponent component : instance.getClass().getRecordComponents()) {
            final var componentPath = path.isBlank() ? component.getName() : path + "." + component.getName();
            final Object value;
            try {
                final var accessor = component.getAccessor();
                accessor.setAccessible(true);
                value = accessor.invoke(instance);
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to validate component " + componentPath, e);
            }

            if (component.isAnnotationPresent(Required.class) && value == null) {
                violations.add(new ConfigViolation(componentPath, "is required"));
                continue;
            }
            if (value == null) {
                continue;
            }

            final var notBlank = component.getAnnotation(NotBlank.class);
            if (notBlank != null && value instanceof final String stringValue && stringValue.isBlank()) {
                violations.add(new ConfigViolation(componentPath, "must not be blank"));
            }

            final var min = component.getAnnotation(Min.class);
            if (min != null && value instanceof final Number number && number.longValue() < min.value()) {
                violations.add(new ConfigViolation(componentPath, "must be >= " + min.value()));
            }

            final var max = component.getAnnotation(Max.class);
            if (max != null && value instanceof final Number number && number.longValue() > max.value()) {
                violations.add(new ConfigViolation(componentPath, "must be <= " + max.value()));
            }

            final var pattern = component.getAnnotation(Pattern.class);
            if (pattern != null && value instanceof final String stringValue) {
                try {
                    if (!java.util.regex.Pattern.compile(pattern.value()).matcher(stringValue).matches()) {
                        violations.add(new ConfigViolation(componentPath, "must match " + pattern.value()));
                    }
                } catch (final PatternSyntaxException e) {
                    throw new IllegalArgumentException("Invalid regex on " + componentPath + ": " + pattern.value(), e);
                }
            }

            final var size = component.getAnnotation(Size.class);
            if (size != null) {
                final var actualSize = sizeOf(value);
                if (actualSize >= 0 && (actualSize < size.min() || actualSize > size.max())) {
                    violations.add(new ConfigViolation(componentPath,
                            "size must be between " + size.min() + " and " + size.max()));
                }
            }

            if (component.isAnnotationPresent(Valid.class)) {
                validateNested(value, componentPath, violations, depth + 1);
            }
        }
    }

    private static void validateNested(final Object value, final String path, final List<ConfigViolation> violations,
            final int depth) {
        if (depth >= MAX_DEPTH) {
            return;
        }
        if (value == null) {
            return;
        }
        if (value.getClass().isRecord()) {
            validateInstance(value, path, violations, depth + 1);
            return;
        }
        if (value instanceof final Collection<?> collection) {
            var index = 0;
            for (final Object element : collection) {
                validateNested(element, path + "[" + index + "]", violations, depth + 1);
                index++;
            }
            return;
        }
        if (value instanceof final Map<?, ?> map) {
            for (final var entry : map.entrySet()) {
                validateNested(entry.getValue(), path + "." + entry.getKey(), violations, depth + 1);
            }
        }
    }

    private static int sizeOf(final Object value) {
        return switch (value) {
        case final String stringValue -> stringValue.length();
        case final Collection<?> collection -> collection.size();
        case final Map<?, ?> map -> map.size();
        case null, default -> -1;
        };
    }
}
