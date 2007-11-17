/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event.system;

import com.l7tech.common.Component;
import com.l7tech.service.PublishedService;

/**
 * @author alex
 */
public abstract class ServiceCacheEvent extends SystemEvent {
    private final PublishedService service;

    private ServiceCacheEvent(PublishedService source) {
        super(source, Component.GW_SERVER);
        this.service = source;
    }

    public PublishedService getService() {
        return service;
    }

    public static class Deleted extends ServiceCacheEvent {
        public Deleted(PublishedService source) {
            super(source);
        }

        public String getAction() {
            return "Deleted";
        }
    }

    public static class Updated extends ServiceCacheEvent {
        public Updated(PublishedService source) {
            super(source);
        }

        public String getAction() {
            return "Updated";
        }
    }
}