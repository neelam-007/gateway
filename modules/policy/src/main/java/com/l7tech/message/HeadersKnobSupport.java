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
        return getHeaderValues(name, null);
    }

    @Override
    public String[] getHeaderNames() {
        return getHeaderNames(null);
    }

    @Override
    public void setHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type) {
        setHeader(name, value, type, true);
    }

    @Override
    public void setHeader(@NotNull String name, @Nullable Object value, @NotNull final String type, boolean passThrough) {
        removeHeader(name, type);
        addHeader(name, value, type, passThrough);
    }

    @Override
    public void addHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type) {
        addHeader(name, value, type, true);
    }

    @Override
    public void addHeader(@NotNull String name, @Nullable Object value, @NotNull final String type, boolean passThrough) {
        headers.add(new Header(name, value, type, passThrough));
    }

    @Override
    public void removeHeader(@NotNull final String name, @NotNull final String type) {
        removeHeader(name, type, false);
    }

    @Override
    public void removeHeader(@NotNull String name, @NotNull final String type, boolean caseSensitive) {
        final Collection<Header> toRemove = new ArrayList<>();
        for (final Header header : headers) {
            if (nameMatches(name, caseSensitive, header.getKey()) && (null == type || type.equals(header.getType()))) {
                toRemove.add(header);
            }
        }
        if (toRemove.isEmpty()) {
            logger.log(Level.FINE, "No header found with name: " + name);
        }
        headers.removeAll(toRemove);
    }

    @Override
    public void removeHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type) {
        removeHeader(name, value, type, false);
    }

    @Override
    public void removeHeader(@NotNull String name, @Nullable Object value, @NotNull String type, boolean caseSensitive) {
        final Collection<Header> toRemove = new ArrayList<>();
        final Collection<Header> replacements = new ArrayList<>();

        for (final Header header : headers) {
            if ((nameMatches(name, caseSensitive, header.getKey())) && (type.equals(header.getType()))) {
                if (header.getValue() instanceof String && ((String) header.getValue()).contains(VALUE_SEPARATOR)) {
                    // handle comma-separated multiple values
                    final List<String> subValues = new ArrayList<>();
                    for (final String token : StringUtils.split((String) header.getValue(), VALUE_SEPARATOR)) {
                        subValues.add(token.trim());
                    }
                    final List<String> subValuesToRemove = new ArrayList<>();
                    for (final String subValue : subValues) {
                        if (ObjectUtils.equals(value, subValue)) {
                            subValuesToRemove.add(subValue);
                        }
                    }
                    subValues.removeAll(subValuesToRemove);
                    toRemove.add(header);
                    replacements.add(new Header(header.getKey(), StringUtils.join(subValues, VALUE_SEPARATOR), header.getType()));
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
    public boolean containsHeader(@NotNull final String name, @NotNull String type) {
        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name) && type.equals(header.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Header> getHeaders() {
        return getHeaders(null);
    }

    @Override
    public Collection<Header> getHeaders(@Nullable final String type) {
        return getHeaders(type, true);
    }

    @Override
    public Collection<Header> getHeaders(@Nullable final String type, final boolean includeNonPassThrough) {
        final Collection<Header> ret = new ArrayList<>();

        for (final Header header : headers) {
            if ((header.isPassThrough() || includeNonPassThrough) && (null == type || type.equals(header.getType()))) {
                ret.add(header);
            }
        }

        return Collections.unmodifiableCollection(ret);
    }

    @Override
    public Collection<Header> getHeaders(@NotNull final String name, @Nullable final String type) {
        return getHeaders(name, type, true);
    }

    @Override
    public Collection<Header> getHeaders(@NotNull final String name, @Nullable final String type, final boolean includeNonPassThrough) {
        final Collection<Header> ret = new ArrayList<>();

        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name) &&
                    (header.isPassThrough() || includeNonPassThrough) &&
                    (null == type || type.equals(header.getType()))) {
                ret.add(header);
            }
        }

        return Collections.unmodifiableCollection(ret);
    }

    @Override
    public String[] getHeaderValues(@NotNull final String name, @Nullable final String type) {
        return getHeaderValues(name, type, true);
    }

    @Override
    public String[] getHeaderValues(@NotNull final String name, @Nullable final String type, final boolean includeNonPassThrough) {
        final Collection<String> valuesAsString = new ArrayList<>();

        for (final Header header : headers) {
            if (header.getKey().equalsIgnoreCase(name) &&
                    (header.isPassThrough() || includeNonPassThrough) &&
                    (null == type || type.equals(header.getType()))) {
                valuesAsString.add(header.getValue() == null ? null : header.getValue().toString());
            }
        }

        return valuesAsString.toArray(new String[valuesAsString.size()]);
    }

    @Override
    public String[] getHeaderNames(@Nullable final String type) {
        return getHeaderNames(type, true, false);
    }

    @Override
    public String[] getHeaderNames(@Nullable final String type, final boolean includeNonPassThrough, final boolean caseSensitive) {
        final Collection<String> names = caseSensitive
                ? new TreeSet<String>()
                : new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final Header header : headers) {
            if ((header.isPassThrough() || includeNonPassThrough) && (null == type || type.equals(header.getType()))) {
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
