package com.l7tech.external.assertions.uddinotification.server;

import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.external.assertions.uddinotification.UDDINotificationAssertion;
import com.l7tech.xml.DocumentReferenceProcessor;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.xml.sax.EntityResolver;

/**
 * Module initialization for the UDDINotifiationAssertion module.
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
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
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
        this.serviceTemplateManager = (ServiceTemplateManager) spring.getBean("serviceTemplateManager", ServiceTemplateManager.class);
        this.applicationEventProxy = (ApplicationEventProxy) spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseEvent ) {
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
            String url = FAKE_URL_PREFIX + "uddi_subr_v3_service.wsdl";

            final EntityResolver doctypeResolver = new WsdlEntityResolver();
            final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
            final Map<String,String> contents = processor.processDocument( url, new DocumentReferenceProcessor.ResourceResolver(){
                @Override
                public String resolve(final String resourceUrl) throws IOException {
                    String resource = resourceUrl;
                    if ( resource.startsWith(FAKE_URL_PREFIX) ) {
                        resource = resourceUrl.substring(FAKE_URL_PREFIX.length());
                    }
                    String content = loadMyResource( resource );
                    return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, doctypeResolver, false, true);
                }
            } );

            final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                    ResourceTrackingWSDLLocator.toWSDLResources(url, contents, false, false, false);

            final List<ServiceDocument> svcDocs = ServiceDocumentWsdlStrategy.fromWsdlResources( sourceDocs );

            String policyContents = getDefaultPolicyXml();
            template = new ServiceTemplate("UDDI Notification Service", "/uddi/notification", contents.get(url), url, policyContents, svcDocs, ServiceType.OTHER_INTERNAL_SERVICE, null);
        } catch (IOException e) {
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

    private String loadMyResource( final String resource ) throws IOException {
        String resourcePath = resource;
        int dirIndex = resource.lastIndexOf( '/' );
        if ( dirIndex > 0 ) {
            resourcePath = resource.substring( dirIndex+1 );
        }

        logger.fine("Loading wsdl resource '" + resource + "' as '" + resourcePath +"'.");        

        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("serviceTemplate/" + resourcePath));
        return HexUtils.decodeUtf8(bytes);
    }

    private String getDefaultPolicyXml() throws IOException {
        Assertion allAss = new AllAssertion(Arrays.asList(new UDDINotificationAssertion()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, baos);
        return HexUtils.decodeUtf8(baos.toByteArray());
    }
}
