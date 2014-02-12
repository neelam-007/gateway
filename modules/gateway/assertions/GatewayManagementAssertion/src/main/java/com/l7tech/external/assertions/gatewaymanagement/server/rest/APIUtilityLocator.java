package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.objectmodel.EntityType;

/**
 * The APIUtilityLocator is used to find api utilities. These include APIResourceFactory and APITransformer.
 * It is a centralized point that will be able to retrieve the utilities.
 */
public interface APIUtilityLocator {
    /**
     * Returns an APIResourceFactory for the given entityType.
     *
     * @param entityType The entity type to return the factory for.
     * @return The APIResourceFactory for the given entity type, or null if there is no APIResourceFactory for that type.
     */
    APIResourceFactory findFactoryByEntityType(EntityType entityType);

    /**
     * Returns an APITransformer for the given entity type.
     *
     * @param entityType The entity type to return the transformer for.
     * @return The APITransformer for the given entity type, or null if there is no APITransformer for that type.
     */
    APITransformer findTransformerByEntityType(EntityType entityType);
}
