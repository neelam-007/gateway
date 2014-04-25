package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy.ServiceDocumentResources;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Module initialization for the GatewayManagementAssertion module.
 */
public class GatewayManagementModuleLifecycle implements ApplicationListener {

    //- PUBLIC

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded( final ApplicationContext context ) {
        if (instance != null) {
            logger.log( Level.WARNING, "Gateway management module is already initialized");
        } else {
            GatewayManagementModuleLifecycle gatewayManagementModuleLifecycle =
                    new GatewayManagementModuleLifecycle(context);
            gatewayManagementModuleLifecycle.initialize();
            instance = gatewayManagementModuleLifecycle;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any global
     * resources that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        GatewayManagementModuleLifecycle instance = GatewayManagementModuleLifecycle.instance;
        if ( instance != null ) {
            logger.log(Level.INFO, "Gateway management module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Gateway management module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                GatewayManagementModuleLifecycle.instance = null;
            }
        }
    }

    public GatewayManagementModuleLifecycle( final ApplicationContext spring ) {
        this.serviceTemplateManager = spring.getBean("serviceTemplateManager", ServiceTemplateManager.class);
        this.serviceManager = spring.getBean("serviceManager", ServiceManager.class);
        this.serviceDocumentManager = spring.getBean("serviceDocumentManager", ServiceDocumentManager.class);
        this.policyVersionManager = spring.getBean("policyVersionManager", PolicyVersionManager.class);
        this.folderManager = spring.getBean("folderManager", FolderManager.class);
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseChangeEvent ) {
            handleLicenceEvent();
        }
        if( event instanceof Started){
            if ( isLicensed() ) {
                installBootstrapServices();
            }
        }
        if(event instanceof AssertionModuleRegistrationEvent){
            AssertionModuleRegistrationEvent regEvent = (AssertionModuleRegistrationEvent)event;
            if (regEvent.getModule() instanceof ModularAssertionModule) {
                final ModularAssertionModule assMod = (ModularAssertionModule)regEvent.getModule();
                Set<? extends Assertion> protos = assMod.getAssertionPrototypes();
                if (protos.size() > 0) {
                    Assertion proto = protos.iterator().next();
                    if (proto.getClass().getClassLoader() == getClass().getClassLoader()) {
                        // Our module has just been registered.
                        installBootstrapServices();
                    }
                }
            }
        }
    }

    //- PACKAGE

