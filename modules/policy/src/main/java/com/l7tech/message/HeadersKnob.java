package com.l7tech.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A generic headers knob for storing all types of headers.
 */
public interface HeadersKnob extends HasHeaders, MessageKnob {

    public static final String HEADER_TYPE_HTTP = "HTTP Header";

    /**
     * Sets a header to a specified value, overriding an existing value if found (case insensitive).
     *
     * @param name  the header name.
     * @param value the header value.
     * @param type  the header type.
     */
    void setHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type);

    /**
     * Sets a header to a specified value, overriding an existing value if found (case insensitive).
     *
     * @param name        the header name.
     * @param value       the header value.
     * @param type        the header type.
     * @param passThrough true if the header should be passed through when routing or back to the client in a response.
     */
    void setHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type, final boolean passThrough);

    /**
     * Adds a header with the given value and type.
     *
     * @param name  the header name.
     * @param value the header value.
     * @param type  the header type.
     */
    void addHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type);

    /**
     * Adds a header with the given value, type, and passthrough setting.
     *
     * @param name        the header name.
     * @param value       the header value.
     * @param type        the header type.
     * @param passThrough true if the header should be passed through when routing or back to the client in a response.
     */
    void addHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type, final boolean passThrough);

    /**
     * Removes all headers with the given name (case insensitive) and type.
     *
     * @param name the header name.
     * @param type the header type.
     */
    void removeHeader(@NotNull final String name, @NotNull final String type);

    /**
     * Removes all headers with the given name and type.
     *
     * @param name          the header name.
     * @param type          the header type.
     * @param caseSensitive set to true for case sensitive name matching.
     */
    void removeHeader(@NotNull final String name, @NotNull final String type, final boolean caseSensitive);

    /**
     * Removes all headers with the given name (case insensitive), type, and value (case sensitive).
     *
     * @param name  the header name.
     * @param value the header value.
     * @param type  the header type.
     */
    void removeHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type);

    /**
     * Removes all headers with the given name (case sensitive), type, and value (case sensitive).
     *
     * @param name          the header name.
     * @param type          the header type.
     * @param value         the header value.
     * @param caseSensitive set to true for case sensitive name matching.
     */
    void removeHeader(@NotNull final String name, @Nullable final Object value, @NotNull final String type, final boolean caseSensitive);

    /**
     * Checks for the presence of a header in the knob with the specified name and type.
     *
     * @param name the header name to look for (case insensitive).
     * @param type the type of the header to look for.
     * @return true if the knob contains at least one header with the given name and type.
     */
    boolean containsHeader(@NotNull final String name, @NotNull final String type);

    /**
     * Get all of the headers in the knob.
     *
     * @return an unmodifiable collection of all headers of every type in the knob.
     */
    Collection<Header> getHeaders();

    /**
     * Get all headers of the specified type in the knob.
     *
     * @param type  the type of the headers to retrieve, or null to retrieve all
     * @return an unmodifiable collection of matching headers.
     */
    Collection<Header> getHeaders(@Nullable final String type);

    /**
     * Get all headers of the specified type in the knob, optionally excluding non-passthrough headers.
     *
     * @param type                  the type of the headers to retrieve, or null to retrieve all
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when
     *                              routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an unmodifiable collection of matching headers.
     */
    Collection<Header> getHeaders(@Nullable final String type, final boolean includeNonPassThrough);

    /**
     * Get all headers of the specified name and type in the knob.
     *
     * @param name the name of the headers to retrieve (case-insensitive).
     * @param type the type of the headers to retrieve
     * @return an unmodifiable collection of the matching headers.
     */
    Collection<Header> getHeaders(@NotNull final String name, @Nullable final String type);

    /**
     * Get all headers of the specified name and type in the knob, optionally excluding non-passthrough headers.
     *
     * @param name                  the name of the headers to retrieve (case-insensitive).
     * @param type                  the type of the headers to retrieve.
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when
     *                              routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an unmodifiable collection of the matching headers.
     */
    Collection<Header> getHeaders(@NotNull final String name, @Nullable final String type, final boolean includeNonPassThrough);

    /**
     * Get the values of all headers of the specified name and type in the knob.
     *
     * @param name  the name of the headers to retrieve values for (case-insensitive).
     * @param type  the type of headers to retrieve values for, or null to retrieve headers of all types
     * @return an array of the matching header values.
     */
    String[] getHeaderValues(@NotNull final String name, @Nullable final String type);

    /**
     * Get the values of all headers of the specified name and type in the knob, optionally excluding
     * non-passthrough headers.
     *
     * @param name                  the name of the headers to retrieve values for (case-insensitive).
     * @param type                  the type of headers to retrieve values for, or null to retrieve headers of all types
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @return an array of the matching header values.
     */
    String[] getHeaderValues(@NotNull final String name, @Nullable final String type, final boolean includeNonPassThrough);

    /**
     * Get the names of all headers of the specified type in the knob, sorted in case-insensitive order.
     *
     * @param type  the type of headers to retrieve the names of, or null to include all header types
     * @return an array of the matching header names.
     */
    String[] getHeaderNames(@Nullable final String type);

    /**
     * Get the names of all headers of the specified type in the knob, sorted in case-insensitive order,
     * optionally excluding non-passthrough headers.
     *
     * @param type                  the type of headers to retrieve the names of, or null to include all header types
     * @param includeNonPassThrough if true, headers which are not meant to be passed through when routing or back to the client in a response will be included.
     *                              If false, non-passthrough headers will not be included.
     * @param caseSensitive         if true, header names will be returned in case-sensitive order.
     *                              if false, header names will be returned in case-insensitive order.
     * @return an array of header names.
     */
    String[] getHeaderNames(@Nullable final String type, final boolean includeNonPassThrough, final boolean caseSensitive);
}
