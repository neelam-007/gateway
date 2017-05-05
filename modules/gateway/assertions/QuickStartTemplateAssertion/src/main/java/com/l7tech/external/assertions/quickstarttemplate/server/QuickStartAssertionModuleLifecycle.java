package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPublishedServiceLocator;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.service.ServiceManager;
import org.springframework.context.ApplicationContext;

public class QuickStartAssertionModuleLifecycle {
    private static final String PROVIDED_FRAGMENT_FOLDER_GOID = "2a97ddf9a6e77162832b9c27bc8f57e0";
    private static QuickStartEncapsulatedAssertionLocator assertionLocator = null;
    private static QuickStartPublishedServiceLocator serviceLocator = null;

    @SuppressWarnings("unused")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (assertionLocator == null) {
            final EncapsulatedAssertionConfigManager eacm = context.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
            final FolderManager fm = context.getBean("folderManager", FolderManager.class);
            assertionLocator = new QuickStartEncapsulatedAssertionLocator(eacm, fm, new Goid(PROVIDED_FRAGMENT_FOLDER_GOID));
        }

        if (serviceLocator == null) {
            final ServiceManager serviceManager = context.getBean("serviceManager", ServiceManager.class);
            serviceLocator = new QuickStartPublishedServiceLocator(serviceManager);
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

}
