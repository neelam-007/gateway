package com.l7tech.external.assertions.siteminder.server;

import com.l7tech.external.assertions.siteminder.*;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

public class SiteMinderModuleLoadListener {
    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static SiteMinderExternalReferenceFactory externalReferenceFactory; // external reference factory for SiteMinderCheckProtectedAssertion
    private static SiteMinderExternalReferenceFactory changePasswordExternalReferenceFactory;
    private static SiteMinderExternalReferenceFactory enableUserExternalReferenceFactory;

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

        changePasswordExternalReferenceFactory= new SiteMinderExternalReferenceFactory(
                SiteMinderChangePasswordAssertion.class, SiteMinderExternalReference.class);
        policyExporterImporterManager.register(changePasswordExternalReferenceFactory);

        enableUserExternalReferenceFactory= new SiteMinderExternalReferenceFactory(
                SiteMinderEnableUserAssertion.class, SiteMinderExternalReference.class);
        policyExporterImporterManager.register(enableUserExternalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null) {
            if (externalReferenceFactory != null) {
                policyExporterImporterManager.unregister(externalReferenceFactory);
                externalReferenceFactory = null;
            }

            if (changePasswordExternalReferenceFactory != null) {
                policyExporterImporterManager.unregister(changePasswordExternalReferenceFactory);
                changePasswordExternalReferenceFactory = null;
            }

            if (enableUserExternalReferenceFactory != null) {
                policyExporterImporterManager.unregister(enableUserExternalReferenceFactory);
                enableUserExternalReferenceFactory = null;
            }
        }
    }
}