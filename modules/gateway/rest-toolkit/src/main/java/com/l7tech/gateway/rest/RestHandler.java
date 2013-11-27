package com.l7tech.gateway.rest;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the rest handler. It converts request message information and context into something that Jersey can
 * understand and process.
 *
 * @author Victor Kazakov
 */
public class RestHandler {
    private ApplicationHandler appHandler;

    /**
     * It should only ever be called by the {@link RestHandlerProvider}.
     *
     * @param application The jersey application handler
     */
    private RestHandler(final ApplicationHandler application) {
        this.appHandler = application;
    }

    /**
     * Handle a rest request. This will delegate to Jersey to perform service resolution and parameter marshaling
     *
     * @param baseUri         This is the base uri of the server. This should include the host name, and port. For
     *                        example: 'https://restman-demo.l7tech.com:8443/rest/1.0/'
     * @param uri             The uri of the request. This should be the full uri of the request. Including the base Uri
     *                        and the query params. For example: 'https://restman-demo.l7tech.com:8443/rest/1.0/policies?name=myPolicy'
     * @param httpMethod      The http method used in the request.
     * @param contentType     The content type of the request body
     * @param body            The request body input stream
     * @param securityContext The security context that this call is made in. The principle user should be set.
     * @return Returns a container response containing the jersey response.
     * @throws PrivilegedActionException
     * @throws RequestProcessingException
     */
    public ContainerResponse handle(@NotNull final URI baseUri, @NotNull final URI uri, @NotNull final String httpMethod, final String contentType, @NotNull final InputStream body, @Nullable final SecurityContext securityContext, @NotNull final OutputStream responseOutputStream) throws PrivilegedActionException, RequestProcessingException {
        // Build the Jersey Container Request
        final ContainerRequest request = new ContainerRequest(baseUri, URI.create(baseUri.toString() + uri.toString()), httpMethod, securityContext, new MapPropertiesDelegate());
        //Set the content type header.
        if (contentType != null) {
            request.header(HttpHeaders.CONTENT_TYPE, contentType);
        }
        //Set the request body
        request.setEntityStream(body);

        //Create the subject to wrap the call to the jersey with. Setting the subject is required for securing manager calls
        final Subject subject = new Subject();
        if (securityContext != null) {
            subject.getPrincipals().add(securityContext.getUserPrincipal());
        }
        final AtomicReference<Future<ContainerResponse>> futureReference = new AtomicReference<>();
        //Surround the jersey call with a 'do as' in order to set the authenticated user.
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                //Get jersey to process the request.
                try {
                    futureReference.set(appHandler.apply(request, responseOutputStream));
                } catch (WebApplicationException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        final ContainerResponse response;
        try {
            //Get the response
            response = futureReference.get().get();
        } catch (InterruptedException e) {
            throw new RequestProcessingException("Unexpected exception getting a response", e);
        } catch (ExecutionException e) {
            throw new RequestProcessingException("Exception encountered processing a rest message: " + e.getMessage(), e);
        }
        return response;
    }

    /**
     * This is used by Jersey to create a new instance of the RestHandler. Leave it as package private
     */
    static final class RestHandlerProvider implements ContainerProvider {

        @Override
        public <T> T createContainer(Class<T> type, ApplicationHandler application) throws ProcessingException {
            if (RestHandler.class == type) {
                return type.cast(new RestHandler(application));
            }
            return null;
        }
    }
}
