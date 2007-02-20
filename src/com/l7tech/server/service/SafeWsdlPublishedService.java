package com.l7tech.server.service;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.StringReader;
import javax.wsdl.WSDLException;

import org.xml.sax.InputSource;

import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceDocument;
import com.l7tech.common.xml.Wsdl;

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
        Wsdl parsedWsdl = null;

        try {
            parsedWsdl = Wsdl.newInstance(new ServiceDocumentResolver(uri, wsdl, serviceDocuments));
        }
        catch (WSDLException we) {
            logger.log(Level.WARNING, "Error parsing WSDL.", we);
            parsedWsdl = Wsdl.newInstance(uri, new InputSource(new StringReader(wsdl)));
        }
        
        return parsedWsdl;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SafeWsdlPublishedService.class.getName());

    private final Collection<ServiceDocument> serviceDocuments;
}
