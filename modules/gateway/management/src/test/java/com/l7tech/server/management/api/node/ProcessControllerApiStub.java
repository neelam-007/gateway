package com.l7tech.server.management.api.node;

import com.l7tech.gateway.common.transport.SsgConnector;

/**
 *
 */
public class ProcessControllerApiStub implements ProcessControllerApi {
    public void connectorCreated(SsgConnector connector) {
    }

    public void connectorUpdated(SsgConnector connector) {
    }

    public void connectorDeleted(SsgConnector connector) {
    }

    public void notifyEvent(EventSubscription sub, Object event) {
    }

    public void ping() {
    }
}
