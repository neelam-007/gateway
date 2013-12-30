package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This handles InvalidResourceException's
 *
 */
@Provider
public class InvalidResourceExceptionMapper implements ExceptionMapper<ResourceFactory.InvalidResourceException> {
    @Override
    public Response toResponse(ResourceFactory.InvalidResourceException exception) {
        return Response.status(Response.Status.BAD_REQUEST).
                entity(exception.getMessage()).
                type("text/plain").
                build();
    }
}
