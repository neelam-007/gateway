/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.objectmodel.event.PersistenceEvent;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.event.EventManager;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditBootProcess implements ServerComponentLifecycle {
    public AuditBootProcess() {
        adminAuditListener = new AdminAuditListener();
    }

    public void init( ComponentConfig config ) throws LifecycleException {
    }

    public void start() throws LifecycleException {
        EventManager.addListener(PersistenceEvent.class, adminAuditListener);
    }

    public void stop() throws LifecycleException {
        EventManager.removeListener(adminAuditListener);
    }

    public void close() throws LifecycleException {
    }

    private final AdminAuditListener adminAuditListener;
}
