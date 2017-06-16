package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReferenceFactory;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

/**
 * The LogMessageToSysLogModuleLoadListener registers and unregisters LogMessageToSysLogExternalReferenceFactories
 * when the Log Message to Syslog Assertion is loaded and unloaded.
 *
 * @author huaal03
 * @see com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion
 * @see LogMessageToSysLogExternalReferenceFactory
 */
public class LogMessageToSysLogModuleLoadListener {

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static LogMessageToSysLogExternalReferenceFactory externalReferenceFactory;

    /**
     * Registers a LogMessageToSysLogExternalReferenceFactory when the Log Message to Syslog Assertion is loaded
     *
     * @param context the current application context
     */
    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        registerExternalReferenceFactory(context);
    }

    /**
     * Deregisters a LogMessageToSysLogExternalReferenceFactory when the Log Message to Syslog Assertion is unloaded
     */
    @SuppressWarnings("UnusedDeclaration")
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
