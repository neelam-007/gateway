package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.util.Arrays;

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
     *
     * @return The rest wadl
     */
    @GET
    @Produces({"application/vnd.sun.wadl+xml"})
    public StreamingOutput getWadl(@QueryParam("version") @DefaultValue("1.0.2") final String version) {
        final RestManVersion restManVersion = RestManVersion.fromString(version);
        if (restManVersion == null) {
            throw new WebApplicationException("Cannot read wadl resource. Unknown version: '" + version + "' expected one of: " + Functions.map(Arrays.asList(RestManVersion.values()), new Functions.Unary<String, RestManVersion>() {
                @Override
                public String call(RestManVersion restManVersion) {
                    return restManVersion.getStringRepresentation();
                }
            }).toString(), 404);
        }
        final String responseDocument = readWadlResourceAsString(restManVersion).replace(GATEWAY_URL, uriInfo.getBaseUri().toString());
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(new ByteArrayInputStream(responseDocument.getBytes()), output);
            }
        };
    }

    @NotNull
    private String readWadlResourceAsString(@NotNull final RestManVersion version) {
        final InputStream resource = getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/restAPI_" + version.getStringRepresentation() + ".wadl");
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            IOUtils.copyStream(resource, bout);
        } catch (IOException e) {
            throw new WebApplicationException("Cannot read wadl resource of version: " + version, 404);
        }
        return bout.toString();
    }
}

