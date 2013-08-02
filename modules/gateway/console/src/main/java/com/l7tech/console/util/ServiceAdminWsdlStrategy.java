package com.l7tech.console.util;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.objectmodel.FindException;

import javax.wsdl.WSDLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ServiceAdminWsdlStrategy extends ServiceDocumentWsdlStrategy {

    //- PUBLIC

    public ServiceAdminWsdlStrategy() {
        super(null);
    }

    //- PROTECTED

    @Override
    protected Collection<ServiceDocument> loadServiceDocuments( final PublishedService service ) throws WSDLException {
        Collection<ServiceDocument> serviceDocuments;
        try {
            serviceDocuments = Registry.getDefault().getServiceManager().findServiceDocumentsByServiceID( service.getId() );
            if (!serviceDocuments.isEmpty()) {
                logger.log(Level.FINE, "Service ''{0,number,0}'', has ''{1}'' imports.", new Object[]{service.getGoid(), serviceDocuments.size()});
            } else {
                logger.log(Level.FINE, "Service ''{0,number,0}'', has no available imports.", service.getGoid());
            }
        } catch ( FindException fe ) {
            throw new WSDLException( WSDLException.OTHER_ERROR, "Unable to access documents for service with id '"+service.getGoid()+"'.", fe);
        }

        return serviceDocuments;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServiceAdminWsdlStrategy.class.getName());
}