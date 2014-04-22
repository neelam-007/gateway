package com.l7tech.message;

import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a non-mutable header (key-value pair).
 */
public class Header extends Pair<String, Object> {
    private final String type;
    private final boolean passThrough;

    /**
     * @param name  the header name.
     * @param value the header value.
     */
    public Header(@NotNull final String name, @Nullable final Object value, @NotNull final String type) {
        this(name, value, type, true);
    }

    /**
     * @param name        the header name.
     * @param value       the header value.
     * @param passThrough true if the header should be passed along in the request (when routing) or in the response back to the client. False if otherwise.
     */
    public Header(@NotNull final String name, @Nullable final Object value, @NotNull final String type, final boolean passThrough) {
        super(name, value);

        this.type = type;
        this.passThrough = passThrough;
    }

    /**
     * @return true if the header should be passed along in the request (when routing) or in the response back to the client. False if otherwise.
     */
    public boolean isPassThrough() {
        return passThrough;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && this.type.equals(((Header) o).getType());
    }
}
