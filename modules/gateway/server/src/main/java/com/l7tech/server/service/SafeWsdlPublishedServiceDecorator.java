package com.l7tech.server.service;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.util.Decorator;
import com.l7tech.objectmodel.FindException;

/**
 * Decorator for PublishedServices that adds support for parsing WSDLs with imports.
 *
 * @author Steve Jones
 */
public class SafeWsdlPublishedServiceDecorator implements Decorator<PublishedService> {

    //- PUBLIC

    /**
     * Create a SafeWsdlPublishedServiceDecorator with the given ServiceDocumentManager.
     *
     * <p>The given ServiceDocumentManager will be used to find any WSDL imports for
     * docorated PublishedServices.</p>
     */
    public SafeWsdlPublishedServiceDecorator(final ServiceDocumentManager serviceDocumentManager) {        
        this.serviceDocumentManager = serviceDocumentManager;
    }

    /**
     * Decorate the given  PublishedService.
     *
     * @param undecorated The PublishedService to decorate.
     * @return The decorated PublishedService
     */
    public PublishedService decorate(final PublishedService undecorated) {
        PublishedService publishedService = undecorated;

        if (!isDecorated(publishedService)) {
            publishedService = buildSafeWsdlPublishedService(publishedService);
        }

        return publishedService;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SafeWsdlPublishedServiceDecorator.class.getName());

    private final ServiceDocumentManager serviceDocumentManager;

    /**
     * Check if already decorated.
     */
    private boolean isDecorated(final PublishedService publishedService) {
        boolean decorated = false;

        if (publishedService.parseWsdlStrategy() instanceof SafeWsdlPublishedService) {
            decorated = true;
        }
        else {
            PublishedService decoratedPS = publishedService;

            while (decoratedPS instanceof Decorated) {
                decoratedPS = (PublishedService) ((Decorated)decoratedPS).undecorate();

                if (publishedService.parseWsdlStrategy() instanceof SafeWsdlPublishedService) {
                    decorated = true;
                    break;
                }
            }
        }

        return decorated;
    }

    /**
     * Wrap the given PublishedService if it has any imports.
     *
     * <p>If there are no imports this either means that the services WSDL has no imports
     * or that we are unable to automatically fetch and store them (in which case we fall
     * back to fetching from the remote server[s])</p>
     */
    private PublishedService buildSafeWsdlPublishedService(final PublishedService undecorated) {
        PublishedService publishedService = undecorated;
        try {
            Collection<ServiceDocument> serviceDocuments = serviceDocumentManager.findByServiceIdAndType(undecorated.getOid(), "WSDL-IMPORT");
            if (!serviceDocuments.isEmpty()) {
                logger.log(Level.FINE, "Service ''{0,number,0}'', has ''{1}'' imports.", new Object[]{undecorated.getOid(), serviceDocuments.size()});
                publishedService.parseWsdlStrategy(new SafeWsdlPublishedService(serviceDocuments));
            } else {
                logger.log(Level.FINE, "Service ''{0,number,0}'', has no available imports.", undecorated.getOid());
            }
        }
        catch(FindException fe) {
            logger.log(Level.WARNING, "Unable to access documents for service with id '"+undecorated.getOid()+"'.", fe);
        }
        return publishedService;
    }
}
