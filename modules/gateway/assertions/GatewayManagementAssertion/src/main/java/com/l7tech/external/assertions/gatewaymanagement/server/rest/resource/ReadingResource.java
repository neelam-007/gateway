package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.FindException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * The read resource interface. All resources that allow retrieving should implement this in order to support consistent
 * rest calls.
 *
 * @author Victor Kazakov
 */
public interface ReadingResource<R> {
    /**
     * This implements the GET method to retrieve an entity by a given id.
     *
     * @param id The identity of the entity to select
     * @return The selected entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @GET
    @Path("{id}")
    public Item<R> getResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, FindException;
}
