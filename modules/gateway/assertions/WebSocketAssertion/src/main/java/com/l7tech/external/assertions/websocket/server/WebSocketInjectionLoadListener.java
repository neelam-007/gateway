package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 8/28/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketInjectionLoadListener {
    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static WebSocketConnectionExternalReferenceFactory externalReferenceFactory;

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

        externalReferenceFactory = new WebSocketConnectionExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);

    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
