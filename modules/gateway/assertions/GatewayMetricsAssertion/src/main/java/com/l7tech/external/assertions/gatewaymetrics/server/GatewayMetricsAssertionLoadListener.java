package com.l7tech.external.assertions.gatewaymetrics.server;

import com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 3/18/13
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayMetricsAssertionLoadListener {

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static GatewayMetricsExternalReferenceFactory gatewayMetricsExternalReferenceFactory;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        // Register ExternalReferenceFactory
        registerExternalReferenceFactory(context);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory();

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        gatewayMetricsExternalReferenceFactory = new GatewayMetricsExternalReferenceFactory();
        policyExporterImporterManager.register(gatewayMetricsExternalReferenceFactory);

    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && gatewayMetricsExternalReferenceFactory != null) {
            policyExporterImporterManager.unregister(gatewayMetricsExternalReferenceFactory);
            gatewayMetricsExternalReferenceFactory = null;
        }
    }
}