package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.gateway.rest.RequestProcessingException;
import com.l7tech.gateway.rest.RestAgent;
import com.l7tech.gateway.rest.RestAgentImpl;
import com.l7tech.gateway.rest.RestResponse;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.glassfish.jersey.server.ServerProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is used to build the rest wadl during compile time so that it can be statically served.
 */
public class WadlGenerator {

    private static final Pattern pattern = Pattern.compile("(\\w*)\\.class");
    private static final String RESOURCE_PATH = "/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/impl";


    public static void main(String args[]) throws IOException, ClassNotFoundException, PrivilegedActionException, RequestProcessingException {
        //get list of java resource classes
        List<String> classes = new ArrayList<>();
        URL url = WadlGenerator.class.getResource(RESOURCE_PATH);
        if (url == null) {
            throw new FileNotFoundException(RESOURCE_PATH);
        } else {
            File dir = new File(url.getFile());
            for (File nextFile : dir.listFiles()) {
                Matcher matcher = pattern.matcher(nextFile.getName());
                if (matcher.matches()) {
                    classes.add(RESOURCE_PATH.substring(1).replaceAll("/", ".") + "." + matcher.group(1));
                }
            }
        }

        RestAgent restAgent = buildRestAgent(classes);

        RestResponse response = restAgent.handleRequest(URI.create(""), URI.create("application.wadl"), "GET", null, new EmptyInputStream(), null, null);

        File wadlFile = new File(args[0] + "/restAPI.wadl");

        IOUtils.copyStream(response.getInputStream(), new FileOutputStream(wadlFile));
    }

    private static RestAgent buildRestAgent(final List<String> classes) throws ClassNotFoundException {
        RestAgentImpl restAgent = new RestAgentImpl();
        restAgent.setAdditionalResourceClasses(Functions.map(classes, new Functions.UnaryThrows<Class<?>, String, ClassNotFoundException>() {
            @Override
            public Class<?> call(String s) throws ClassNotFoundException {
                return Class.forName(s);
            }
        }));

        // This adds the wsdl configurator so that the java docs get added to the generated wsdl
        restAgent.setResourceConfigProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put(ServerProperties.WADL_GENERATOR_CONFIG, Class.forName("com.l7tech.external.assertions.gatewaymanagement.tools.L7WadlGeneratorConfig"))
                .map());

        restAgent.init();

        return restAgent;
    }
}
