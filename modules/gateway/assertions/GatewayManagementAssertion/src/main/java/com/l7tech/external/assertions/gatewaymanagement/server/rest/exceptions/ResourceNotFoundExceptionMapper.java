package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This handles ResourceNotFoundException's
 *
 * @author Victor Kazakov
 */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceFactory.ResourceNotFoundException> {
    @Override
    public Response toResponse(ResourceFactory.ResourceNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND).
                entity(exception.getMessage()).
                type("text/plain").
                build();
    }
}
