package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.sun.research.ws.wadl.Application;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.glassfish.jersey.server.model.ExtendedResource;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * This is a custom wadl creator. This is used so that we can control the namespaces added to the wadl document.
 */
@Singleton
@Path("generatewadl.wadl")
@ExtendedResource
public class WadlGeneratorResource {

    @Context
    private WadlApplicationContext wadlContext;

    @Produces({"application/vnd.sun.wadl+xml", "application/xml"})
    @GET
    public synchronized Response getWadl(@Context UriInfo uriInfo) {
        try {
            //create the wadl
            Application application = wadlContext.getApplication(uriInfo, false).getApplication();

            // Remove the jersey forced application docs
            application.getDoc().clear();

            //marshal the wadl to xml
            byte[] wadlXmlRepresentation;
            try {
                final Marshaller marshaller = wadlContext.getJAXBContext().createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
                    @Override
                    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                        switch (namespaceUri) {
                            case "http://wadl.dev.java.net/2009/02":
                                return "wadl";
                            case "http://ns.l7tech.com/2010/04/gateway-management":
                                return "l7";
                            case "http://www.w3.org/2001/XMLSchema":
                                return "xsd";
                            default:
                                return suggestion;
                        }
                    }

                    @Override
                    public String[] getPreDeclaredNamespaceUris() {
                        return new String[]{"http://ns.l7tech.com/2010/04/gateway-management", "http://www.w3.org/2001/XMLSchema"};
                    }

                });
                //marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://wadl.dev.java.net/2009/02 wadl.xsd");
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                marshaller.marshal(application, os);
                wadlXmlRepresentation = os.toByteArray();
                os.close();
            } catch (Exception e) {
                throw new ProcessingException("Could not marshal the wadl Application.", e);
            }

            return Response.ok(new ByteArrayInputStream(wadlXmlRepresentation)).build();
        } catch (Exception e) {
            throw new ProcessingException("Error generating /application.wadl.", e);
        }
    }
}
