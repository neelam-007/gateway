package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by a service that can look up full entities given their entity header.
 */
public interface HeaderBasedEntityFinder <ET extends Entity, HT extends EntityHeader> {
    /**
     * Returns the the full entity that matches the provided header.
     *
     * @param header the header.  Required.
     * @return The entity that matches the specified header.  Never null.
     * @throws FindException If no entity could be found
     */
    @NotNull
    ET findByHeader(@NotNull HT header) throws FindException;
}
