/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.service.ServiceEventPromoter;
import com.l7tech.cluster.ClusterInfoManager;
import org.springframework.context.ApplicationContext;

/**
 * Initializes the audit system.
 *
 * @author alex
 * @version $Revision$
 */
public class AuditBootProcess implements ServerComponentLifecycle {
    public AuditBootProcess() {
    }

    public void setServerConfig(ServerConfig config) throws LifecycleException {
        ApplicationContext appCtx = config.getSpringContext();
        eventManager = (EventManager)appCtx.getBean("eventManager");
        messageAuditListener = new MessageProcessingAuditListener(appCtx);
        adminAuditListener = new AdminAuditListener(appCtx);
        servicePromoter = new ServiceEventPromoter();
    }

    public void start() throws LifecycleException {
        eventManager.addListener(MessageProcessed.class, messageAuditListener);
        eventManager.addListener(AdminEvent.class, adminAuditListener);
        eventManager.addPromoter(AdminEvent.class, servicePromoter);
    }

    public void stop() throws LifecycleException {
        eventManager.removeListener(messageAuditListener);
        eventManager.removeListener(adminAuditListener);
        eventManager.removePromoter(servicePromoter);
    }

    public void close() throws LifecycleException {
    }

    private AdminAuditListener adminAuditListener;
    private ServiceEventPromoter servicePromoter;
    private MessageProcessingAuditListener messageAuditListener;
    private EventManager eventManager;
}
