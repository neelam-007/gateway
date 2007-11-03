package com.l7tech.server.event.system;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * Events related to transport modules.
 */
public class TransportEvent extends SystemEvent {
    private final String action;

    public TransportEvent(Object source, Component component, String ipAddress, Level level, String action, String message) {
        super(source, component, ipAddress, level, message);
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
