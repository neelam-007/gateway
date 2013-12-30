/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.esm.server;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.external.assertions.esm.EsmConstants;
import com.l7tech.external.assertions.esm.EsmMetricsAssertion;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.wsdm.QoSMetricsService.*;
import static com.l7tech.server.wsdm.subscription.SubscriptionNotifier.*;

/** @author alex */
public class EsmModuleApplicationListener implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(EsmModuleApplicationListener.class.getName());

    private static final String FAKE_URL_PREFIX = "file://__ssginternal/";

    private static EsmModuleApplicationListener instance = null;

    private final ServiceTemplateManager serviceTemplateManager;
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;

    ServiceTemplate metricsTemplate;
    ServiceTemplate subscriptionsTemplate;

    private EsmApplicationContext esmSpring;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "ESM module is already initialized");
        } else {
            instance = new EsmModuleApplicationListener(context);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "ESM module is shutting down");
            try {
                instance.unregisterServices();
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "ESM module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }

     public void destroy() throws Exception {
        try {
            logger.info("EsmModuleApplicationListener destroy");
        } finally {
            // Unsubscribe ourself from the applicationEventProxy
            if (applicationEventProxy != null)
                applicationEventProxy.removeApplicationListener(this);
        }
    }

      /**
     * Get the current instance, if there is one.
     *
     * @return  the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    public static EsmModuleApplicationListener getInstance() {
        return instance;
    }

    public EsmModuleApplicationListener(ApplicationContext spring) {
        this.serviceTemplateManager = (ServiceTemplateManager)spring.getBean("serviceTemplateManager");
        this.applicationEventProxy = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AssertionModuleRegistrationEvent) {
            initialize();
        } else if (event instanceof AssertionModuleUnregistrationEvent) {
            logger.info("ESM module is unregistering");
            unregisterServices();
        } else if (event instanceof LicenseChangeEvent) {
            handleLicenceEvent();
        } else if (event instanceof Started) {
            // Make sure we initialize properly on startup (too early to get our own registration event)
            initialize();
        }

        if (esmSpring != null) esmSpring.getEsmService().onApplicationEvent(event);
    }

    private void handleLicenceEvent() {
        if (isLicensed())
            initialize();
        else
            unregisterServices();
    }

    private void initialize() {
        if (initialized.getAndSet(true))
            return;
        
        this.esmSpring = EsmApplicationContext.getInstance(spring);

        if (!isLicensed()) {
            logger.warning("The ESM Assertion module is not licensed. ESM services will not be available."); return;
        }

        metricsTemplate = createServiceTemplate(ESM_QOS_METRICS_SERVICE_NAME, ESM_QOS_METRICS_ROOT_WSDL, ESM_QOS_METRICS_URI_PREFIX, null);

        Map<String, String> esmNotifyPolicyTags = new HashMap<String, String>();
        Assertion allAss = new AllAssertion(Arrays.asList(new HttpRoutingAssertion("${esmNotificationUrl}")));
        String polXml;
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            WspWriter.writePolicy(allAss, baos);
            polXml = baos.toString(Charsets.UTF8);
        } catch (IOException e1) {
            throw new RuntimeException("Could not serialize the default policy for ESM subscription notifications.", e1);
        } finally {
            baos.close();
        }

        esmNotifyPolicyTags.put(EsmConstants.POLICY_TAG_ESM_NOTIFICATION, polXml);
        subscriptionsTemplate = createServiceTemplate(ESM_SUBSCRIPTION_SERVICE_NAME, ESM_SUBSCRIPTION_SERVICE_ROOT_WSDL, ESM_SUBSCRIPTION_SERVICE_URI_PREFIX, esmNotifyPolicyTags);

        registerServices();
    }

    private boolean isLicensed() {
        LicenseManager licMan = (LicenseManager) spring.getBean("licenseManager");
        return licMan.isFeatureEnabled(new EsmMetricsAssertion().getFeatureSetName());
    }

    private void registerServices() {
        registerService(metricsTemplate);
        registerService(subscriptionsTemplate);
    }

    private void unregisterServices() {
        unregisterService(metricsTemplate);
        unregisterService(subscriptionsTemplate);
    }


    private ServiceTemplate createServiceTemplate(String serviceName, final String wsdlUrl, String uriPrefix, Map<String, String> policyTags) {
        ServiceTemplate template = null;        
        try {
            String url = FAKE_URL_PREFIX + wsdlUrl;

            DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
            Map<String,String> contents = processor.processDocument( url, new DocumentReferenceProcessor.ResourceResolver(){
                public String resolve(final String resourceUrl) throws IOException {
                    String content = loadMyResource( resourceUrl.substring(FAKE_URL_PREFIX.length()) );
                    return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, false, true);
                }
            } );

            Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                    ResourceTrackingWSDLLocator.toWSDLResources(url, contents, false, false, false);

            List<ServiceDocument> svcDocs = new ArrayList<ServiceDocument>();

            for (ResourceTrackingWSDLLocator.WSDLResource sourceDoc : sourceDocs) {
                ServiceDocument doc = new ServiceDocument();
                doc.setUri(sourceDoc.getUri());
                doc.setType("WSDL-IMPORT");
                doc.setContents(sourceDoc.getWsdl());
                doc.setContentType("text/xml");
                svcDocs.add(doc);
            }

            String policyContents = getDefaultPolicyXml(serviceName);
            template = new ServiceTemplate(serviceName, uriPrefix, contents.get(url), url, policyContents, svcDocs, ServiceType.OTHER_INTERNAL_SERVICE, policyTags);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't load WSDL and/or Policy XML; " + serviceName + " service templates will not be available", e);
        }

        return template;
    }

    private void registerService(ServiceTemplate svcTemplate) {
        if (svcTemplate == null) return;

        logger.info("Registering the '" + svcTemplate.getName() + "' service with the gateway (Routing URI = " + svcTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.register(svcTemplate);
    }

    private void unregisterService(ServiceTemplate svcTemplate) {
        if (svcTemplate == null) return;

        logger.info("Unregistering the '" + svcTemplate.getName() + "' service with the gateway (Routing URI = " + svcTemplate.getDefaultUriPrefix() + ")");
        serviceTemplateManager.unregister(svcTemplate);
    }

    private String loadMyResource(final String what) throws IOException {
        logger.fine("loading wsdl resource: " + what);

        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("resources/" + what));
        return new String(bytes, Charsets.UTF8);
    }

    public String getDefaultPolicyXml(String serviceName) throws IOException {
        AllAssertion allAss = new AllAssertion();

        if (ESM_SUBSCRIPTION_SERVICE_NAME.equals(serviceName))
            allAss.addChild(new EsmSubscriptionAssertion());
        else
            allAss.addChild(new EsmMetricsAssertion());
            
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, baos);
        return baos.toString();
    }
}
