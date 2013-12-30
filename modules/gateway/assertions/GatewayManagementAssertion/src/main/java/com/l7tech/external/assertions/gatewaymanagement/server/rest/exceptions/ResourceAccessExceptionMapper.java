package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This is used to display a ResourceAccessException
 *
 * @author Victor Kazakov
 */
@Provider
public class ResourceAccessExceptionMapper implements ExceptionMapper<ResourceFactory.ResourceAccessException> {

    @Override
    public Response toResponse(ResourceFactory.ResourceAccessException exception) {
        return Response.status(Response.Status.FORBIDDEN).
                entity(exception.getMessage()).
                type("text/plain").
                build();
    }
}