    static ServiceTemplate createServiceTemplate() {
        ServiceTemplate template = null;

        try {
            final String policyContents = getDefaultPolicyXml();
            final ServiceDocumentResources resources = ServiceDocumentWsdlStrategy.loadResources(
                    GatewayManagementModuleLifecycle.class.getPackage().getName().replace( '.', '/' ) + "/serviceTemplate/",
                    FAKE_URL_PREFIX,
                    "gateway-management-8_0.wsdl",
                    GatewayManagementModuleLifecycle.class.getClassLoader() );

            template = new ServiceTemplate(
                    "Gateway Management Service",
                    "/wsman",
                    resources.getContent(),
                    resources.getUri(),
                    policyContents,
                    resources.getDependencies(),
                    ServiceType.OTHER_INTERNAL_SERVICE,
                    null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        } catch ( URISyntaxException e ) {
            logger.log( Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e );
        }

        return template;
    }

    static ServiceTemplate createRestServiceTemplate() {
        ServiceTemplate template = null;

        try {
            final String policyContents = getDefaultRestPolicyXml();

            template = new ServiceTemplate(
                    "Gateway REST Management Service",
                    "/restman/*",
                    policyContents,
                    ServiceType.OTHER_INTERNAL_SERVICE,
                    null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        }

        return template;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GatewayManagementModuleLifecycle.class.getName());

    private static final String FAKE_URL_PREFIX = "file://__ssginternal/";

    private static final String BOOTSTRAP_SERVICES_FOLDER = ConfigFactory.getProperty("bootstrap.folder.services");

    private static GatewayManagementModuleLifecycle instance = null;

    private final ServiceTemplateManager serviceTemplateManager;
    private final FolderManager folderManager;
    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final PolicyVersionManager policyVersionManager;
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;

    private ServiceTemplate serviceTemplate;
    private ServiceTemplate restServiceTemplate;

    private void handleLicenceEvent() {
        if ( isLicensed() ) {
            registerServices();
        } else {
            unregisterServices();
        }
    }

    protected void initialize() {
        serviceTemplate = createServiceTemplate();

        restServiceTemplate = createRestServiceTemplate();

        registerServices();
    }

    private void destroy() throws Exception {
        instance.unregisterServices();

        if (applicationEventProxy != null)
            applicationEventProxy.removeApplicationListener(this);
    }

    private boolean isLicensed() {
        LicenseManager licMan = spring.getBean("licenseManager", LicenseManager.class);
        return licMan.isFeatureEnabled(new GatewayManagementAssertion().getFeatureSetName());
    }

    private void registerServices() {
        if ( !isLicensed() ) {
            logger.warning("The Gateway Management assertion module is not licensed. The Gateway Management service will not be available.");
        } else {
            registerService(serviceTemplate);
            registerService(restServiceTemplate);
        }
    }

    private void unregisterServices() {
        unregisterService(serviceTemplate);
        unregisterService(restServiceTemplate);
    }

    private void registerService( final ServiceTemplate svcTemplate ) {
        if (svcTemplate == null) return;

        logger.info("Registering the '" + svcTemplate.getName() + "' service with the gateway (Routing URI = " + svcTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.register(svcTemplate);
    }

    private void unregisterService( final ServiceTemplate svcTemplate ) {
        if (svcTemplate == null) return;

        logger.info("Unregistering the '" + svcTemplate.getName() + "' service with the gateway (Routing URI = " + svcTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.unregister(svcTemplate);
    }

    private static String getDefaultPolicyXml() throws IOException {
        final AuthenticationAssertion authenticationAssertion = new AuthenticationAssertion();
        authenticationAssertion.setIdentityProviderOid( IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID );
        final Assertion allAss = new AllAssertion( Arrays.asList(
                new SslAssertion(),
                new HttpBasic(),
                authenticationAssertion,
                new GatewayManagementAssertion()
        ) );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, outputStream);
        return HexUtils.decodeUtf8(outputStream.toByteArray());
    }

    private static String getDefaultRestPolicyXml() throws IOException {
        final AuthenticationAssertion authenticationAssertion = new AuthenticationAssertion();
        authenticationAssertion.setIdentityProviderOid( IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID );
        final Assertion allAss = new AllAssertion( Arrays.asList(
                new SslAssertion(),
                new HttpBasic(),
                authenticationAssertion,
                new RESTGatewayManagementAssertion()
        ) );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, outputStream);
        return HexUtils.decodeUtf8(outputStream.toByteArray());
    }

    protected void installBootstrapServices() {
        // if no services are installed try installing services
        try {
            AdminInfo.find(false).wrapCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    if (serviceManager.findAll().isEmpty() && BOOTSTRAP_SERVICES_FOLDER != null) {
                        File serviceFolder = new File(BOOTSTRAP_SERVICES_FOLDER);
                        if (!serviceFolder.exists()) return false;
                        if (!serviceFolder.isDirectory()) return false;
                        File[] serviceFiles = serviceFolder.listFiles();
                        if (serviceFiles == null) return false;

                        boolean created = false;
                        for (File serviceFile : serviceFiles) {
                            if (serviceFile.getName().equals("wsman")) {
                                // install wsman service
                                created = installTemplateService(serviceTemplate) || created;
                            } else if (serviceFile.getName().equals("restman")) {
                                // install rest-man service
                                created = installTemplateService(restServiceTemplate) || created;
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

    private boolean installTemplateService(@NotNull ServiceTemplate template) {
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
            return true;

        } catch ( ObjectModelException | IOException e) {
            logger.warning("Error creating internal service " + template.getName() + " caused by:" + e.getMessage());
            return false;
        }

    }
}
