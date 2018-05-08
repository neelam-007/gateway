package com.l7tech.gateway.common;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import org.jetbrains.annotations.NotNull;

/**
 * Helps to create {@link Mapping} for more readable unit tests.
 */
public class MappingBuilder {
    private Mapping mapping = ManagedObjectFactory.createMapping();

    public MappingBuilder withType(@NotNull final String type) {
        mapping.setType(type);
        return this;
    }

    public MappingBuilder withAction(@NotNull final Mapping.Action action) {
        mapping.setAction(action);
        return this;
    }

    public MappingBuilder withSrcId(@NotNull final String srcId) {
        mapping.setSrcId(srcId);
        return this;
    }

    public Mapping build() {
        return mapping;
    }
}
