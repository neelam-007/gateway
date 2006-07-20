/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.URL;

/**
 * Should be fired before attempting to route the message to the protected service.
 */
public class PreRoutingEvent extends RoutingEvent {
    public PreRoutingEvent(Object object, PolicyEnforcementContext context, URL url) {
        super(object, context, url);
    }
}
