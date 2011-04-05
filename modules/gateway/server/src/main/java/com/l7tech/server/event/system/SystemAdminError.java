package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

import java.util.logging.Level;

/**
 * System event for errors in administrative APIs.
 */
public class SystemAdminError extends SystemEvent {

    public SystemAdminError( final Object source,
                             final String message ) {
        super( source, Component.GW_SERVER, null, Level.INFO, message );
    }

    @Override
    public String getAction() {
        return "Manager Action";
    }
}
