package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.XslStyleSheetResource;
import org.glassfish.jersey.message.XmlHeader;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The create resource interface. All resources that allow creation should implement this in order to support consistent
 * rest calls.
 *
 * @author Victor Kazakov
 */
public interface CreatingResource<R> {
    /**
     * Creates a new entity
     *
     * @param resource The entity to create
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResource(@Context UriInfo uriInfo, R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;

    /**
     * Creates a new entity with the given id
     *
     * @param resource The entity to create
     * @param id       The id to create the entity with.
     * @return a reference to the newly created entity
     * @throws ResourceFactory.ResourceNotFoundException
     *
     * @throws ResourceFactory.InvalidResourceException
     *
     */
    @POST
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResourceWithId(@Context UriInfo uriInfo, R resource, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;
}
