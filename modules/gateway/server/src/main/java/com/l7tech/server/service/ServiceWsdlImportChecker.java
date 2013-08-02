package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.Config;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.InputSource;

import javax.wsdl.WSDLException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Check for any non-cached WSDL imports and save to the DB.
 *
 * @author Steve Jones
 */
public class ServiceWsdlImportChecker implements InitializingBean {

    //- PUBLIC

    /**
     * Create Service WSDL import checker.
     *
     * @param transactionManager     The transaction manager to use
     * @param serviceManager         The service manager to use
     * @param serviceDocumentManager The service document manager to use
     */
    public ServiceWsdlImportChecker(final Config config,
                                    final PlatformTransactionManager transactionManager,
                                    final ServiceManager serviceManager,
                                    final ServiceDocumentManager serviceDocumentManager) {
        this.config = config;
        this.transactionManager = transactionManager;
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
    }

    /**
     * Checks for any missing WSDL imports on startup.
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        checkServiceWsdlImports();
    }

    /**
     * Check imports are available for existing SOAP published services.
     *
     * <p>If imports are not locally available they will be saved to the DB.</p>
     */
    public void checkServiceWsdlImports() {
        if (Boolean.valueOf( config.getProperty( "wsdlImportFixupEnabled" ) )) {
            logger.info("Checking WSDL imports for SOAP services.");

            // Load all published services.
            Collection<PublishedService> services = Collections.EMPTY_LIST;
            try {
                services = serviceManager.findAll();
            }
            catch(FindException fe) {
                logger.log(Level.WARNING, "Unable to load services, skipping WSDL import check.", fe);
            }

            for (PublishedService publishedService : services) {
                if (publishedService.isDisabled() ||
                    !publishedService.isSoap()) continue; // ignore disabled and XML services

                checkWsdlImports(publishedService);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServiceWsdlImportChecker.class.getName());

    private final Config config;
    private final PlatformTransactionManager transactionManager;
    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;

    /**
     * See if we need to check imports for the given service.
     *
     * <p>If there are no service documents we have to check the WSDL since
     * there is no way to know if it has missing imports or just no imports.</p>
     */
    private boolean checkImportsForService(final PublishedService service) {
        boolean checkImports = false;
        try {
            Collection<ServiceDocument> serviceDocuments =
                    serviceDocumentManager.findByServiceIdAndType(service.getGoid(), "WSDL-IMPORT");

            if (serviceDocuments.isEmpty()) {
                checkImports = true;
            }
        } catch(FindException fe) {
            logger.log(Level.WARNING, "Unable to load service documents.", fe);
        }
        return checkImports;
    }

    /**
     * Create a ServiceDocument from the given WSDL resource.
     */
    private ServiceDocument buildServiceDocument(Goid serviceId, ResourceTrackingWSDLLocator.WSDLResource wsdlResource) {
        ServiceDocument serviceDocument = new ServiceDocument();
        
        serviceDocument.setServiceId(serviceId);
        serviceDocument.setUri(wsdlResource.getUri());
        serviceDocument.setContentType("text/xml");
        serviceDocument.setType("WSDL-IMPORT");
        serviceDocument.setContents(wsdlResource.getWsdl());

        return serviceDocument;
    }

    /**
     * Check the imports for the given service. 
     */
    private void checkWsdlImports(final PublishedService service) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (checkImportsForService(service)) {
                    try {
                        // Set up input
                        InputSource is = new InputSource();
                        String url = service.getWsdlUrl();
                        if (url != null && url.trim().length() > 0) {
                            is.setSystemId(url);
                        }
                        is.setCharacterStream(new StringReader(service.getWsdlXml()));

                        // Parse to process imports
                        ResourceTrackingWSDLLocator wloc = new ResourceTrackingWSDLLocator(Wsdl.getWSDLLocator(is, false), false, true, true);
                        Wsdl.newInstance(wloc);

                        for (ResourceTrackingWSDLLocator.WSDLResource wsdlResource : wloc.getWSDLResources()) {
                            try {
                                logger.info("Saving imported WSDL '"+wsdlResource.getUri()+"' for service '"+service.getGoid()+"'.");
                                serviceDocumentManager.save(buildServiceDocument(service.getGoid(), wsdlResource));
                            } catch(SaveException se) {
                                logger.log(Level.WARNING, "Error saving service document '"+wsdlResource.getUri()+"' for service '"+service.getGoid()+"'.", se);
                                status.setRollbackOnly();
                                break;
                            }
                        }
                    } catch (WSDLException we) {
                        logger.log(Level.WARNING, "Could not process WSDL imports for service '"+service.getGoid()+"'.", we);
                    }
                }
            }
        });
    }
}
