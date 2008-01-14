/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event.system;

import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationEvent;

/**
 * @author alex
 */
public abstract class ServiceCacheEvent extends ApplicationEvent {
    private ServiceCacheEvent(PublishedService source) {
        super(source);
    }

    public PublishedService getService() {
        return (PublishedService) source;
    }

    public static class Deleted extends ServiceCacheEvent {
        public Deleted(PublishedService source) {
            super(source);
        }
    }

    public static class Updated extends ServiceCacheEvent {
        public Updated(PublishedService source) {
            super(source);
        }
    }
}