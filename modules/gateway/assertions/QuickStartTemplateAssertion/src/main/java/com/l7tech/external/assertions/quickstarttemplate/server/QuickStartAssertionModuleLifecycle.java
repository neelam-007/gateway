package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.springframework.context.ApplicationContext;
import java.util.logging.Logger;

public class QuickStartAssertionModuleLifecycle {
    public static final String PROVIDED_FRAGMENT_FOLDER_GOID = "2a97ddf9a6e77162832b9c27bc8f57e0";
    private static final Logger LOGGER = Logger.getLogger(QuickStartAssertionModuleLifecycle.class.getName());
    private static QuickStartEncapsulatedAssertionLocator assertionLocator = null;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (assertionLocator == null) {
            final EncapsulatedAssertionConfigManager eacm = context.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
            final FolderManager fm = context.getBean("folderManager", FolderManager.class);
            assertionLocator = new QuickStartEncapsulatedAssertionLocator(eacm, fm, new Goid(PROVIDED_FRAGMENT_FOLDER_GOID));
        }
    }

    public static synchronized void onModuleUnloaded() {
        // Do nothing; these shared resources are used by both the template and the documentation assertion.
    }

    public static QuickStartEncapsulatedAssertionLocator getEncapsulatedAssertionLocator() {
        return assertionLocator;
    }

}
