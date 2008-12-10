package com.l7tech.server.ems.gateway;

import org.springframework.context.ApplicationEvent;

/**
 * Event that can be fired when gateway (cluster) registration information is updated.
 */
public class GatewayRegistrationEvent extends ApplicationEvent {
    public GatewayRegistrationEvent(Object o) {
        super(o);
    }
}
