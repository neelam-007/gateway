/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.management.api.node.EventSubscription;
import com.l7tech.server.management.api.node.ProcessControllerApi;

import javax.annotation.Resource;
import javax.jws.WebService;
import java.util.logging.Logger;

/** @author alex */
@WebService(name="ProcessControllerApi", targetNamespace = "http://ns.l7tech.com/secureSpan/5.0/component/processController/processControllerApi", endpointInterface="com.l7tech.server.management.api.node.ProcessControllerApi")
public class ProcessControllerApiImpl implements ProcessControllerApi {
    private static final Logger logger = Logger.getLogger(ProcessControllerApiImpl.class.getName());

    @Resource
    private ProcessController processController;

    public void connectorCreated(SsgConnector connector) {
    }

    public void connectorUpdated(SsgConnector connector) {
    }

    public void connectorDeleted(SsgConnector connector) {
    }

    public void notifyEvent(EventSubscription sub, Object event) {
    }

    public void ping() {
        logger.fine("ping");
    }
}
