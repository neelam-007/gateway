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
            final SystemEvent se = (SystemEvent)event;

            SystemAuditRecord record = null;
            if (event instanceof AuditPurgeEvent) {
                AuditPurgeEvent ape = (AuditPurgeEvent)event;
                if (ape.isUpdate()) {
                    record = ape.getSystemAuditRecord();
                    auditContext.setUpdate(true);
                } else {
                    record = createSystemAuditRecord(se);
                    ape.setSystemAuditRecord(record);
                }
            } else {
                record = createSystemAuditRecord(se);
            }

            auditContext.setCurrentRecord(record);
            auditContext.flush();
            auditContext.setUpdate(false);
        }
    }

    private SystemAuditRecord createSystemAuditRecord(SystemEvent event) {
        Level level = event.getLevel();
        if (event.getComponent() == Component.GW_SERVER) {
            level = (event instanceof Started || event instanceof Stopped) ? Level.INFO : Level.FINE;
        } else if (event.getComponent() == Component.GW_AUDIT_SYSTEM) {
            level = Level.SEVERE;
        }

        boolean alwaysAudit = true;
        if (event instanceof RoutineSystemEvent) {
            alwaysAudit = false;
        }

        final SystemAuditRecord record = new SystemAuditRecord(level,
                                                               nodeId,
                                                               event.getComponent(),
                                                               event.getMessage(),
                                                               alwaysAudit,
                                                               event.getIdentityProviderOid(),
                                                               event.getUserName(),
                                                               event.getUserId(),
                                                               event.getAction(),
                                                               event.getIpAddress());
        return record;
    }
}
