/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.URL;

/**
 * @author alex
 */
public class RoutingEvent extends MessageProcessingEvent {
    protected final URL url;

    public RoutingEvent(Object object, PolicyEnforcementContext context, URL url) {
        super(object, context);
        this.url = url;
    }

    /**
     * @return the URL of the protected service to which the message was routed.
     */
    public URL getUrl() {
        return url;
    }
}
