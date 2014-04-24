package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.util.IOUtils;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This resource servers the documentation and it's resources
 */
@Provider
@Path(DependencyResource.Version_URI + DocumentationResource.DocumentationBasePath)
@Singleton
public class DocumentationResource {
    public static final String DocumentationBasePath = "doc";

    @Context
    protected UriInfo uriInfo;

    /**
     * Returns the home page of the documentation.
     * @return The documentation home page.
     */
    @GET
    public Response documentation() {
        //Need to enforce that the doc url ends with a / This way document resources will load properly
        if(!uriInfo.getPath().endsWith("/")){
            return Response.status(Response.Status.MOVED_PERMANENTLY).header("Location", uriInfo.getRequestUriBuilder().path("/").build().toString()).build();
        }
        return documentationResources("home.html");
    }

    /**
     * Returns any resources that the documentation requires.
     *
     * @param resource The resource Name
     * @return The resource.
     */
    @Path("{resource}")
    @GET
    public Response documentationResources(@PathParam("resource") final String resource) {
        final String resourcePath = "/com/l7tech/external/assertions/gatewaymanagement/server/rest/doc/" + resource;
        final InputStream resourceAsStream = DocumentationResource.this.getClass().getResourceAsStream(resourcePath);
        if (resourceAsStream == null) {
            throw new WebApplicationException("Cannot locate documentation resource: " + resource, 404);
        }

        final Response.ResponseBuilder response = Response.ok(new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(resourceAsStream, output);
            }
        });

        //add the correct content type header.
        if(resource.endsWith(".css")){
            response.header(HttpHeaders.CONTENT_TYPE, "text/css");
        } else if(resource.endsWith(".js")){
            response.header(HttpHeaders.CONTENT_TYPE, "text/javascript");
        } else if(resource.endsWith(".html")){
            response.header(HttpHeaders.CONTENT_TYPE, "text/html");
        } else if(resource.endsWith(".png")){
            response.header(HttpHeaders.CONTENT_TYPE, "image/png");
        }

        return response.build();
    }
}
