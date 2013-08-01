package com.l7tech.external.assertions.siteminder.server;

import com.l7tech.external.assertions.siteminder.SiteMinderExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

public class SiteMinderModuleLoadListener {

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static SiteMinderExternalReferenceFactory externalReferenceFactory;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        registerExternalReferenceFactory(context);

    }

    public static synchronized void onModuleUnloaded() {
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory(); // ensure not already registered

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new SiteMinderExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory!=null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
