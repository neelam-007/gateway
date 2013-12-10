package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.objectmodel.EntityType;

/**
 * The restResource Locator is used to find rest rest resources. It is a centralized point that will be able to retrieve the resources.
 */
public interface RestResourceLocator {
    /**
     * Returns a RestEntityResource for the given entityType.
     *
     * @param entityType The entity type to return the entity resource for.
     * @return The RestEntityResource for the given entity type, or null if there is no RestEntityResource for that type.
     */
    RestEntityResource findByEntityType(EntityType entityType);
}
