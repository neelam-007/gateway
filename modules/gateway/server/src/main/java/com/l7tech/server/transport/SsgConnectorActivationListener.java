package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;

/**
 * Listener interface for endpoint activations
 */
public interface SsgConnectorActivationListener {

    /**
     * Notification of activation of an endpoint.
     *
     * @param connector The activated connector
     */
    void notifyActivated( SsgConnector connector );
}
