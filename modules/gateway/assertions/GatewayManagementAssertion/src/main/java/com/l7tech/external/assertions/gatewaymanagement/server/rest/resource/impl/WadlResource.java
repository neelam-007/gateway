package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.*;

/**
 * This resource serves the wadl used by the rest api
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + WadlResource.WadlPath)
@Singleton
public class WadlResource {
    public static final String WadlPath = "rest.wadl";
    public static final String GATEWAY_URL = "-ssg-template-url-";

    @Context
    protected UriInfo uriInfo;

    /**
     * Returns the rest wadl
     * @return The rest wadl
     */
    @GET
    @Produces({"application/vnd.sun.wadl+xml"})
    public StreamingOutput getWadl() {
        final String responseDocument = readWadlResourceAsString().replace(GATEWAY_URL, uriInfo.getBaseUri().toString());
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(new ByteArrayInputStream(responseDocument.getBytes()), output);
            }
        };
    }

    @NotNull
    private String readWadlResourceAsString() {
        final InputStream resource = getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/restAPI.wadl");
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            IOUtils.copyStream(resource, bout);
        } catch (IOException e) {
            throw new WebApplicationException("Cannot read wadl resource", 404);
        }
        return bout.toString();
    }
}

