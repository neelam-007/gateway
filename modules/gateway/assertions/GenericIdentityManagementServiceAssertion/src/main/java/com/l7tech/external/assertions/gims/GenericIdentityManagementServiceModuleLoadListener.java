package com.l7tech.external.assertions.gims;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.service.ServiceTemplateManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 1/15/13
 */
public class GenericIdentityManagementServiceModuleLoadListener implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(GenericIdentityManagementServiceModuleLoadListener.class.getName());

    public static final String DEFAULT_POLICY_FILE = "gims_default_policy.xml";
    public static final String SERVICE_TEMPLATE_NAME = "Generic Identity Management Service";
    public static final String DEFAULT_URI_PREFIX = "/gims/1/*";

    private static GenericIdentityManagementServiceModuleLoadListener _instance;

    private final ServiceTemplateManager serviceTemplateManager;
    private final ServiceTemplate serviceTemplate;
    private final ApplicationEventProxy applicationEventProxy;
    private final LicenseManager licenseManager;


    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (_instance != null) {
            logger.log(Level.WARNING, "GenericIdentityManagementServiceModuleLoadListener is already initialized");
        } else {
            GenericIdentityManagementServiceModuleLoadListener moduleLoadListener = new GenericIdentityManagementServiceModuleLoadListener(context);
            moduleLoadListener.registerServiceTemplate();
            _instance = moduleLoadListener;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded
     */
    public static synchronized void onModuleUnloaded() {
        if (_instance != null) {
            logger.log(Level.INFO, "GenericIdentityManagementServiceModuleLoadListener is shutting down");
            try {
                _instance.destroy();
            } catch (final Exception e) {
                logger.log(Level.WARNING, "GenericIdentityManagementServiceModuleLoadListener threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                _instance = null;
            }
        }
    }

    public GenericIdentityManagementServiceModuleLoadListener(ApplicationContext applicationContext) {
        serviceTemplateManager = getBean(applicationContext, "serviceTemplateManager", ServiceTemplateManager.class);
        applicationEventProxy = getBean(applicationContext, "applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
        licenseManager = getBean(applicationContext, "licenseManager", LicenseManager.class);
        serviceTemplate = createServiceTemplate(DEFAULT_POLICY_FILE, SERVICE_TEMPLATE_NAME, DEFAULT_URI_PREFIX);
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof LicenseChangeEvent) {
            registerServiceTemplate();
        }
    }

    private void registerServiceTemplate() {
        if ( serviceTemplate != null ) {
            if(licenseManager.isFeatureEnabled(new GenericIdentityManagementServiceAssertion().getFeatureSetName())) {
                logger.info("Registering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
                serviceTemplateManager.register(serviceTemplate);
            }
            else {
                logger.warning("The Generic Identity Management Service assertion module is not licensed. The Generic Identity Management Service will not be available.");
            }
        }
    }

    private ServiceTemplate createServiceTemplate(final String policyResourceFile, final String serviceName, final String uriPrefix) {
        ServiceTemplate template = null;
        try {
            final String policyContents = readPolicyFile(policyResourceFile);
            template = new ServiceTemplate(
                    serviceName,
                    uriPrefix,
                    policyContents,
                    ServiceType.OTHER_INTERNAL_SERVICE,
                    null);
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error creating service template. " + serviceName + " will not be available.", ExceptionUtils.getDebugException(e));
        }
        return template;
    }

    /**
     * Helper method that reads policy file from the resource
     * @param policyResourceFile  - policy file name
     * @return  String containing template policy
     * @throws IOException
     */
    private String readPolicyFile(final String policyResourceFile) throws IOException {
        final InputStream resourceAsStream = GenericIdentityManagementServiceModuleLoadListener.class.getClassLoader().getResourceAsStream(DEFAULT_POLICY_FILE);
        if (resourceAsStream == null) {
            throw new IOException("Policy resource file does not exist: " + policyResourceFile);
        }
        final byte[] fileBytes = IOUtils.slurpStream(resourceAsStream);
        resourceAsStream.close();
        return new String(fileBytes);
    }

    /**
     * Removes application listener and unregister service template
     * @throws Exception
     */
    private void destroy() throws Exception {
        applicationEventProxy.removeApplicationListener(this);
        if ( serviceTemplate != null ) {
            logger.info("Unregistering the '" + serviceTemplate.getName() + "' service with the gateway (Routing URI = " + serviceTemplate.getDefaultUriPrefix() + ")");
            serviceTemplateManager.unregister(serviceTemplate);
        }
    }


    /**
     * Helper template method to instantiate a proper bean object from a class
     * @param beanFactory
     * @param beanName
     * @param beanClass
     * @param <T> type returned
     * @return
     */
    private static <T> T getBean(final BeanFactory beanFactory, final String beanName, final Class<T> beanClass) {
        final T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass())) {
            return got;
        }
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);
    }
}
