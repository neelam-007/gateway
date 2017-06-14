package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by huaal03 on 2017-06-05.
 */
public class LogMessageToSysLogModuleLoadListener {
    private static final Logger logger = Logger.getLogger(LogMessageToSysLogModuleLoadListener.class.getName());

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static LogMessageToSysLogExternalReferenceFactory externalReferenceFactory;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        registerExternalReferenceFactory(context);
    }

    public static synchronized void onModuleUnloaded() {
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory();

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new LogMessageToSysLogExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
