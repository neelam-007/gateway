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
 * This resource serves the gateway-management schema
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + SchemaResource.SchemaPath)
@Singleton
public class SchemaResource {
    public static final String SchemaPath = "gateway-management.xsd";

    /**
     * Returns the rest schema
     *
     * @return The rest schema
     */
    @GET
    @Produces({"application/xml"})
    public StreamingOutput getStyleSheet() {
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(SchemaResource.this.getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/gateway-management.xsd"), output);
            }
        };
    }
}

