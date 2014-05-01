package com.l7tech.server.service;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Installs template services on startup
 */
public class ServiceTemplateBootstrapService implements PostStartupApplicationListener {

    private static final Logger logger = Logger.getLogger(ServiceTemplateBootstrapService.class.getName());

    private static Map<String,Goid> servicesInstalled = new HashMap<>(); // service name, service id
    private static boolean loadServices = false;


    private static String BOOTSTRAP_SERVICES_FOLDER = ConfigFactory.getProperty("bootstrap.folder.services");

    static void loadFolder(){ // for test coverage
        BOOTSTRAP_SERVICES_FOLDER = ConfigFactory.getProperty("bootstrap.folder.services");
    }

    private final FolderManager folderManager;
    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final PolicyVersionManager policyVersionManager;
    private final ServiceTemplateManager serviceTemplateManager;

    public ServiceTemplateBootstrapService(
            final FolderManager folderManager,
            final ServiceManager serviceManager,
            final ServiceDocumentManager serviceDocumentManager,
            final PolicyVersionManager policyVersionManager,
            final ServiceTemplateManager serviceTemplateManager)
    {
        this.folderManager = folderManager;
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.policyVersionManager = policyVersionManager;
        this.serviceTemplateManager = serviceTemplateManager;
        init();

    }

    protected void init() {
        try {
            loadServices = serviceManager.findAll().isEmpty() && BOOTSTRAP_SERVICES_FOLDER != null;
        } catch (FindException e) {
            logger.warning("Error finding services: " + ExceptionUtils.getMessageWithCause(e));
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseChangeEvent ||
             event instanceof Started ||
             event instanceof AssertionModuleRegistrationEvent){
            installBootstrapServices();
        }
        if( event instanceof Created){
            PersistenceEvent persistenceEvent = (PersistenceEvent)event;
            if( persistenceEvent.getEntity() instanceof PublishedService){
                if(! servicesInstalled.containsKey(((PublishedService) persistenceEvent.getEntity()).getGoid())){
                    loadServices = false;
                }
            }
        }
    }

    protected void installBootstrapServices() {
        // if no services are installed try installing services
        try {
            AdminInfo.find(false).wrapCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    if (loadServices) {
                        File serviceFolder = new File(BOOTSTRAP_SERVICES_FOLDER);
                        if (!serviceFolder.exists()) return false;
                        if (!serviceFolder.isDirectory()) return false;
                        File[] serviceFiles = serviceFolder.listFiles();
                        if (serviceFiles == null) return false;

                        for (File serviceFile : serviceFiles) {
                            ServiceTemplate template = serviceTemplateManager.findByAutoProvisionName(serviceFile.getName());
                            if (template != null && !servicesInstalled.containsKey(template.getName())) {
                                Goid serviceId = installTemplateService(template);
                                if(serviceId != null)
                                    servicesInstalled.put(template.getName(), serviceId);
                            }
                        }
                    }

                    return false;
                }
            }).call();
        } catch ( Exception e ) {
            logger.warning(ExceptionUtils.getMessageWithCause(e));
        }
    }

    private Goid installTemplateService(@NotNull ServiceTemplate template) {
        try {

            PublishedService service = new PublishedService();
            service.setFolder(folderManager.findRootFolder());

            service.setName(template.getName());
            service.getPolicy().setXml(template.getDefaultPolicyXml());
            service.setRoutingUri(template.getDefaultUriPrefix());
            service.setSoap(template.isSoap());
            if(!template.isSoap()){
                // set supported http methods
                service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            }
            service.setInternal(true);
            service.parseWsdlStrategy(new ServiceDocumentWsdlStrategy(template.getServiceDocuments()));
            service.setWsdlUrl(template.getServiceDescriptorUrl());
            service.setWsdlXml(template.getServiceDescriptorXml());

            service.setDisabled(false);

            // SAVING NEW SERVICE
            logger.fine("Saving new PublishedService");

            final Policy policy = service.getPolicy();
            if(policy != null && policy.getGuid() == null) {
                UUID guid = UUID.randomUUID();
                policy.setGuid(guid.toString());
            }

            // Services may not be saved for the first time with the trace bit set.
            service.setTracingEnabled(false);

            final Goid serviceGoid = serviceManager.save(service);
            if (policy != null) {
                policyVersionManager.checkpointPolicy(policy, true, true);
            }

            if(template.isSoap()){

                Collection<ServiceDocument> existingServiceDocuments = serviceDocumentManager.findByServiceId(serviceGoid);
                for (ServiceDocument serviceDocument : existingServiceDocuments) {
                    serviceDocumentManager.delete(serviceDocument);
                }
                for (ServiceDocument serviceDocument : template.getServiceDocuments()) {
                    serviceDocument.setGoid(ServiceDocument.DEFAULT_GOID);
                    serviceDocument.setServiceId(serviceGoid);
                    serviceDocumentManager.save(serviceDocument);
                }
            }

            serviceManager.createRoles(service);


            logger.info("Internal service " + template.getName() + " created");
            return service.getGoid();

        } catch ( ObjectModelException | IOException e) {
            logger.warning("Error creating internal service " + template.getName() + " caused by:" + e.getMessage());
            return null;
        }
    }
}
