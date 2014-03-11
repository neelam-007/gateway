package com.l7tech.message;

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
        return getHeaderValues(name, true);
    }

    @Override
    public String[] getHeaderNames() {
        return getHeaderNames(true, false);
    }

    @Override
    public void setHeader(@NotNull final String name, @Nullable final Object value) {
        setHeader(name, value, true);
    }

    @Override
    public void setHeader(@NotNull String name, @Nullable Object value, boolean passThrough) {
        removeHeader(name);
        addHeader(name, value, passThrough);
    }

    @Override
    public void addHeader(@NotNull final String name, @Nullable final Object value) {
        addHeader(name, value, true);
    }

    @Override
    public void addHeader(@NotNull String name, @Nullable Object value, boolean passThrough) {
        headers.add(new Header(name, value, passThrough));
    }

    @Override
    public void removeHeader(@NotNull final String name) {
        removeHeader(name, false);
    }

    @Override
    public void removeHeader(@NotNull String name, boolean caseSensitive) {
        final Collection<Header> toRemove = new ArrayList<>();
        for (final Header header : headers) {
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
        final Collection<Header> toRemove = new ArrayList<>();
        final Collection<Header> replacements = new ArrayList<>();
        for (final Header header : headers) {
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
                    replacements.add(new Header(header.getKey(), StringUtils.join(subValues, VALUE_SEPARATOR)));
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
        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Header> getHeaders() {
        return getHeaders(true);
    }

    @Override
    public Collection<Header> getHeaders(final boolean includeNonPassThrough) {
        final Collection<Header> ret;
        if (includeNonPassThrough) {
            ret = Collections.unmodifiableCollection(headers);
        } else {
            final Collection<Header> passThroughHeaders = new ArrayList<>();
            for (final Header header : headers) {
                if (header.isPassThrough()) {
                    passThroughHeaders.add(header);
                }
            }
            ret = Collections.unmodifiableCollection(passThroughHeaders);
        }
        return ret;
    }

    @Override
    public Collection<Header> getHeaders(@NotNull final String name) {
        return getHeaders(name, true);
    }

    @Override
    public Collection<Header> getHeaders(@NotNull final String name, final boolean includeNonPassThrough) {
        final Collection<Header> ret = new ArrayList<>();
        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name) && (header.isPassThrough() || includeNonPassThrough)) {
                ret.add(header);
            }
        }
        return Collections.unmodifiableCollection(ret);
    }

    @Override
    public String[] getHeaderValues(@NotNull final String name, final boolean includeNonPassThrough) {
        final Collection<String> valuesAsString = new ArrayList<>();
        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name) && (header.isPassThrough() || includeNonPassThrough)) {
                valuesAsString.add(header.getValue() == null ? null : header.getValue().toString());
            }
        }
        return valuesAsString.toArray(new String[valuesAsString.size()]);
    }

    @Override
    public String[] getHeaderNames(final boolean includeNonPassThrough, final boolean caseSensitive) {
        final Collection<String> names = caseSensitive ? new TreeSet<String>() : new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final Header header : headers) {
            if (header.isPassThrough() || includeNonPassThrough) {
                names.add(header.getKey());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private boolean nameMatches(final String toMatch, final boolean caseSensitive, final String headerName) {
        return (!caseSensitive && headerName.equalsIgnoreCase(toMatch)) ||
                (caseSensitive && headerName.equals(toMatch));
    }

    private Collection<Header> headers = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(HeadersKnobSupport.class.getName());
}
