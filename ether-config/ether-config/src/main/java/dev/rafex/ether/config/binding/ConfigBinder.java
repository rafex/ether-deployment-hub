package dev.rafex.ether.config.binding;

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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.annotations.ConfigAlias;
import dev.rafex.ether.config.annotations.ConfigPrefix;
import dev.rafex.ether.config.sources.MapConfigSource;
import dev.rafex.ether.config.validation.ConfigValidator;

public final class ConfigBinder {

    private static final Pattern INDEXED_KEY = Pattern.compile("^(.+)\\[(\\d+)](?:\\.|$)");

    private ConfigBinder() {
    }

    public static <T extends Record> T bind(final EtherConfig config, final Class<T> recordType) {
        return bindInternal(config, resolvePrefix("", recordType), recordType);
    }

    public static <T extends Record> T bind(final EtherConfig config, final String prefix, final Class<T> recordType) {
        return bindInternal(config, resolvePrefix(prefix, recordType), recordType);
    }

    public static <T extends Record> T bindValidated(final EtherConfig config, final Class<T> recordType) {
        final var bound = bind(config, recordType);
        ConfigValidator.validate(bound);
        return bound;
    }

    public static <T extends Record> T bindValidated(final EtherConfig config, final String prefix,
            final Class<T> recordType) {
        final var bound = bind(config, prefix, recordType);
        ConfigValidator.validate(bound);
        return bound;
    }

    private static <T extends Record> T bindInternal(final EtherConfig config, final String prefix,
            final Class<T> recordType) {
        if (!recordType.isRecord()) {
            throw new IllegalArgumentException("Only record types are supported: " + recordType.getName());
        }

        try {
            final var components = recordType.getRecordComponents();
            final var argTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            final var constructor = recordType.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            final var args = new Object[components.length];
            final var snapshot = config.snapshot();

            for (var i = 0; i < components.length; i++) {
                final var component = components[i];
                final var key = join(prefix, componentName(component));
                args[i] = bindValue(snapshot, key, component.getType(), component.getGenericType(), true);
            }
            return constructor.newInstance(args);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to bind config to record " + recordType.getName(), e);
        }
    }

    private static Object bindValue(final Map<String, String> snapshot, final String key, final Class<?> rawType,
            final Type genericType, final boolean required) {
        if (rawType.isRecord()) {
            if (!hasNestedKeys(snapshot, key)) {
                if (required) {
                    throw new IllegalArgumentException("Missing config key: " + key);
                }
                return null;
            }
            return bindInternal(EtherConfig.of(new MapConfigSource("nested", snapshot)), key,
                    rawType.asSubclass(Record.class));
        }

        if (List.class.isAssignableFrom(rawType)) {
            return bindList(snapshot, key, genericType, required);
        }

        if (Map.class.isAssignableFrom(rawType)) {
            return bindMap(snapshot, key, genericType, required);
        }

        final var raw = snapshot.get(key);
        if (raw == null) {
            if (required) {
                throw new IllegalArgumentException("Missing config key: " + key);
            }
            return null;
        }
        return convert(raw, rawType);
    }

    private static Object bindList(final Map<String, String> snapshot, final String key, final Type genericType,
            final boolean required) {
        final Class<?> itemType = firstTypeArgument(genericType);
        final var directValues = directIndexedValues(snapshot, key);
        if (!directValues.isEmpty()) {
            final List<Object> result = new ArrayList<>(directValues.size());
            for (final String value : directValues) {
                result.add(convert(value, itemType));
            }
            return List.copyOf(result);
        }

        if (snapshot.containsKey(key)) {
            final var raw = snapshot.get(key);
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            final List<Object> result = new ArrayList<>();
            for (final String part : raw.split(",")) {
                result.add(convert(part.trim(), itemType));
            }
            return List.copyOf(result);
        }

        if (itemType.isRecord()) {
            final var indices = nestedListIndices(snapshot, key);
            if (!indices.isEmpty()) {
                final List<Object> result = new ArrayList<>(indices.size());
                for (final Integer index : indices) {
                    result.add(bindInternal(EtherConfig.of(new MapConfigSource("nested", snapshot)),
                            key + "[" + index + "]", itemType.asSubclass(Record.class)));
                }
                return List.copyOf(result);
            }
        }

        return List.of();
    }

