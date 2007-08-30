/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.audit;

import com.l7tech.common.Component;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.server.event.system.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;

/**
 * @author alex
 */
public class SystemAuditListener implements ApplicationListener {
    private final String nodeId;
    private AuditContext auditContext;

    public SystemAuditListener(String nodeId, AuditContext auditContext) {
        this.nodeId = nodeId;
        this.auditContext = auditContext;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof SystemEvent) {
            SystemEvent se = (SystemEvent)event;
            Level level = se.getLevel();
            if (se.getComponent() == Component.GW_SERVER) {
                level = (event instanceof Started || event instanceof Stopped) ? Level.INFO : Level.FINE;
            } else if (se.getComponent() == Component.GW_AUDIT_SYSTEM) {
                level = Level.SEVERE;
            }
            boolean alwaysAudit = true;
            if (event instanceof RoutineSystemEvent) {
                alwaysAudit = false;
            }
            SystemAuditRecord record = new SystemAuditRecord(level, nodeId, se.getComponent(), se.getMessage(),
                    alwaysAudit, se.getIdentityProviderOid(), se.getUserName(), se.getUserId(), se.getAction(),
                    se.getIpAddress());
            if (event instanceof AuditPurgeEvent) {
                AuditPurgeEvent ape = (AuditPurgeEvent) event;
                ape.setSystemAuditRecord(record);
            }
            auditContext.setCurrentRecord(record);
            auditContext.flush();
        }
    }
}
