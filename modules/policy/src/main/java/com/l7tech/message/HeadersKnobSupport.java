package com.l7tech.message;

import com.l7tech.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Supporting implementation of HeadersKnob which stores the headers in a collection.
 */
public class HeadersKnobSupport implements HeadersKnob {
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
        headers.add(new Pair<String, Object>(name, value));
    }

    @Override
    public void removeHeader(@NotNull final String name) {
        final Collection<Pair<String, Object>> toRemove = new ArrayList<>();
        for (final Pair<String, Object> header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                toRemove.add(header);
            }
        }
        headers.removeAll(toRemove);
    }

    @Override
    public void removeHeader(@NotNull final String name, @Nullable final Object value) {
        final Collection<Pair<String, Object>> toRemove = new ArrayList<>();
        for (final Pair<String, Object> header : headers) {
            if (header.getKey().equalsIgnoreCase(name) && ObjectUtils.equals(value, header.getValue())) {
                toRemove.add(header);
            }
        }
        headers.removeAll(toRemove);
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

    private Collection<Pair<String, Object>> headers = new ArrayList<>();
}
