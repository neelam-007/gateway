package com.l7tech.server.bundling;

import com.l7tech.gateway.common.transport.InterfaceTag;
import org.jetbrains.annotations.NotNull;

/**
 * An entity container for interface tags.
 */
public class InterfaceTagContainer extends EntityContainer<InterfaceTag> {
    /**
     * Create a new entity container for an interface tag.
     *
     * @param interfaceTag The interface tag to create the container for
     */
    public InterfaceTagContainer(@NotNull final InterfaceTag interfaceTag) {
        super(InterfaceTag.getSyntheticId(interfaceTag), interfaceTag);
    }
}
