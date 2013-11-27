package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * The delete resource interface. All resources that allow deletion should implement this in order to support consistent
 * rest calls.
 *
 * @author Victor Kazakov
 */
public interface DeletingResource {

    /**
     * Deletes an existing entity.
     *
     * @param id The id of the entity to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @DELETE
    @Path("{id}")
    public void deleteResource(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException;
}
