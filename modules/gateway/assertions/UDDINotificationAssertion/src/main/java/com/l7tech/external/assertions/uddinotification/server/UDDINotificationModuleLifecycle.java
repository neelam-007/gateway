package com.l7tech.external.assertions.uddinotification.server;

import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy.ServiceDocumentResources;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.external.assertions.uddinotification.UDDINotificationAssertion;

import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

/**
 * Module initialization for the UDDINotificationAssertion module.
 */
public class UDDINotificationModuleLifecycle implements ApplicationListener {

    //- PUBLIC

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded( final ApplicationContext context ) {
        if (instance != null) {
            logger.log( Level.WARNING, "UDDI notification module is already initialized");
        } else {
            UDDINotificationModuleLifecycle uddiNotificationModuleLifecycle =
                    new UDDINotificationModuleLifecycle(context);
            uddiNotificationModuleLifecycle.initialize();
            instance = uddiNotificationModuleLifecycle;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any global
     * resources that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        UDDINotificationModuleLifecycle instance = UDDINotificationModuleLifecycle.instance;
        if ( instance != null ) {
            logger.log(Level.INFO, "UDDI notification module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "UDDI notification module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                UDDINotificationModuleLifecycle.instance = null;
            }
        }
    }

    public UDDINotificationModuleLifecycle( final ApplicationContext spring ) {
        this.serviceTemplateManager = spring.getBean("serviceTemplateManager", ServiceTemplateManager.class);
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseChangeEvent) {
            handleLicenceEvent();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(UDDINotificationModuleLifecycle.class.getName());

    private static final String FAKE_URL_PREFIX = "file://__ssginternal/";

    private static UDDINotificationModuleLifecycle instance = null;

    private final ServiceTemplateManager serviceTemplateManager;
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;

    private ServiceTemplate serviceTemplate;    

    private void handleLicenceEvent() {
        if ( isLicensed() ) {
            registerServices();
        } else {
            unregisterServices();
        }
    }

    private void initialize() {
        serviceTemplate = createServiceTemplate();

        registerServices();
    }

    private void destroy() throws Exception {
        instance.unregisterServices();

        if (applicationEventProxy != null)
            applicationEventProxy.removeApplicationListener(this);
    }

    private boolean isLicensed() {
        LicenseManager licMan = (LicenseManager) spring.getBean("licenseManager");
        return licMan.isFeatureEnabled(new UDDINotificationAssertion().getFeatureSetName());
    }

    private void registerServices() {
        if ( !isLicensed() ) {
            logger.warning("The UDDI Notification Assertion module is not licensed. UDDI Notification service will not be available.");
        } else {
            registerService(serviceTemplate);
        }
    }

    private void unregisterServices() {
        unregisterService(serviceTemplate);
    }

    private ServiceTemplate createServiceTemplate() {
        ServiceTemplate template = null;
        
        try {
            final String policyContents = getDefaultPolicyXml();
            final ServiceDocumentResources resources = ServiceDocumentWsdlStrategy.loadResources(
                    UDDINotificationModuleLifecycle.class.getPackage().getName().replace( '.', '/' ) + "/serviceTemplate/",
                    FAKE_URL_PREFIX,
                    "uddi_subr_v3_service.wsdl",
                    UDDINotificationModuleLifecycle.class.getClassLoader() );

            template = new ServiceTemplate(
                    "UDDI Notification Service",
                    "/uddi/notification",
                    resources.getContent(),
                    resources.getUri(),
                    policyContents,
                    resources.getDependencies(),
                    ServiceType.OTHER_INTERNAL_SERVICE,
                    null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        } catch ( URISyntaxException e ) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        }

        return template;
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

    private String getDefaultPolicyXml() throws IOException {
        Assertion allAss = new AllAssertion(Arrays.asList(new UDDINotificationAssertion()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, outputStream);
        return HexUtils.decodeUtf8(outputStream.toByteArray());
    }
}
