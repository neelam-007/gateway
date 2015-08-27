package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for working with Resources and ResourceSets.
 */
class ResourceHelper {

    //- PACKAGE

    static final String DTD_TYPE = "dtd";
    static final String POLICY_TYPE = "policy";
    static final String POLICY_EXPORT_TYPE = "policyexport";
    static final String ENCASS_EXPORT_TYPE = "encassexport";
    static final String WSDL_TYPE = "wsdl";
    static final String SCHEMA_TYPE = "xmlschema";    

    static final String POLICY_TAG = "policy";
    static final String WSDL_TAG = "wsdl";

    Map<String,ResourceSet> getResourceSetMap( final List<ResourceSet> resourceSets ) throws ResourceFactory.InvalidResourceException {
        Map<String,ResourceSet> resourceSetMap = new HashMap<String,ResourceSet>();

        if ( resourceSets != null ) {
            for ( final ResourceSet resourceSet : resourceSets ) {
                final String tag = resourceSet.getTag();
                if ( tag == null || tag.trim().isEmpty() ) {
                    throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "missing or empty resource set tag");
                }

                if ( resourceSetMap.put( tag, resourceSet ) != null ) {
                    throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "duplicate resource set tag '"+tag+"'");
                }
            }
        }

        return resourceSetMap;
    }

    Resource getResource( final Map<String,ResourceSet> resourceSetMap,
                          final String tag,
                          final String type,
                          final boolean required,
                          final Functions.UnaryThrows<String,String,IOException> resourceResolver ) throws ResourceFactory.InvalidResourceException {
        final Collection<Resource> resources = getResources( resourceSetMap, tag, required, resourceResolver );

        if ( resources.size() > 1 ) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "expected one resource with tag '"+tag+"'");
        }

        final Resource resource = resources.iterator().next();
        if ( type != null && !type.equals(resource.getType()) ) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "expected one resource with tag '"+tag+"' of type '"+type+"'");
        }

        return resource;
    }

    /**
     * Get the resources with a particular tag, with the root resource first.
     *
     * @param resourceSetMap The resource sets
     * @param tag The tag for the containing resource set
     * @param required True to fail if no resources are found
     * @param resourceResolver Resolver to use to access external resources by URL
     * @return The collection of resources (may be empty but not null)
     * @throws ResourceFactory.InvalidResourceException If an error occurs
     * @see #getResourceSetMap
     */
    Collection<Resource> getResources( final Map<String, ResourceSet> resourceSetMap,
                                       final String tag,
                                       final boolean required,
                                       final Functions.UnaryThrows<String,String,IOException> resourceResolver ) throws ResourceFactory.InvalidResourceException {
        final ArrayList<Resource> resources = new ArrayList<Resource>();

        final ResourceSet resourceSet = resourceSetMap.get( tag );
        if ( resourceSet != null && tag.equals( resourceSet.getTag() ) ) {
            final String rootUrl = resourceSet.getRootUrl();
            if ( resourceSet.getResources() != null ) {
                for ( final Resource resource : resourceSet.getResources() ) {
                    if ( rootUrl != null && !rootUrl.startsWith("#") && rootUrl.equals(resource.getSourceUrl()) ) {
                        resources.add( 0, resource );
                    } else if ( rootUrl != null && rootUrl.startsWith("#") && rootUrl.substring(1).equals(resource.getId()) ) {
                        resources.add( 0, resource );
                    } else {
                        resources.add( resource );
                    }
                }
                if ( rootUrl == null && resources.size() > 1 ) {
                    throw new ResourceFactory.InvalidResourceException(
                            ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "missing rootUrl for resource with tag '"+tag+"'");
                } else if ( rootUrl != null && (resources.isEmpty() || !rootUrl.equals(resources.get( 0 ).getSourceUrl())) ) {
                    throw new ResourceFactory.InvalidResourceException(
                            ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "rootUrl does not match any resource for tag '"+tag+"'");
                }
            } else if ( rootUrl != null && resourceResolver != null ) {
                fetchResources( resources, rootUrl, resourceResolver );
            }
        }

        if ( required && resources.isEmpty() ) {
            throw new ResourceFactory.InvalidResourceException(
                    ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES,
                    "missing resource with tag '"+tag+"'");
        }

        return resources;
    }

    String getType( final String uri,
                    final String contents,
                    final String defaultType ) {
        try {
            return getType( uri, contents, defaultType, false );
        } catch ( ResourceFactory.InvalidResourceException e ) {
            throw new ResourceFactory.ResourceAccessException( e ); // should not occur
        }
    }

    String getType( final String uri,
                    final String contents,
                    final String defaultType,
                    final boolean failOnError ) throws ResourceFactory.InvalidResourceException {
        String type = defaultType;

        if ( uri != null && uri.endsWith( ".wsdl" )) {
            type = ResourceHelper.WSDL_TYPE;
        } else if ( uri != null && uri.endsWith( ".xsd" )) {
            type = ResourceHelper.SCHEMA_TYPE;
        } else {
            String namespace = null;
            final XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setXMLReporter( SILENT_REPORTER );
            xif.setXMLResolver( FAILING_RESOLVER );
            XMLStreamReader reader = null;
            try {
                reader = xif.createXMLStreamReader( new StringReader(contents) );
                while( reader.hasNext() ) {
                    int eventType = reader.next();
                    if ( eventType == XMLStreamReader.START_ELEMENT ) {
                        namespace = reader.getNamespaceURI();
                        break;
                    }
                }
            } catch ( XMLStreamException e ) {
                if ( failOnError ) {
                    throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "resource error '"+e.getMessage()+"' when processing '"+uri+"'");
                }
            } finally {
                ResourceUtils.closeQuietly( reader );
            }

            if ( namespace != null ) {
                if ( XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(namespace) ) {
                    type = ResourceHelper.SCHEMA_TYPE;
                } else if ( NAMESPACE_WSDL.equals(namespace) ) {
                    type = ResourceHelper.WSDL_TYPE;
                }
            }                                                      
        }

        return type;
    }

    //- PRIVATE

    private static final String NAMESPACE_WSDL = "http://schemas.xmlsoap.org/wsdl/";

    private static final XMLReporter SILENT_REPORTER = new XMLReporter() {
        @Override
        public void report( final String message, final String errorType, final Object relatedInformation, final Location location ) throws XMLStreamException {
            throw new XMLStreamException(message, location);
        }
    };

    private static final XMLResolver FAILING_RESOLVER = new XMLResolver() {
        @Override
        public Object resolveEntity( final String publicID, final String systemID, final String baseURI, final String namespace ) throws XMLStreamException {
            throw new XMLStreamException("External entity access forbidden '"+systemID+"' relative to '"+baseURI+"'.");
        }
    };

    private void fetchResources( final Collection<Resource> resources,
                                 final String uri,
                                 final Functions.UnaryThrows<String,String,IOException> resourceResolver ) throws ResourceFactory.InvalidResourceException {
        try {
            final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
            final EntityResolver entityResolver = new ResourceHelperEntityResolver( resourceResolver );
            final Map<String,String> urisToResources =
                    processor.processDocument( uri, new ResourceHelperResourceResolver(entityResolver, resourceResolver) );


            final Collection<ResourceTrackingWSDLLocator.WSDLResource> wsdls = ResourceTrackingWSDLLocator.toWSDLResources(uri, urisToResources, true, false, false);
            for ( final ResourceTrackingWSDLLocator.WSDLResource wsdlResource : wsdls ) {
                final Resource resource = ManagedObjectFactory.createResource();
                resource.setSourceUrl( wsdlResource.getUri() );
                resource.setContent( wsdlResource.getWsdl() );
                resource.setType( getType( wsdlResource.getUri(), wsdlResource.getWsdl(), WSDL_TYPE, true ) );
                resources.add( resource );
            }
        } catch ( IOException e ) {
            throw new ResourceFactory.InvalidResourceException(
                    ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                    "error accessing resource set with url '"+uri+"', due to '"+ ExceptionUtils.getMessage(e) +"'" );
        }
    }

    private static final class ResourceHelperEntityResolver implements EntityResolver {
        private final EntityResolver catalogResolver;
        private final Functions.UnaryThrows<String,String,IOException> resourceResolver;

        private ResourceHelperEntityResolver( final Functions.UnaryThrows<String,String,IOException> resourceResolver ) {
            this.catalogResolver = new WsdlEntityResolver(true);
            this.resourceResolver = resourceResolver;
        }

        @Override
        public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
            InputSource inputSource = catalogResolver.resolveEntity( publicId, systemId );

            if ( inputSource == null ) {
                final String resource = resourceResolver.call( systemId ) ;
                if ( resource == null ) {
                    throw new IOException("Could not resolve entity '"+systemId+"'.");
                }

                inputSource = new InputSource();
                inputSource.setPublicId( publicId );
                inputSource.setSystemId( systemId );
                inputSource.setCharacterStream( new StringReader(resource) );
            }

            return inputSource;
        }
    }

    private static final class ResourceHelperResourceResolver implements DocumentReferenceProcessor.ResourceResolver {
        private final EntityResolver entityResolver;
        private final Functions.UnaryThrows<String,String,IOException> resourceResolver;

        private ResourceHelperResourceResolver( final EntityResolver entityResolver,
                                                final Functions.UnaryThrows<String,String,IOException> resourceResolver ) {
            this.entityResolver = entityResolver;
            this.resourceResolver = resourceResolver;
        }

        @Override
        public String resolve( final String importLocation ) throws IOException {
            String resource = null;
            
            final String resolvedResource = resourceResolver.call(importLocation);
            if ( resolvedResource != null ) {
                resource = ResourceTrackingWSDLLocator.processResource(importLocation, resolvedResource, entityResolver, false, true);
            }

            if ( resource == null) {
                throw new IOException("Resource not found '"+importLocation+"'.");
            }

            return resource;
        }
    }
}
