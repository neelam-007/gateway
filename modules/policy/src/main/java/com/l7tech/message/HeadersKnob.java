package com.l7tech.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A generic headers knob for storing all types of headers.
 */
public interface HeadersKnob extends HasHeaders, MessageKnob {

    /**
     * Sets a header to a specified value, overriding an existing value if found (case insensitive).
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void setHeader(@NotNull final String name, @Nullable final Object value);

    /**
     * Sets a header to a specified value, overriding an existing value if found (case insensitive).
     *
     * @param name        the header name.
     * @param value       the header value.
     * @param passThrough true if the header should be passed through when routing or back to the client in a response.
     */
    void setHeader(@NotNull final String name, @Nullable final Object value, final boolean passThrough);

    /**
     * Adds a header with the given value.
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void addHeader(@NotNull final String name, @Nullable final Object value);

    /**
     * Adds a header with the given value.
     *
     * @param name        the header name.
     * @param value       the header value.
     * @param passThrough true if the header should be passed through when routing or back to the client in a response.
     */
    void addHeader(@NotNull final String name, @Nullable final Object value, final boolean passThrough);

    /**
     * Removes all headers with the given name (case insensitive).
     *
     * @param name the header name.
     */
    void removeHeader(@NotNull final String name);

    /**
     * Removes all headers with the given name.
     *
     * @param name          the header name.
     * @param caseSensitive set to true for case sensitive name matching.
     */
    void removeHeader(@NotNull final String name, final boolean caseSensitive);

    /**
     * Removes all headers with the given name (case insensitive) and value (case sensitive).
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void removeHeader(@NotNull final String name, @Nullable final Object value);

    /**
     * Removes all headers with the given name and value (case sensitive).
     *
     * @param name          the header name.
     * @param value         the header value.
     * @param caseSensitive set to true for case sensitive name matching.
     */
    void removeHeader(@NotNull final String name, @Nullable final Object value, final boolean caseSensitive);

    /**
     * @param name the header name to look for (case insensitive).
     * @return true if the knob contains at least one header with the given name.
     */
    boolean containsHeader(@NotNull final String name);

    /**
     * @return an unmodifiable collection of the headers in the knob.
     */
    Collection<Header> getHeaders();

    /**
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an unmodifiable collection of the headers in the knob.
     */
    Collection<Header> getHeaders(final boolean includeNonPassThrough);

    /**
     * @param name the name of the headers to retrieve (case-insensitive).
     * @return an unmodifiable collection of the headers in the knob with the given name (case insensitive).
     */
    Collection<Header> getHeaders(@NotNull final String name);

    /**
     * @param name                  the name of the headers to retrieve (case-insensitive).
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an unmodifiable collection of the headers in the knob with the given name (case insensitive).
     */
    Collection<Header> getHeaders(@NotNull final String name, final boolean includeNonPassThrough);

    /**
     * @param name                  the name of the headers to retrieve values for (case-insensitive).
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an array of header values.
     */
    String[] getHeaderValues(@NotNull final String name, final boolean includeNonPassThrough);

    /**
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an array of header values.
     */
    String[] getHeaderNames(final boolean includeNonPassThrough, final boolean caseSensitive);
}
