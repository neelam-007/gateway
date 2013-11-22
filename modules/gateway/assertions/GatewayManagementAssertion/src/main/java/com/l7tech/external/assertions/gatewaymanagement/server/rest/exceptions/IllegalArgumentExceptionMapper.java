package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This is used to display an IllegalArgumentException
 *
 * @author Victor Kazakov
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST).
                entity(exception.getMessage()).
                type("text/plain").
                build();
    }
}
