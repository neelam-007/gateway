package com.l7tech.gateway.rest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.net.URI;
import java.security.PrivilegedActionException;

/**
 * This the rest management agent will process a rest request using jersey.
 *
 * @author Victor Kazakov
 */
public interface RestAgent {

    /**
     * Processes a rest request.
     *
     * @param baseUri         This is the base uri of the server. This should include the host name, and port. For
     *                        example: 'https://restman-demo.l7tech.com:8443/rest/1.0/'
     * @param uri             The uri of the request. This should be the full uri of the request. Including the base Uri
     *                        and the query params. For example: 'https://restman-demo.l7tech.com:8443/rest/1.0/policies?name=myPolicy'
     * @param httpMethod      The http method used for the request.
     * @param contentType     The request content type
     * @param body            The request body stream
     * @param securityContext The security context that this call is made in. The principle user should be set.
     * @return The rest response after processing the request.
     * @throws PrivilegedActionException  This is thrown if the user given does not have access to a resource.
     * @throws RequestProcessingException This is thrown if there was an error processing the request.
     */
    public RestResponse handleRequest(@NotNull URI baseUri, @NotNull URI uri, @NotNull String httpMethod, @NotNull String contentType, @NotNull InputStream body, @Nullable SecurityContext securityContext) throws PrivilegedActionException, RequestProcessingException;
}
