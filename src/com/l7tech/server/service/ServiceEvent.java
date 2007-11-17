/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service;

import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.service.PublishedService;

public abstract class ServiceEvent extends Updated<PublishedService> {
    private ServiceEvent(PublishedService entity, EntityChangeSet changes, String which) {
        super(entity, changes);
        if (entity == null) throw new NullPointerException("Service must not be null");
        this.note = which;
    }

    public static class Disabled extends ServiceEvent {
        public Disabled(PublishedService entity, EntityChangeSet changes) {
            super(entity, changes, "disabled");
        }
    }

    public static class Enabled extends ServiceEvent {
        public Enabled(PublishedService entity, EntityChangeSet changes) {
            super(entity, changes, "re-enabled");
        }
    }

}
