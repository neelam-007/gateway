package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy.ServiceDocumentResources;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
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

            // Need to pre cache the Jaxb context. If it is already loaded this will do nothing.
            // If the context does not get loaded on gateway startup then it will fail to properly load when the service is executed.
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                //Set the context class loaded this way. It will force it to use the ModularAssertionClassLoader
                Thread.currentThread().setContextClassLoader(GatewayManagementAssertion.class.getClassLoader());
                MarshallingUtils.getJAXBContext();
            } catch (JAXBException e) {
                logger.log( Level.WARNING, "Gateway management module failed to properly load the JAXBContext: " + e.getMessage(), e);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
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
        if ( event instanceof LicenseChangeEvent ) {
            handleLicenceEvent();
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
                    "gateway-management-8_2_00.wsdl",
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

    private static GatewayManagementModuleLifecycle instance = null;

    private final ServiceTemplateManager serviceTemplateManager;
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

    private void initialize() {
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
            registerService(serviceTemplate,"wsman");
            registerService(restServiceTemplate,"restman");
        }
    }

    private void unregisterServices() {
        unregisterService(serviceTemplate);
        unregisterService(restServiceTemplate);
    }

    private void registerService( final ServiceTemplate svcTemplate, String autoProvisionName ) {
        if (svcTemplate == null) return;

        logger.info("Registering the '" + svcTemplate.getName() + "' service with the gateway (Routing URI = " + svcTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.register(svcTemplate,autoProvisionName );
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
        authenticationAssertion.setIdentityProviderOid(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        final AllAssertion basic = new AllAssertion(Arrays.asList(new SslAssertion(), new HttpBasic()));
        final OneOrMoreAssertion mutualOrBasic = new OneOrMoreAssertion(Arrays.asList(new SslAssertion(true), basic));

        //Returning error is the message size limit is exceeded
        //Adds the Limit Message Size assertion (SSG-10893)
        final RequestSizeLimit requestSizeLimit = new RequestSizeLimit();
        requestSizeLimit.setLimit("${gateway.restman.request.message.maxSize}");
        //The response to return if the message size is too large
        final CustomizeErrorResponseAssertion customizeErrorResponseAssertion = new CustomizeErrorResponseAssertion();
        customizeErrorResponseAssertion.setContentType("application/xml; charset=UTF-8");
        customizeErrorResponseAssertion.setHttpStatus("400");
        customizeErrorResponseAssertion.setContent( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                                                    "<l7:Error xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                                                    "    <l7:Type>InvalidRequest</l7:Type>\n" +
                                                    "    <l7:Detail>the specified maximum data size limit would be exceeded</l7:Detail>\n" +
                                                    "</l7:Error>");
        final AllAssertion returnErrorMessageAndStop = new AllAssertion(Arrays.asList(customizeErrorResponseAssertion, new FalseAssertion()));
        final OneOrMoreAssertion checkMessageSize = new OneOrMoreAssertion(Arrays.asList(
                new CommentAssertion("Check the request message size. If the maximum message size needs to be increased change the 'restman.request.message.maxSize' cluster property"),
                requestSizeLimit,
                returnErrorMessageAndStop));

        final Assertion allAss = new AllAssertion(Arrays.asList(
                mutualOrBasic,
                authenticationAssertion,
                checkMessageSize,
                new RESTGatewayManagementAssertion()
        ));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, outputStream);
        return HexUtils.decodeUtf8(outputStream.toByteArray());
    }
}
