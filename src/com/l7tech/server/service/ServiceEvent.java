/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service;

import com.l7tech.objectmodel.Entity;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.service.PublishedService;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServiceEvent extends Updated {
    private ServiceEvent( Entity entity, EntityChangeSet changes, String which ) {
        super( entity, changes );
        if (!(entity instanceof PublishedService)) throw new IllegalArgumentException("Entity must be a PublishedService");
        this.note = which;
    }

    public static class Disabled extends ServiceEvent {
        public Disabled( Entity entity, EntityChangeSet changes) {
            super( entity, changes, "disabled");
        }
    }

    public static class Enabled extends ServiceEvent {
        public Enabled( Entity entity, EntityChangeSet changes) {
            super( entity, changes, "re-enabled");
        }
    }

}
