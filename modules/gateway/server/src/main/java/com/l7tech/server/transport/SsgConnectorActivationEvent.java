package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Application event broadcast when a connector is activated.
 */
public class SsgConnectorActivationEvent extends SsgConnectorEvent {
    public SsgConnectorActivationEvent( Object source, @NotNull SsgConnector connector ) {
        super( source, connector, "activated" );
    }
}
