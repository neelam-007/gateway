/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.service.ServiceEventPromoter;

/**
 * Initializes the audit system.
 *
 * @author alex
 * @version $Revision$
 */
public class AuditBootProcess implements ServerComponentLifecycle {
    public AuditBootProcess() {
        messageAuditListener = new MessageProcessingAuditListener();
        adminAuditListener = new AdminAuditListener();
        servicePromoter = new ServiceEventPromoter();
    }

    public void setComponentConfig( ComponentConfig config ) throws LifecycleException {
    }

    public void start() throws LifecycleException {
        EventManager.addListener(MessageProcessed.class, messageAuditListener);
        EventManager.addListener(AdminEvent.class, adminAuditListener);
        EventManager.addPromoter(AdminEvent.class, servicePromoter);
    }

    public void stop() throws LifecycleException {
        EventManager.removeListener(messageAuditListener);
        EventManager.removeListener(adminAuditListener);
        EventManager.removePromoter(servicePromoter);
    }

    public void close() throws LifecycleException {
    }

    private final AdminAuditListener adminAuditListener;
    private final ServiceEventPromoter servicePromoter;
    private final MessageProcessingAuditListener messageAuditListener;
}
