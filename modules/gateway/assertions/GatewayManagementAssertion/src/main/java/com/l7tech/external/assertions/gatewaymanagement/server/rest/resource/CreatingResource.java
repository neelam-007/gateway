package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.XslStyleSheetResource;
import org.glassfish.jersey.message.XmlHeader;

import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

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
    public Response createResource(R resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException;
}
