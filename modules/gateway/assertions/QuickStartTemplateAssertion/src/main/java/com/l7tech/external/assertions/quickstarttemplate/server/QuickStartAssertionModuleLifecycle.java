package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartJsonServiceInstaller;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPublishedServiceLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilder;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.GatewayState;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public class QuickStartAssertionModuleLifecycle {
    private static final Logger logger = Logger.getLogger(QuickStartAssertionModuleLifecycle.class.getName());
    private static final String PROVIDED_FRAGMENT_FOLDER_GOID = "2a97ddf9a6e77162832b9c27bc8f57e0";

    private static QuickStartEncapsulatedAssertionLocator assertionLocator = null;
    private static QuickStartPublishedServiceLocator serviceLocator = null;
    private static QuickStartServiceBuilder serviceBuilder = null;
    private static QuickStartJsonServiceInstaller jsonServiceInstaller = null;

    @SuppressWarnings("unused")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        final FolderManager folderManager = context.getBean("folderManager", FolderManager.class);
        if (assertionLocator == null) {
            final EncapsulatedAssertionConfigManager eacm = context.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
            assertionLocator = new QuickStartEncapsulatedAssertionLocator(eacm, folderManager, new Goid(PROVIDED_FRAGMENT_FOLDER_GOID));
        }

        if (serviceLocator == null) {
            final ServiceManager serviceManager = context.getBean("serviceManager", ServiceManager.class);
            serviceLocator = new QuickStartPublishedServiceLocator(serviceManager);
        }

        if (serviceBuilder == null) {
            final ServiceCache ServiceCache = context.getBean("serviceCache", ServiceCache.class);
            serviceBuilder = new QuickStartServiceBuilder(ServiceCache, folderManager, serviceLocator, assertionLocator);
        }

        if (jsonServiceInstaller == null) {
            final ServiceManager serviceManager = context.getBean("serviceManager", ServiceManager.class);
            final PolicyVersionManager policyVersionManager = context.getBean("policyVersionManager", PolicyVersionManager.class);
            jsonServiceInstaller = new QuickStartJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager);
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////
        // install customer custom json files
        //////////////////////////////////////////////////////////////////////////////////////////////////
        // Check if the Gateway is ready for messages
        final GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
        if (gatewayState.isReadyForMessages()) {
            // our module is dynamically loaded after the gateway startup
            installJsonServices();
        } else {
            // wait for the gateway to become ready for messages (our module is loaded with gateway startup)
            final ApplicationEventProxy applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
            final ApplicationListener applicationListener = event -> {
                if (event instanceof ReadyForMessages) {
                    installJsonServices();
                }
            };
            applicationEventProxy.addApplicationListener(applicationListener);
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * Utility method that actually invokes {@link QuickStartJsonServiceInstaller#installJsonServices()} and
     * loges any exception thrown (though there shouldn't be any at this point).
     */
    private static void installJsonServices() {
        try {
            jsonServiceInstaller.installJsonServices();
        } catch (final Throwable e) {
            logger.log(Level.SEVERE, "Unhandled exception while installing JSON services: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @SuppressWarnings("unused")
    public static synchronized void onModuleUnloaded() {
        // Do nothing; these shared resources are used by both the template and the documentation assertion.
    }

    static QuickStartEncapsulatedAssertionLocator getEncapsulatedAssertionLocator() {
        return assertionLocator;
    }

    static QuickStartPublishedServiceLocator getPublishedServiceLocator() {
        return serviceLocator;
    }

    static QuickStartServiceBuilder getServiceBuilder() {
        return serviceBuilder;
    }
}
