package com.l7tech.server.message;

import org.jetbrains.annotations.NotNull;

/**
 * Interface that the {@link PolicyEnforcementContext} implements in order to support passing the assertion ordinal path
 * onto the child PEC.
 */

public interface AssertionOrdinalProcessor {
    /**
     * Adds the assertion ordinal path from this object onto the specified {@code child} PEC.
     * @param child     the child PEC that'll
     */
    void passDownAssertionOrdinal(@NotNull final PolicyEnforcementContext child);
}
