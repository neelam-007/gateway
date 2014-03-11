package com.l7tech.message;

import com.l7tech.util.Pair;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-mutable header (key-value pair).
 */
public class Header extends Pair<String, Object> {
    private final boolean passThrough;

    /**
     * @param name  the header name.
     * @param value the header value.
     */
    public Header(@NotNull final String name, @Nullable final Object value) {
        this(name, value, true);
    }

    /**
     * @param name        the header name.
     * @param value       the header value.
     * @param passThrough true if the header should be passed along in the request (when routing) or in the response back to the client. False if otherwise.
     */
    public Header(@NotNull final String name, @Nullable final Object value, final boolean passThrough) {
        super(name, value);
        this.passThrough = passThrough;
    }

    /**
     * @return true if the header should be passed along in the request (when routing) or in the response back to the client. False if otherwise.
     */
    public boolean isPassThrough() {
        return passThrough;
    }
}
