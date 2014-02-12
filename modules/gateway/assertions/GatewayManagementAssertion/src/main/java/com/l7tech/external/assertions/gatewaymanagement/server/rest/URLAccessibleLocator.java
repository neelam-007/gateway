package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.objectmodel.EntityType;

/**
 * The restResource Locator is used to find rest urlAccessible services. It is a centralized point that will be able to retrieve the urlAccessible service.
 */
public interface URLAccessibleLocator {
    /**
     * Returns a urlAccessible service for the given entityType.
     *
     * @param entityType The entity type to return the url accessible for.
     * @return The URLAccessible for the given entity type, or null if there is no URLAccessible for that type.
     */
    URLAccessible findByEntityType(EntityType entityType);
}
