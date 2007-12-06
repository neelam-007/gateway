package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.common.Component;

/**
 * Event class for use by Syslog client.
 */
public class SyslogEvent extends SystemEvent {

    //- PUBLIC

    public SyslogEvent(
            Object source,
            String target,
            boolean connected) {
        super( source,
                Component.GW_SYSLOG,
                null,
                getLevel(connected),
                getMessage(connected,target) );
        this.connected = connected;
    }


    public String getAction() {
        if ( connected ) {
            return ACTION_CONNECTED;
        } else {
            return ACTION_DISCONNECTED;
        }
    }

    //- PRIVATE

    private static final String ACTION_CONNECTED = "Connected";
    private static final String ACTION_DISCONNECTED = "Disconnected";

    private final boolean connected;

    private static Level getLevel( final boolean connected ) {
        if ( connected ) {
            return Level.INFO;
        } else {
            return Level.WARNING;
        }
    }

    private static String getMessage( final boolean connected,
                                      final String target ) {
        if ( connected ) {
            return "Connected to Syslog server '" + target + "'";
        } else {
            return "Connection failed/lost for Syslog server '" + target + "'";
        }
    }
}
