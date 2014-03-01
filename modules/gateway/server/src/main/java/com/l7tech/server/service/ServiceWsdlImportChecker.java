package com.l7tech.server.service;

import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.xml.sax.InputSource;

import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLLocator;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
                try {
                    // Set up input
                    InputSource is = new InputSource();
                    String url = service.getWsdlUrl();
                    if (url != null && url.trim().length() > 0) {
                        is.setSystemId(url);
                    }
                    is.setCharacterStream(new StringReader(service.getWsdlXml()));

                    // Build a map of the service documents in the database
                    final Map<String, ServiceDocument> cached = Functions.toMap(serviceDocumentManager.findByServiceIdAndType(service.getGoid(), "WSDL-IMPORT"), new Functions.Unary<Pair<String, ServiceDocument>, ServiceDocument>() {
                        @Override
                        public Pair<String, ServiceDocument> call(ServiceDocument serviceDocument) {
                            return new Pair<>(serviceDocument.getUri(), serviceDocument);
                        }
                    });

                    // Parse to process imports
                    ResourceTrackingWSDLLocator wloc = new ResourceTrackingWSDLLocator(new CacheCheckingWSDLLocator(is, cached), false, true, true);
                    Wsdl.newInstance(wloc);

                    for (ResourceTrackingWSDLLocator.WSDLResource wsdlResource : wloc.getWSDLResources()) {
                        if(cached.containsKey(wsdlResource.getUri())){
                            continue;
                        }
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
                    //should not log the stack trace here. SSG-7734
                    logger.log(Level.WARNING, "Could not process WSDL imports for service '"+service.getGoid()+"'. Message: " + we.getMessage());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Could not retrieve cached service documents for service '"+service.getGoid()+"'.", e);
                }
            }
        });
    }

    /**
     * This is a wsdl locator that will check a given service document cache to see if the wsdl exist in it. If it does that wsdl is returned
     */
    private class CacheCheckingWSDLLocator implements WSDLLocator {
        @NotNull
        private final InputSource inputSource;
        @NotNull
        private final Map<String, ServiceDocument> cached;
        private String lastResolvedUri = null;

        private CacheCheckingWSDLLocator(@NotNull InputSource inputSource, @NotNull Map<String, ServiceDocument> cached) {
            this.inputSource = inputSource;
            this.cached = cached;
        }

        @Override
        public InputSource getBaseInputSource() {
            lastResolvedUri = getBaseURI();
            return inputSource;
        }

        @Override
        public String getBaseURI() {
            return inputSource.getSystemId();
        }

        /**
         * Resolve a (possibly relative) import.
         *
         * @param parentLocation A URI specifying the location of the document doing the importing. This can be null if
         *                       the import location is not relative to the parent location.
         * @param importLocation A URI specifying the location of the document to import. This might be relative to the
         *                       parent document's location.
         * @return the InputSource object or null if the import cannot be found.
         */
        @Override
        public InputSource getImportInputSource(String parentLocation, String importLocation) {
            InputSource is = null;
            URI resolvedUri;
            lastResolvedUri = importLocation; // ensure set even if not valid

            try {
                if (parentLocation != null) {
                    URI base = new URI(parentLocation);
                    URI relative = new URI(importLocation);
                    resolvedUri = base.resolve(relative);
                } else {
                    resolvedUri = new URI(importLocation);
                }
                lastResolvedUri = resolvedUri.toString();

                //check the cache for the resource
                if (cached.containsKey(resolvedUri.toString())) {
                    is = new InputSource();
                    is.setSystemId(resolvedUri.toString());
                    is.setCharacterStream(new StringReader(cached.get(resolvedUri.toString()).getContents()));
                } else if (resolvedUri.isAbsolute()) {
                    if (!"file".equals(resolvedUri.getScheme())) {
                        is = new InputSource();
                        is.setSystemId(resolvedUri.toString());
                    } else {
                        is = new InputSource();
                        is.setSystemId(resolvedUri.toString());
                        is.setByteStream(new IOExceptionThrowingInputStream(new IOException("Local import not permitted '" + resolvedUri.toString() + "'.")));
                    }
                }
            } catch (URISyntaxException e) {
                //invalid uri. We can return null
            }
            return is;
        }

        @Override
        public String getLatestImportURI() {
            return lastResolvedUri;
        }

        @Override
        public void close() {
        }
    }
}
