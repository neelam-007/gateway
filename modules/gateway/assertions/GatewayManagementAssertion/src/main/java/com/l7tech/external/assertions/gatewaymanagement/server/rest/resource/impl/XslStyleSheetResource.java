package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

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
 * This resource servers all the xsl style sheets used by the rest api for transforming resources
 * <p/>
 * The {path:.*} allows the stylesheet to be found from any directory in the rest application. We do this because it is
 * not easy to find the root path for load the stylesheets from otherwise
 *
 * @author Victor Kazakov
 */
@Provider
@Path("{path:.*}" + XslStyleSheetResource.StyleSheetBasePath)
@Singleton
public class XslStyleSheetResource {
    public static final String StyleSheetBasePath = "stylesheets";
    public static final String DEFAULT_STYLESHEET_HEADER = "<?xml-stylesheet type=\"text/xsl\" href=\"" + XslStyleSheetResource.DefaultStyleSheetPath + "\"?>";

    public static final String DefaultStyleSheetPath = StyleSheetBasePath + "/defaultStyleSheet.xsl";

    @Path("defaultStyleSheet.xsl")
    @GET
    @Produces({"text/xsl"})
    public StreamingOutput getDefaultStyleSheet() {
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                IOUtils.copyStream(XslStyleSheetResource.this.getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/defaultStyleSheet.xsl"), output);
            }
        };
    }
}
