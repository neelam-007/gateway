package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Supports transformation between portal generic entities and their resource representations.
 *
 * @param <R> the type of portal resource.
 * @param <P> the type of portal generic entity.
 */
public interface ResourceTransformer<R extends Resource, P extends AbstractPortalGenericEntity> {
    /**
     * Convert portal resource to portal generic entity.
     *
     * @param resource the resource to convert.
     * @return the resource converted to a portal generic entity.
     */
    P resourceToEntity(@NotNull final R resource);

    /**
     * Convert portal generic entity to portal resource.
     *
     * @param entity the entity to convert.
     * @return the portal generic entity as a resource.
     */
    R entityToResource(@NotNull final P entity);
}
