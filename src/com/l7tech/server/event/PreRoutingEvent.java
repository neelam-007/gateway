/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.URL;

/**
 * Should be fired before attempting to route the message to the protected service.
 */
public class PreRoutingEvent extends MessageProcessingEvent {
    private final URL url;

    public PreRoutingEvent(Object object, PolicyEnforcementContext context, URL url) {
        super(object, context);
        this.url = url;
    }

    /**
     * @return the URL to which the messag is about to be sent
     */
    public URL getUrl() {
        return url;
    }
}
