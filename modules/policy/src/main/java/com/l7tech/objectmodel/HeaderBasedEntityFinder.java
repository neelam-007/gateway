package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by a service that can look up full entities given their entity header.
 */
public interface HeaderBasedEntityFinder {
    /**
     * Returns the the full entity that matches the provided header.
     *
     * @param header the header.  Required.
     * @return The entity that matches the specified header.  For some entity types, may be null if no entity was found.
     * @throws FindException If no entity could be found
     */
    @Nullable
    Entity find(@NotNull EntityHeader header) throws FindException;
}
