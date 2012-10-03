package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ApiPortalIntegrationModule extends LifecycleBean implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(ApiPortalIntegrationModule.class.getName());


    public ApiPortalIntegrationModule(String name, Logger logger, String licenseFeature, LicenseManager licenseManager) {
        super(name, logger, licenseFeature, licenseManager);

        // Include additional Sprint objects as needed
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);

    }

    static ApiPortalIntegrationModule createModule(ApplicationContext appContext) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);

        // need to add new featureSet?
        return new ApiPortalIntegrationModule("API portal integration module", logger, GatewayFeatureSets.SERVICE_L7RAWTCP_MESSAGE_INPUT, licenseManager);
//        return new ApiPortalIntegrationModule("API portal ontegration module", logger, GatewayFeatureSets.SERVICE_L7RAWTCP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
//            try {
//
//
//                // TBD: initialize API Portal module
//
//
//
//            } catch (FindException e) {
//                logger.log(Level.SEVERE, "Unable to access initial raw connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
//            }
        }
    }


}
