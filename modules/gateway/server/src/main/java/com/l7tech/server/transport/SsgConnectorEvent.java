package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

/**
 * Superclass for application events involving connector activation/deactivation.
 */
public class SsgConnectorEvent extends ApplicationEvent {
    @NotNull
    final SsgConnector connector;

    @NotNull
    final String action;

    public SsgConnectorEvent( Object source, @NotNull SsgConnector connector, @NotNull String action ) {
        super( source );
        this.connector = connector;
        this.action = action;
    }

    @NotNull
    public String getAction() {
        return action;
    }

    @NotNull
    public SsgConnector getConnector() {
        return connector;
    }
}
