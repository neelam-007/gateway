package com.l7tech.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic headers knob for storing all types of headers.
 */
public interface HeadersKnob extends HasHeaders, MessageKnob {

    /**
     * Sets a header to a specified value, overriding an existing value if found.
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void setHeader(@NotNull final String name, @Nullable final Object value);

    /**
     * Adds a header with the given value.
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void addHeader(@NotNull final String name, @Nullable final Object value);

    /**
     * Removes all headers with the given name.
     *
     * @param name the header name.
     */
    void removeHeader(@NotNull final String name);

    /**
     * Removes all headers with the given name and value.
     *
     * @param name  the header name.
     * @param value the header value.
     */
    void removeHeader(@NotNull final String name, @Nullable final Object value);
}
