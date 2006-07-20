/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.URL;

/**
 * Should be fired after a response is received from the protected service.
 */
public class PostRoutingEvent extends RoutingEvent {
    private final int httpResponseStatus;

    public PostRoutingEvent(Object source, PolicyEnforcementContext context, URL url, int status) {
        super(source, context, url);
        this.httpResponseStatus = status;
    }

    /**
     * @return the HTTP status code that was returned by the protected service.
     */
    public int getHttpResponseStatus() {
        return httpResponseStatus;
    }
}
