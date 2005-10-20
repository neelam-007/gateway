/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.Component;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.event.system.SystemEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;

/**
 * @author alex
 */
public class SystemAuditListener implements ApplicationListener {
    private final String nodeId;
    private AuditContext auditContext;

    public SystemAuditListener(ClusterInfoManager clusterInfoManager, AuditContext auditContext) {
        this.nodeId = clusterInfoManager.thisNodeId();
        this.auditContext = auditContext;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof SystemEvent) {
            SystemEvent se = (SystemEvent)event;
            Level level = se.getLevel();
            if (se.getComponent() == Component.GW_SERVER) {
                level = (event instanceof Started || event instanceof Stopped) ? Level.INFO : Level.FINE;
            } if (se.getComponent() == Component.GW_AUDIT_SYSTEM) {
                level = Level.SEVERE;
            }
            auditContext.setCurrentRecord(new SystemAuditRecord(level, nodeId, se.getComponent(), se.getMessage(), se.getAction(), se.getIpAddress()));
            auditContext.flush();
        }
    }
}
