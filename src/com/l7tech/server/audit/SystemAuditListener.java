/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.event.Event;
import com.l7tech.objectmodel.event.GenericListener;
import com.l7tech.server.event.lifecycle.LifecycleEvent;

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
                auditRecordManager.save(new SystemAuditRecord(Level.INFO, nodeId, le.getComponent(), le.getAction()));
            } catch ( SaveException e ) {
                logger.log( Level.SEVERE, "Couldn't save SystemAuditRecord " + event, e );
            }
        }
    }

    private AuditRecordManager auditRecordManager;
    private static final Logger logger = Logger.getLogger(SystemAuditListener.class.getName());
}
