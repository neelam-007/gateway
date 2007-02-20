package com.l7tech.server.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;
import java.net.URISyntaxException;
import javax.wsdl.xml.WSDLLocator;

import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import com.l7tech.service.ServiceDocument;

/**
 * Resolver for a services documents.
 *
 * @author Steve Jones
 */
public class ServiceDocumentResolver implements EntityResolver, WSDLLocator {

    //- PUBLIC

    /**
     * 
     */
    public ServiceDocumentResolver(String uri, String wsdl, Collection<ServiceDocument> documents) {
        this.uri = uri;
        this.wsdl = wsdl;
        this.serviceDocuments = new ArrayList(documents);
    }

    /**
     * EntityResolver
     */
    public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
        String entity = null;
        if (uri.equals(systemId)) {
            entity = wsdl;
        } else {
            ServiceDocument serviceDocument = getDocumentByUri(systemId);
            if (serviceDocument != null) {
                entity = serviceDocument.getContents();
            }
        }

        if (entity == null)
            throw new IOException("Could not resolve system identifier '"+systemId+"'.");

        InputSource is = new InputSource();
        is.setSystemId(systemId);
        is.setCharacterStream(new StringReader(entity));        
        return is;
    }

    /**
     * WSDLLocator
     */
    public InputSource getBaseInputSource() {
        InputSource inputSource = new InputSource();
        inputSource.setSystemId(uri);
        inputSource.setCharacterStream(new StringReader(wsdl));
        return inputSource;
    }

    /**
     * WSDLLocator
     */
    public String getBaseURI() {
        lastUri = uri;
        return uri;
    }

    /**
     * WSDLLocator
     */
    public InputSource getImportInputSource(final String parentLocation, final String importLocation) {
        InputSource is = null;
        try {
            URI resolvedUri = null;
            lastUri = importLocation; // ensure set even if not valid

            if (parentLocation != null) {
                URI base = new URI(parentLocation);
                URI relative = new URI(importLocation);
                resolvedUri = base.resolve(relative);
            }
            else {
                resolvedUri = new URI(importLocation);
            }

            lastUri = resolvedUri.toString();
            ServiceDocument serviceDocument = getDocumentByUri(resolvedUri.toString());

            logger.log(Level.INFO, "Resolving WSDL uri '"+resolvedUri.toString()+"', document found '"+(serviceDocument != null)+"'.");

            if (serviceDocument != null) {
                is = new InputSource();
                is.setSystemId(serviceDocument.getUri());
                is.setCharacterStream(new StringReader(serviceDocument.getContents()));
            }
        }
        catch (URISyntaxException use) {
            // of interest?
        }
        return is;
    }

    /**
     * WSDLLocator
     */
    public String getLatestImportURI() {
        return lastUri;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServiceDocumentResolver.class.getName());

    private final String uri;
    private final String wsdl;
    private final Collection<ServiceDocument> serviceDocuments;
    private String lastUri = null;

    /**
     * Get the document for the given uri 
     */
    private ServiceDocument getDocumentByUri(final String uri) {
        ServiceDocument serviceDocument = null;

        for (ServiceDocument candidate : serviceDocuments) {
            if (candidate.getUri().equals(uri)) {
                serviceDocument = candidate;
                break;
            }
        }

        return serviceDocument;
    }
}
