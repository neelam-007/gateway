package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
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
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseEvent) {
            handleLicenceEvent();
        }
    }

    //- PACKAGE

    static ServiceTemplate createServiceTemplate() {
        ServiceTemplate template = null;

        try {
            String url = FAKE_URL_PREFIX + "gateway-management.wsdl";

            final WsdlEntityResolver entityResolver = new WsdlEntityResolver(true);
            final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
            final Map<String,String> contents = processor.processDocument( url, new DocumentReferenceProcessor.ResourceResolver(){
                @Override
                public String resolve(final String resourceUrl) throws IOException {
                    String resource = resourceUrl;
                    if ( resource.startsWith(FAKE_URL_PREFIX) ) {
                        resource = resourceUrl.substring(FAKE_URL_PREFIX.length());
                    }
                    String content = loadMyResource( resource, entityResolver );
                    return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, entityResolver.failOnMissing(), false, true);
                }
            } );

            final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                    ResourceTrackingWSDLLocator.toWSDLResources(url, contents, false, false, false);

            final List<ServiceDocument> svcDocs = ServiceDocumentWsdlStrategy.fromWsdlResources( sourceDocs );

            String policyContents = getDefaultPolicyXml();
            template = new ServiceTemplate("Gateway Management Service", "/wsman", contents.get(url), url, policyContents, svcDocs, ServiceType.OTHER_INTERNAL_SERVICE, null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; service template will not be available", e);
        }

        return template;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GatewayManagementModuleLifecycle.class.getName());

    private static final String FAKE_URL_PREFIX = "file://__ssginternal/";

    private static GatewayManagementModuleLifecycle instance = null;

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
        LicenseManager licMan = spring.getBean("licenseManager", LicenseManager.class);
        return licMan.isFeatureEnabled(new GatewayManagementAssertion().getFeatureSetName());
    }

    private void registerServices() {
        if ( !isLicensed() ) {
            logger.warning("The Gateway Management assertion module is not licensed. The Gateway Management service will not be available.");
        } else {
            registerService(serviceTemplate);
        }
    }

    private void unregisterServices() {
        unregisterService(serviceTemplate);
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

    private static String loadMyResource( final String resource, final EntityResolver resolver ) throws IOException {
        byte[] bytes = null;

        InputSource in = null;
        try {
            in = resolver.resolveEntity( null, resource );
            if ( in != null ) {
                bytes = IOUtils.slurpStream( in.getByteStream() );
            }
        } catch (SAXException e) {
            throw new IOException("Cannot load resource '"+resource+"'.", e);
        } finally {
            if ( in != null ) {
                ResourceUtils.closeQuietly( in.getByteStream() );
            }
        }

        if ( bytes == null ) {
            String resourcePath = resource;
            int dirIndex = resource.lastIndexOf( '/' );
            if ( dirIndex > 0 ) {
                resourcePath = resource.substring( dirIndex+1 );
            }

            logger.fine("Loading WSDL resource '" + resource + "' as '" + resourcePath +"'.");

            final String resourceName = "serviceTemplate/" + resourcePath;
            final URL resourceUrl = GatewayManagementModuleLifecycle.class.getResource(resourceName);
            if ( resourceUrl == null ) {
                throw new IOException( "Missing resource '"+resourceName+"'" );
            }
            bytes = IOUtils.slurpUrl( resourceUrl );
        }

        return HexUtils.decodeUtf8(bytes);
    }

    private static String getDefaultPolicyXml() throws IOException {
        final AuthenticationAssertion authenticationAssertion = new AuthenticationAssertion();
        authenticationAssertion.setIdentityProviderOid( IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID );
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
}
