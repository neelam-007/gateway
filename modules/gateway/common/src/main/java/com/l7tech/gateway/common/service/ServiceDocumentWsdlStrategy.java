package com.l7tech.gateway.common.service;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ServiceDocumentWsdlStrategy implements PublishedService.WsdlStrategy {

    //- PUBLIC

    public ServiceDocumentWsdlStrategy( final Collection<ServiceDocument> serviceDocuments ) {
        this.serviceDocuments = serviceDocuments==null ?
                Collections.<ServiceDocument>emptyList() :
                new ArrayList<ServiceDocument>(serviceDocuments);
    }

    @Override
    public final Wsdl parseWsdl( final PublishedService service,
                                 final String uri,
                                 final String wsdl ) throws WSDLException {
        return Wsdl.newInstance(Wsdl.getWSDLLocator(uri, buildContent(uri, wsdl, service), getLogger()));
    }

    public static Wsdl parseWsdl( final PublishedService service,
                                  final Collection<ServiceDocument> serviceDocuments ) throws WSDLException {
        return parseWsdl( service.getWsdlUrl(), service.getWsdlXml(), serviceDocuments );            
    }

    public static Wsdl parseWsdl( final String uri,
                                  final String wsdl,
                                  final Collection<ServiceDocument> serviceDocuments ) throws WSDLException {
        return new ServiceDocumentWsdlStrategy( serviceDocuments ).parseWsdl( null, uri, wsdl );
    }

    public static List<ServiceDocument> fromWsdlResources( final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs ) {
        List<ServiceDocument> svcDocs = new ArrayList<ServiceDocument>();

        for (ResourceTrackingWSDLLocator.WSDLResource sourceDoc : sourceDocs) {
            ServiceDocument doc = new ServiceDocument();
            doc.setUri(sourceDoc.getUri());
            doc.setType("WSDL-IMPORT");
            doc.setContents(sourceDoc.getWsdl());
            doc.setContentType("text/xml");
            svcDocs.add(doc);
        }
        return svcDocs;
    }

    /**
     * Load service documents from the classpath.
     *
     * @param resourceBase The base resource path
     * @param urlBase The base URL
     * @param startDocument The relative or absolute URL for the WSDL
     * @param classLoader The resource classloader
     * @return The service document resources
     * @throws IOException If an IO error occurs
     * @throws URISyntaxException If the given urlBase or startDocument URLs are invalid
     */
    @NotNull
    public static ServiceDocumentResources loadResources( @NotNull final String resourceBase,
                                                          @NotNull final String urlBase,
                                                          @NotNull final String startDocument,
                                                          @NotNull final ClassLoader classLoader ) throws IOException, URISyntaxException {
        final String url = new URI( urlBase ).resolve( new URI( startDocument ) ).toString();
        final WsdlEntityResolver entityResolver = new WsdlEntityResolver(true);
        final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
        final Map<String,String> contents = processor.processDocument( url, new DocumentReferenceProcessor.ResourceResolver(){
            @Override
            public String resolve(final String resourceUrl) throws IOException {
                String resource = resourceUrl;
                if ( resource.startsWith(urlBase) ) {
                    resource = resourceUrl.substring(urlBase.length());
                }
                String content = loadResource( classLoader, resourceBase, resource, entityResolver );
                return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, entityResolver.failOnMissing(), false, true);
            }
        } );

        final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                ResourceTrackingWSDLLocator.toWSDLResources(url, contents, false, false, false);

        return new ServiceDocumentResources(
                url,
                contents.get( url ),
                ServiceDocumentWsdlStrategy.fromWsdlResources( sourceDocs ) );
    }

    public static final class ServiceDocumentResources {
        private final String uri;
        private final String content;
        private final List<ServiceDocument> dependencies;

        public ServiceDocumentResources( @NotNull final String uri,
                                         @NotNull final String content,
                                         @NotNull final List<ServiceDocument> dependencies ) {
            this.uri = uri;
            this.content = content;
            this.dependencies = dependencies;
        }

        @NotNull
        public String getUri() {
            return uri;
        }

        @NotNull
        public String getContent() {
            return content;
        }

        @NotNull
        public List<ServiceDocument> getDependencies() {
            return dependencies;
        }
    }

    //- PROTECTED

    protected Collection<ServiceDocument> loadServiceDocuments( final PublishedService service ) throws WSDLException {
        return serviceDocuments;
    }

    protected Logger getLogger() {
        return logger;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceDocumentWsdlStrategy.class.getName() );

    private final Collection<ServiceDocument> serviceDocuments;

    private Map<String,String> buildContent( final String baseUri,
                                             final String baseContent,
                                             final PublishedService service ) throws WSDLException {
        Map<String,String> content = new HashMap<String,String>();

        for ( ServiceDocument doc : loadServiceDocuments( service ) ) {
            content.put( doc.getUri(), doc.getContents() );
        }

        content.put( baseUri, baseContent );

        return content;
    }

    private static String loadResource( final ClassLoader classLoader,
                                        final String resourceBase,
                                        final String resource,
                                        final EntityResolver resolver ) throws IOException {
        byte[] bytes = null;

        InputSource in = null;
        try {
            in = resolver.resolveEntity( null, resource );
            if ( in != null ) {
                bytes = IOUtils.slurpStream( in.getByteStream() );
            }
        } catch (SAXException e) {
            throw new IOException("Cannot load resource '"+resource+"'.", e);
        } finally {
            if ( in != null ) {
                ResourceUtils.closeQuietly( in.getByteStream() );
            }
        }

        if ( bytes == null ) {
            String resourcePath = resource;
            int dirIndex = resource.lastIndexOf( '/' );
            if ( dirIndex > 0 ) {
                resourcePath = resource.substring( dirIndex+1 );
            }

            logger.fine("Loading WSDL resource '" + resource + "' as '" + resourcePath +"'.");

            final String resourceName = resourceBase + resourcePath;
            final URL resourceUrl = classLoader.getResource( resourceName );
            if ( resourceUrl == null ) {
                throw new IOException( "Missing resource '"+resourceName+"'" );
            }
            bytes = IOUtils.slurpUrl( resourceUrl );
        }

        return HexUtils.decodeUtf8( bytes );
    }
}