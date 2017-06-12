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
        logger.log(Level.WARNING, "LMTSLModuleLoadListener: made it here");
        registerExternalReferenceFactory(context);
    }

    public static synchronized void onModuleUnloaded() {
        logger.log(Level.WARNING, "LMTSLModuleLoadListener: abcdefg");
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        logger.log(Level.WARNING, "LMTSLModuleLoadListener: reg");

        unregisterExternalReferenceFactory();

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new LogMessageToSysLogExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        logger.log(Level.WARNING, "LMTSLModuleLoadListener: unreg");

        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
