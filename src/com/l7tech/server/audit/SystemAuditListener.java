/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.Component;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.server.event.Event;
import com.l7tech.server.event.GenericListener;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.event.system.SystemEvent;

import java.util.logging.Level;

import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class SystemAuditListener implements GenericListener {
    private final String nodeId;
    private final ApplicationContext applicationContext;

    public SystemAuditListener(ApplicationContext ctx) {
        this.applicationContext = ctx;
        ClusterInfoManager cim = (ClusterInfoManager)applicationContext.getBean("clusterInfoManager");
        nodeId =  cim.thisNodeId();
    }

    public void receive( final Event event ) {
        if (event instanceof SystemEvent) {
            SystemEvent se = (SystemEvent)event;
            Level level = Level.INFO;
            if (se.getComponent() == Component.GW_SERVER) {
                level = (event instanceof Started || event instanceof Stopped) ? Level.INFO : Level.FINE;
            } if (se.getComponent() == Component.GW_AUDIT_SYSTEM) {
                level = Level.SEVERE;
            }
            AuditContext.getCurrent(applicationContext).add(new SystemAuditRecord(level, nodeId, se.getComponent(), se.getAction(), se.getIpAddress()));
            AuditContext.getCurrent(applicationContext).flush();
        }
    }
}
