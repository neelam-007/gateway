package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.util.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This resource servers all the xsl style sheets used by the rest api for transforming resources
 *
 * @author Victor Kazakov
 */
@Provider
@Path(XslStyleSheetResource.StyleSheetBasePath)
public class XslStyleSheetResource {
    public static final String StyleSheetBasePath = "stylesheets";

    public static final String DefaultStyleSheetPath = StyleSheetBasePath+"/defaultStyleSheet.xsl";
    @Path("defaultStyleSheet.xsl")
    @GET
    @Produces({"text/xsl"})
    public StreamingOutput getDefaultStyleSheet(){
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(XslStyleSheetResource.this.getClass().getResourceAsStream("defaultStyleSheet.xsl"), output);
            }
        };

    }
}
