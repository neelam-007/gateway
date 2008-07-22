package com.l7tech.server.service;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.StringReader;
import javax.wsdl.WSDLException;

import org.xml.sax.InputSource;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.wsdl.Wsdl;

/**
 * PublishedService WSDL strategy for safe import handling.
 *
 * <p>This will attempt to retrieve WSDLs from the DB rather than from a URI.</p>
 *
 * @author Steve Jones
 */
public class SafeWsdlPublishedService implements PublishedService.WsdlStrategy {

    //- PUBLIC

    public SafeWsdlPublishedService(Collection<ServiceDocument> serviceDocuments) {
        this.serviceDocuments = serviceDocuments;
    }

    public Wsdl parseWsdl(final String uri, final String wsdl) throws WSDLException {
        Wsdl parsedWsdl;

        try {
            parsedWsdl = Wsdl.newInstance(Wsdl.getWSDLLocator(uri, buildContent(uri, wsdl, serviceDocuments), logger));
        }
        catch (WSDLException we) {
            logger.log(Level.WARNING, "Error parsing WSDL.", we);
            InputSource source = new InputSource();
            source.setSystemId(uri);
            source.setCharacterStream(new StringReader(wsdl));        
            parsedWsdl = Wsdl.newInstance(uri, source);
        }
        
        return parsedWsdl;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SafeWsdlPublishedService.class.getName());

    private final Collection<ServiceDocument> serviceDocuments;

    private Map<String,String> buildContent( String baseUri, String baseContent, Collection<ServiceDocument> docs ) {
        Map<String,String> content = new HashMap<String,String>();

        for ( ServiceDocument doc : docs ) {
            content.put( doc.getUri(), doc.getContents() );            
        }

        content.put( baseUri, baseContent );

        return content;
    }
}
