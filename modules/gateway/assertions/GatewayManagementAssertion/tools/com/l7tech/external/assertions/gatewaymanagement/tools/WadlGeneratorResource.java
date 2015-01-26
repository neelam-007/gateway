package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.util.Functions;
import com.sun.research.ws.wadl.*;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.glassfish.jersey.server.ContainerRequest;
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
import java.util.Iterator;
import java.util.List;

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
    public synchronized Response getWadl(@Context UriInfo uriInfo, @Context ContainerRequest containerRequest) {
        //get the current version
        final RestManVersion currentVersion;
        if (containerRequest.getProperty("RestManVersion") != null && containerRequest.getProperty("RestManVersion") instanceof RestManVersion) {
            currentVersion = (RestManVersion) containerRequest.getProperty("RestManVersion");
        } else {
            currentVersion = RestManVersion.VERSION_1_0;
        }

        try {
            //create the wadl
            Application application = wadlContext.getApplication(uriInfo, false).getApplication();

            // Remove the jersey forced application docs
            application.getDoc().clear();

            //filter out things from newer versions
            for (final Resources resources : application.getResources()) {
                for (final Iterator<Resource> resourceIterator = resources.getResource().iterator(); resourceIterator.hasNext(); ) {
                    final Resource resource = resourceIterator.next();

                    //filter out parameters depending on the version
                    filterResourceParams(currentVersion, resource);

                    if (!supportsVersion(currentVersion, resource.getDoc())) {
                        resourceIterator.remove();
                    } else {
                        filterResourceMethods(currentVersion, resource);
                    }
                }
            }

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

    /**
     * This will remove Template parameters from resources in version 1.0.1 and greats. The template params are moved to the method requests instead. SSG-10082
     *
     * @param currentVersion The current version being processed.
     * @param resource The resource to filter.
     */
    private void filterResourceParams(RestManVersion currentVersion, Resource resource) {
        if(RestManVersion.VERSION_1_0_1.compareTo(currentVersion) <= 0) {
            //need to remove path params from resources, they are in the requests. Only for version above 1.0.1
            for(final Iterator<Param> paramIterator = resource.getParam().iterator(); paramIterator.hasNext(); ) {
                final Param param = paramIterator.next();
                if(ParamStyle.TEMPLATE.equals(param.getStyle())) {
                    paramIterator.remove();
                }
            }
            //remove from any sub-resources
            for (final Object methodOrResource : resource.getMethodOrResource()) {
                if (methodOrResource instanceof Resource) {
                    filterResourceParams(currentVersion, (Resource) methodOrResource);
                }
            }
        }
    }

    /**
     * Checks the list of docs to see if they contain the introduced 'since' version. And checks if this version is supported by the current version
     * @param currentVersion The current version
     * @param docs The list of docs to look for the 'since' version in
     * @return true if the version is supported false otherwise
     */
    private boolean supportsVersion(final RestManVersion currentVersion, final List<Doc> docs) {
        final Doc sinceDoc = Functions.grepFirst(docs, new Functions.Unary<Boolean, Doc>() {
            @Override
            public Boolean call(Doc doc) {
                return "since".equals(doc.getTitle());
            }
        });
        final RestManVersion restManVersion = sinceDoc == null ? null : RestManVersion.fromString((String) sinceDoc.getContent().get(0));
        return restManVersion == null || currentVersion.compareTo(restManVersion) >= 0;
    }

    /**
     * Filters resource methods and sub resources removing versions that are not supported.
     * @param currentVersion The current version
     * @param resource The resource to filter
     */
    private void filterResourceMethods(RestManVersion currentVersion, Resource resource) {
        for (final Iterator<Object> methodOrResourceIterator = resource.getMethodOrResource().iterator(); methodOrResourceIterator.hasNext(); ) {
            final  Object methodOrResource = methodOrResourceIterator.next();
            if (methodOrResource instanceof Resource) {
                if (supportsVersion(currentVersion, ((Resource) methodOrResource).getDoc())) {
                    filterResourceMethods(currentVersion, (Resource) methodOrResource);
                }
            } else if (methodOrResource instanceof Method) {
                if (!supportsVersion(currentVersion, ((Method) methodOrResource).getDoc())) {
                    methodOrResourceIterator.remove();
                } else if (((Method) methodOrResource).getRequest() != null && ((Method) methodOrResource).getRequest().getParam() != null) {
                    for (final Iterator<Param> paramIterator = ((Method) methodOrResource).getRequest().getParam().iterator(); paramIterator.hasNext(); ) {
                        final Param param = paramIterator.next();
                        if (!supportsVersion(currentVersion, param.getDoc())) {
                            paramIterator.remove();
                        }
                    }
                    //remove request if it is empty
                    if((((Method) methodOrResource).getRequest().getRepresentation() == null || ((Method) methodOrResource).getRequest().getRepresentation().isEmpty())
                            && (((Method) methodOrResource).getRequest().getParam() == null || ((Method) methodOrResource).getRequest().getParam().isEmpty())
                            && (((Method) methodOrResource).getRequest().getDoc() == null || ((Method) methodOrResource).getRequest().getDoc().isEmpty())) {
                        ((Method) methodOrResource).setRequest(null);
                    }
                }
            }
        }
    }
}