    private static Object bindMap(final Map<String, String> snapshot, final String key, final Type genericType,
            final boolean required) {
        final Class<?> keyType = firstTypeArgument(genericType);
        final Class<?> valueType = secondTypeArgument(genericType);
        if (keyType != String.class) {
            throw new IllegalArgumentException("Only Map<String, ?> is supported: " + key);
        }

        final Map<String, Object> result = new LinkedHashMap<>();
        final var entries = mapEntries(snapshot, key);
        for (final String entry : entries) {
            final var entryPrefix = key + "." + entry;
            if (valueType.isRecord()) {
                result.put(entry, bindInternal(EtherConfig.of(new MapConfigSource("nested", snapshot)), entryPrefix,
                        valueType.asSubclass(Record.class)));
            } else {
                final var raw = snapshot.get(entryPrefix);
                if (raw != null) {
                    result.put(entry, convert(raw, valueType));
                }
            }
        }

        return Map.copyOf(result);
    }

    private static List<String> directIndexedValues(final Map<String, String> snapshot, final String key) {
        final Map<Integer, String> values = new LinkedHashMap<>();
        for (final var entry : snapshot.entrySet()) {
            final var matcher = INDEXED_KEY.matcher(entry.getKey());
            if (matcher.matches() && matcher.group(1).equals(key)) {
                values.put(Integer.parseInt(matcher.group(2)), entry.getValue());
            }
        }

        if (values.isEmpty()) {
            return List.of();
        }

        final List<String> ordered = new ArrayList<>(values.size());
        for (var i = 0; i < values.size(); i++) {
            if (!values.containsKey(i)) {
                throw new IllegalArgumentException("List indices must be contiguous for key: " + key);
            }
            ordered.add(values.get(i));
        }
        return ordered;
    }

    private static Set<Integer> nestedListIndices(final Map<String, String> snapshot, final String key) {
        final Set<Integer> indices = new TreeSet<>();
        final var prefix = key + "[";
        for (final String candidate : snapshot.keySet()) {
            if (candidate.startsWith(prefix)) {
                final var close = candidate.indexOf(']', prefix.length());
                if (close > 0) {
                    indices.add(Integer.parseInt(candidate.substring(prefix.length(), close)));
                }
            }
        }
        return indices;
    }

    private static Set<String> mapEntries(final Map<String, String> snapshot, final String key) {
        final Set<String> entries = new LinkedHashSet<>();
        final var prefix = key + ".";
        for (final String candidate : snapshot.keySet()) {
            if (candidate.startsWith(prefix)) {
                final var remainder = candidate.substring(prefix.length());
                final var dotIndex = remainder.indexOf('.');
                entries.add(dotIndex >= 0 ? remainder.substring(0, dotIndex) : remainder);
            }
        }
        return entries;
    }

    private static boolean hasNestedKeys(final Map<String, String> snapshot, final String key) {
        final var prefix = key + ".";
        final var listPrefix = key + "[";
        for (final String candidate : snapshot.keySet()) {
            if (candidate.equals(key) || candidate.startsWith(prefix) || candidate.startsWith(listPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static String join(final String prefix, final String name) {
        return prefix == null || prefix.isBlank() ? name : prefix + "." + name;
    }

    private static String resolvePrefix(final String explicitPrefix, final Class<?> recordType) {
        final var annotation = recordType.getAnnotation(ConfigPrefix.class);
        if (annotation == null || annotation.value().isBlank()) {
            return explicitPrefix == null ? "" : explicitPrefix;
        }
        if (explicitPrefix == null || explicitPrefix.isBlank()) {
            return annotation.value();
        }
        return explicitPrefix + "." + annotation.value();
    }

    private static String componentName(final RecordComponent component) {
        final var alias = component.getAnnotation(ConfigAlias.class);
        if (alias != null && !alias.value().isBlank()) {
            return alias.value();
        }
        return component.getName();
    }

    private static Class<?> firstTypeArgument(final Type genericType) {
        if (genericType instanceof final ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getActualTypeArguments()[0]);
        }
        throw new IllegalArgumentException("Generic type information is required");
    }

    private static Class<?> secondTypeArgument(final Type genericType) {
        if (genericType instanceof final ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getActualTypeArguments()[1]);
        }
        throw new IllegalArgumentException("Generic type information is required");
    }

    private static Class<?> rawClass(final Type type) {
        if (type instanceof final Class<?> clazz) {
            return clazz;
        }
        if (type instanceof final ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }
        throw new IllegalArgumentException("Unsupported generic type: " + type.getTypeName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object convert(final String raw, final Class<?> targetType) {
        if (targetType == String.class) {
            return raw;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(raw);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(raw);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(raw);
        }
        if (targetType == Duration.class) {
            return Duration.parse(raw);
        }
        if (targetType == URI.class) {
            return URI.create(raw);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class),
                    raw.toUpperCase(Locale.ROOT));
        }
        throw new IllegalArgumentException("Unsupported config target type: " + targetType.getName());
    }
}
