package com.l7tech.server.audit;

import com.l7tech.server.DefaultKeyImpl;
import com.l7tech.server.ServerConfig;

import java.util.logging.Logger;

/**
 * Created by Spring when everything is ready to go to activate the server audit subsystem.
 */
public class AuditContextFactoryActivator {
    private static final Logger logger = Logger.getLogger(AuditContextFactoryActivator.class.getName());

    public AuditContextFactoryActivator(ServerConfig serverConfig,
                                        AuditContextFactory auditContextFactory,
                                        AuditRecordManager auditRecordManager,
                                        String clusterNodeId,
                                        DefaultKeyImpl defaultKey,
                                        AuditPolicyEvaluator auditPolicyEvaluator,
                                        AuditFilterPolicyManager auditFilterPolicyManager)
    {
        logger.info("Activating server audit subsystem");
        auditContextFactory.activateServerAuditing(serverConfig,
                                                          auditRecordManager,
                                                          clusterNodeId,
                                                          auditPolicyEvaluator,
                                                          auditFilterPolicyManager,
                                                          defaultKey);
    }
}
