package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for transforming entity resources
 */
public interface RestEntityBaseResource<R> extends ReadingResource<R> {

    public Item<R> toReference(EntityHeader entityHeader);

    /**
     * Returns the entity type of the resource
     *
     * @return The resource entity type
     */
    @NotNull
    public EntityType getEntityType() ;

    /**
     * Returns the Url of this resource with the given id
     * @param id The id of the resource. Leave it blank to get the resource listing url
     * @return The url of the resource
     */
    public String getUrl(String id);
}
