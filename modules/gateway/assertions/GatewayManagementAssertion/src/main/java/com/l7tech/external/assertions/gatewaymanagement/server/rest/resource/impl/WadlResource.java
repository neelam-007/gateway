package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.util.IOUtils;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This resource serves the wadl used by the rest api
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + WadlResource.WadlPath)
@Singleton
public class WadlResource {
    public static final String WadlPath = "rest.wadl";

    @GET
    @Produces({"application/vnd.sun.wadl+xml"})
    public StreamingOutput getDefaultStyleSheet() {
        System.out.println(WadlResource.this.getClass().getResource("/").getFile());
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(WadlResource.this.getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/restAPI.wadl"), output);
            }
        };
    }
}

