package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * This resource servers the documentation and it's resources
 */
@Provider
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + DocumentationResource.DocumentationBasePath)
@Singleton
public class DocumentationResource {
    public static final String DocumentationBasePath = "doc";
    //This is the documentation template page. All documentation html pages should be wrapped using this template
    public static final String TemplateDocument = readResourceAsString("template.html");

    //These are all the available documentation pages and their titles
    public static final Map<String, String> DocumentationPagesAndTitles = CollectionUtils.MapBuilder.<String, String>builder()
            .put("home.html", "Getting Started")
            .put("authentication.html", "Authentication")
            .put("migration.html", "Migration")
            .put("migration-example.html", "Migration Example")
            .put("restDoc.html", "API Resources")
            .map();

    @Context
    protected UriInfo uriInfo;

    /**
     * Returns the home page of the documentation.
     *
     * @return The documentation home page.
     */
    @GET
    public Response documentation() {
        //Need to enforce that the doc url ends with a / This way document resources will load properly
        if (!uriInfo.getPath().endsWith("/")) {
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
        final Response.ResponseBuilder response;
        if (DocumentationPagesAndTitles.containsKey(resource)) {
            //This mean that the resource is a html page. So need to templatize it.
            String responseDocument = TemplateDocument
                    .replace("$#{title}", DocumentationPagesAndTitles.get(resource))
                    .replace("$#{content}", readResourceAsString(resource))
                    .replaceAll("\\$#\\{scheme\\}", uriInfo.getBaseUri().getScheme())
                    .replaceAll("\\$#\\{host\\}", uriInfo.getBaseUri().getHost())
                    .replaceAll("\\$#\\{port\\}", String.valueOf(uriInfo.getBaseUri().getPort()))
                    .replaceAll("\\$#\\{service-url\\}", uriInfo.getBaseUri().getPath().substring(1, uriInfo.getBaseUri().getPath().length() - 1));
            response = Response.ok(responseDocument);
        } else {
            //some other resource so just return the stream
            response = Response.ok(new StreamingOutput() {
                public void write(OutputStream output) throws IOException {
                    IOUtils.copyStream(getResourceInputStream(resource), output);
                }
            });
        }

        //add the correct content type header. This is needed to have it display properly in browsers
        if (resource.endsWith(".css")) {
            response.header(HttpHeaders.CONTENT_TYPE, "text/css");
        } else if (resource.endsWith(".js")) {
            response.header(HttpHeaders.CONTENT_TYPE, "text/javascript");
        } else if (resource.endsWith(".html")) {
            response.header(HttpHeaders.CONTENT_TYPE, "text/html");
        } else if (resource.endsWith(".png")) {
            response.header(HttpHeaders.CONTENT_TYPE, "image/png");
        }

        return response.build();
    }

    /**
     * Returns the documentation resource as a string.
     *
     * @param resourcePath The resource path
     * @return The string representation of the resources
     * @throws javax.ws.rs.WebApplicationException This is thrown if the resources cannot be found or if it cannot be
     *                                             read.
     */
    @NotNull
    private static String readResourceAsString(@NotNull final String resourcePath) {
        final InputStream resource = getResourceInputStream(resourcePath);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            IOUtils.copyStream(resource, bout);
        } catch (IOException e) {
            throw new WebApplicationException("Cannot read documentation resource: " + resourcePath, 404);
        }
        return bout.toString();
    }

    /**
     * Returns the input stream for the documentation resource
     *
     * @param resourcePath The path of the documentation resource.
     * @return The input stream for the documenation resource.
     * @throws javax.ws.rs.WebApplicationException This is thrown if the resource cannot be found.
     */
    @NotNull
    private static InputStream getResourceInputStream(@NotNull final String resourcePath) {
        final InputStream resource = DocumentationResource.class.getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/doc/" + resourcePath);
        if (resource == null) {
            throw new WebApplicationException("Cannot locate documentation resource: " + resourcePath, 404);
        }
        return resource;
    }
}
