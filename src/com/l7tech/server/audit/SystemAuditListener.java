/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.Component;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.event.Event;
import com.l7tech.objectmodel.event.GenericListener;
import com.l7tech.server.event.lifecycle.LifecycleEvent;
import com.l7tech.server.event.lifecycle.Started;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SystemAuditListener implements GenericListener {
    private static String nodeId = ClusterInfoManager.getInstance().thisNodeId();

    public SystemAuditListener() {
        auditRecordManager = (AuditRecordManager)Locator.getDefault().lookup(AuditRecordManager.class);
        if (auditRecordManager == null) throw new IllegalStateException("Couldn't locate AuditRecordManager");
    }

    public void receive( final Event event ) {
        if (event instanceof LifecycleEvent) {
            LifecycleEvent le = (LifecycleEvent)event;
            try {
                Level level = Level.INFO;
                if (le.getComponent() == Component.GW_SERVER) {
                    level = event instanceof Started ? Level.INFO : Level.FINE;
                }
                auditRecordManager.save(new SystemAuditRecord(level, nodeId, le.getComponent(), le.getAction(), le.getIpAddress()));
            } catch ( SaveException e ) {
                logger.log( Level.SEVERE, "Couldn't save SystemAuditRecord " + event, e );
            }
        }
    }

    private AuditRecordManager auditRecordManager;
    private static final Logger logger = Logger.getLogger(SystemAuditListener.class.getName());
}
