package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.XslStyleSheetResource;
import org.glassfish.jersey.message.XmlHeader;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * The update resource interface. All resources that allow updating should implement this in order to support consistent
 * rest calls.
 *
 * @author Victor Kazakov
 */
public interface UpdatingResource<R> {
    /**
     * Updates an existing entity
     *
     * @param resource The updated entity
     * @param id       The id of the entity to update
     * @return a reference to the newly updated entity.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @PUT
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response updateResource(R resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;
}
