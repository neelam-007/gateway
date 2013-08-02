package com.l7tech.server.uddi;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.ExceptionUtils;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Check if update of a services WSDL is permitted.
 *
 * <p>This code is refactored from ServiceAdminImpl as of 5.3.</p>
 */
public class ServiceWsdlUpdateChecker {

    //- PUBLIC

    public ServiceWsdlUpdateChecker( final ServiceManager serviceManager,
                                     final UDDIServiceControlManager uddiServiceControlManager ) {
        this.serviceManager = serviceManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
    }

    /**
     * Check if the WSDL for the given service can be updated.
     *
     * @param service The service to check.
     * @return True if updates are permitted
     * @throws UpdateException If an error occurs during the check
     */
    public boolean isWsdlUpdatePermitted( final PublishedService service ) throws UpdateException {
        return isWsdlUpdatePermitted( service, false );
    }

    /**
     * Check if the WSDL for the given service can be updated.
     *
     * <p>WARNING: This resets the service WSDL, the caller is responsible for
     * reset of any WSDL dependencies (service documents)</p>
     *
     * <p>WARNING: The reset behaviour relies on the given service not being
     * attached to the hibernate session. The caller must ensure that the
     * hibernate session will contain the original (non-updated) service and
     * service documents.</p>
     *
     * <p>WARNING: The reset behaviour relies on the given service having been
     * configured with an appropriate wsdl strategy so that is accesses it's
     * WSDL dependencies appropriately.
     *
     * @param service The service to check.
     * @param resetWsdlXml True to reset the services WSDL to the original value.
     * @return True if updates are permitted
     * @throws UpdateException If an error occurs during the check
     */
    public boolean isWsdlUpdatePermitted( final PublishedService service, final boolean resetWsdlXml ) throws UpdateException {
        boolean updatePermitted = true;
        
        //check if it's under UDDI Control
        try {
            final UDDIServiceControl uddiServiceControl = uddiServiceControlManager.findByPublishedServiceGoid(service.getGoid());
            if ( uddiServiceControl != null && uddiServiceControl.isUnderUddiControl() ) {
                updatePermitted = false;

                if ( resetWsdlXml ) {
                    final PublishedService original = serviceManager.findByPrimaryKey(service.getGoid());
                    if(original != null){
                        //check the WSDL
                        try {
                            final String updatedHash = service.parsedWsdl().getHash();
                            final String originalHash = original.parsedWsdl().getHash();
                            if ( !updatedHash.equals(originalHash) ) {
                                logger.log( Level.INFO, "Published Service id(#" + service.getGoid()+" is under UDDI control. The updated WSDL will be ignored");
                                service.setWsdlXml( original.getWsdlXml() );
                            }
                        } catch ( WSDLException e) {
                            throw new UpdateException("Error parsing WSDL: " + ExceptionUtils.getMessage(e));
                        } catch ( IOException e) {
                            throw new UpdateException("Error computing WSDL hash: " + ExceptionUtils.getMessage(e));
                        }
                    }
                }
            }
        } catch ( FindException fe ) {
            throw new UpdateException(fe);
        }

        return updatePermitted;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceWsdlUpdateChecker.class.getName() );

    private final ServiceManager serviceManager;
    private final UDDIServiceControlManager uddiServiceControlManager;

}
