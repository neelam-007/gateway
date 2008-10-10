package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

import java.util.logging.Level;

/**
 * Event type for email listener events.
 */
public class EmailEvent extends TransportEvent {
    //- PUBLIC

    public EmailEvent(
            Object source,
            Level level,
            String ip,
            String message) {
        super(source, Component.GW_EMAILRECV, ip, level, NAME, message);
    }

    //- PRIVATE

    private static final String NAME = "Connect";
}
