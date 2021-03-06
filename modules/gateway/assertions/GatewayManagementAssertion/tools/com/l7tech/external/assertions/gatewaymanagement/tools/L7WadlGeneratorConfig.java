package com.l7tech.external.assertions.gatewaymanagement.tools;

import org.glassfish.jersey.server.wadl.config.WadlGeneratorConfig;
import org.glassfish.jersey.server.wadl.config.WadlGeneratorDescription;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.WadlGeneratorResourceDocSupport;

import java.util.List;

/**
 * The Wsdl generator config is responsible for adding the wsdl generators. Currently it adds the generator that adds
 * the javadocs from the java source files to the wadl
 */
public class L7WadlGeneratorConfig extends WadlGeneratorConfig {
    @Override
    public List<WadlGeneratorDescription> configure() {
        return generator(WadlGeneratorResourceDocSupport.class)
                .prop("resourceDocStream", L7WadlGeneratorConfig.this.getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/resourcedoc.xml"))
                .generator(ExtendedWadlGenerator.class)
                .prop("resourceDocStream", L7WadlGeneratorConfig.this.getClass().getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/resourcedoc.xml"))
                .descriptions();
    }
}
