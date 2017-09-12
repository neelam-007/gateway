package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Application event broadcast when as SsgConnector is deactivated.
 */
public class SsgConnectorDeactivationEvent extends SsgConnectorEvent {
    public SsgConnectorDeactivationEvent( Object source, @NotNull SsgConnector connector ) {
        super( source, connector, "deactivated" );
    }
}
