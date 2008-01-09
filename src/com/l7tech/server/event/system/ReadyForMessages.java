package com.l7tech.server.event.system;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * Event fired by GatewayBoot when the BootProcess has finished.
 * It is now safe to accept incoming requests.
 */
public class ReadyForMessages extends SystemEvent {
    public ReadyForMessages(Object source, Component component, String ip) {
        super(source, component, ip, Level.INFO);
    }

    public String getAction() {
        return "Ready";
    }
}
