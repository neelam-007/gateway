package com.l7tech.message;

import com.l7tech.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supporting implementation of HeadersKnob which stores the headers in a collection.
 */
public class HeadersKnobSupport implements HeadersKnob {

    public static final String VALUE_SEPARATOR = ",";

    @Override
    public String[] getHeaderValues(@NotNull final String name) {
        final Collection<String> valuesAsString = new ArrayList<>();
        for (final Pair<String, Object> header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                valuesAsString.add(header.getValue() == null ? null : header.getValue().toString());
            }
        }
        return valuesAsString.toArray(new String[valuesAsString.size()]);
    }

    @Override
    public String[] getHeaderNames() {
        final Collection<String> names = new HashSet<>();
        for (final Pair<String, Object> header : headers) {
            names.add(header.getKey());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public void setHeader(@NotNull final String name, @Nullable final Object value) {
        removeHeader(name);
        addHeader(name, value);
    }

    @Override
    public void addHeader(@NotNull final String name, @Nullable final Object value) {
        headers.add(new Pair(name, value));
    }

    @Override
    public void removeHeader(@NotNull final String name) {
        removeHeader(name, false);
    }

    @Override
    public void removeHeader(@NotNull String name, boolean caseSensitive) {
        final Collection<Pair<String, Object>> toRemove = new ArrayList<>();
        for (final Pair<String, Object> header : headers) {
            if (nameMatches(name, caseSensitive, header.getKey())) {
                toRemove.add(header);
            }
        }
        if (toRemove.isEmpty()) {
            logger.log(Level.FINE, "No header found with name: " + name);
        }
        headers.removeAll(toRemove);
    }

    @Override
    public void removeHeader(@NotNull final String name, @Nullable final Object value) {
        removeHeader(name, value, false);
    }

    @Override
    public void removeHeader(@NotNull String name, @Nullable Object value, boolean caseSensitive) {
        final Collection<Pair<String, Object>> toRemove = new ArrayList<>();
        final Collection<Pair<String, Object>> replacements = new ArrayList<>();
        for (final Pair<String, Object> header : headers) {
            if ((nameMatches(name, caseSensitive, header.getKey()))) {
                if (header.getValue() instanceof String && ((String) header.getValue()).contains(VALUE_SEPARATOR)) {
                    // handle comma-separated multiple values
                    final List<String> subValues = new ArrayList<>();
                    for (final String token : StringUtils.split((String) header.getValue(), VALUE_SEPARATOR)) {
                        subValues.add(token.trim());
                    }
                    final List<String> subValuesToRemove = new ArrayList<>();
                    for (final String subValue : subValues) {
                        if (value.equals(subValue)) {
                            subValuesToRemove.add(subValue);
                        }
                    }
                    subValues.removeAll(subValuesToRemove);
                    toRemove.add(header);
                    replacements.add(new Pair<String, Object>(header.getKey(), StringUtils.join(subValues, VALUE_SEPARATOR)));
                } else if (ObjectUtils.equals(value, header.getValue())) {
                    toRemove.add(header);
                }
            }
        }
        if (toRemove.isEmpty()) {
            logger.log(Level.FINE, "No header found with name: " + name + " and value: " + value);
        }
        headers.removeAll(toRemove);
        headers.addAll(replacements);
    }

    @Override
    public boolean containsHeader(@NotNull final String name) {
        for (final Pair<String, Object> header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Pair<String, Object>> getHeaders() {
        return Collections.unmodifiableCollection(headers);
    }

    private boolean nameMatches(final String toMatch, final boolean caseSensitive, final String headerName) {
        return (!caseSensitive && headerName.equalsIgnoreCase(toMatch)) ||
                (caseSensitive && headerName.equals(toMatch));
    }

    private Collection<Pair<String, Object>> headers = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(HeadersKnobSupport.class.getName());
}
