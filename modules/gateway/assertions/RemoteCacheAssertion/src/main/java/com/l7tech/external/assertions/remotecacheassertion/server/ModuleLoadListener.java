package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.*;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 15/11/11
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModuleLoadListener {
    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static RemoteCacheExternalReferenceFactory storeExternalReferenceFactory;
    private static RemoteCacheExternalReferenceFactory lookupExternalReferenceFactory;
    private static RemoteCacheExternalReferenceFactory removeExternalReferenceFactory;
    private static RemoteCacheExternalReferenceFactory externalReferenceFactory;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        registerExternalReferenceFactory(context);
    }

    public static synchronized void onModuleUnloaded() {
        RemoteCachesManagerImpl.shutdown();
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory();

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        storeExternalReferenceFactory = new RemoteCacheExternalReferenceFactory(RemoteCacheStoreAssertion.class, RemoteCacheReference.class);
        policyExporterImporterManager.register(storeExternalReferenceFactory);

        lookupExternalReferenceFactory = new RemoteCacheExternalReferenceFactory(RemoteCacheLookupAssertion.class, RemoteCacheReference.class);
        policyExporterImporterManager.register(lookupExternalReferenceFactory);

        removeExternalReferenceFactory = new RemoteCacheExternalReferenceFactory(RemoteCacheRemoveAssertion.class, RemoteCacheReference.class);
        policyExporterImporterManager.register(removeExternalReferenceFactory);

        externalReferenceFactory = new RemoteCacheExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && storeExternalReferenceFactory != null) {
            policyExporterImporterManager.unregister(storeExternalReferenceFactory);
            storeExternalReferenceFactory = null;
        }
        if (policyExporterImporterManager != null && lookupExternalReferenceFactory != null) {
            policyExporterImporterManager.unregister(lookupExternalReferenceFactory);
            lookupExternalReferenceFactory = null;
        }
        if (policyExporterImporterManager != null && removeExternalReferenceFactory != null) {
            policyExporterImporterManager.unregister(removeExternalReferenceFactory);
            removeExternalReferenceFactory = null;
        }
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
