/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service;

import com.l7tech.objectmodel.event.EntityChangeSet;
import com.l7tech.objectmodel.event.Event;
import com.l7tech.objectmodel.event.Updated;
import com.l7tech.server.event.EventPromoter;
import com.l7tech.service.PublishedService;

/**
 * @author alex
 * @version $Revision$
 */
public class ServiceEventPromoter implements EventPromoter {
    private static final String SERVICE_DISABLED = "disabled";

    public Event promote( Event event ) {
        if (event instanceof Updated ) {
            Updated updated = (Updated)event;
            if (updated.getEntity() instanceof PublishedService) {
                EntityChangeSet changes = updated.getChangeSet();
                Object o = changes.getOldValue(SERVICE_DISABLED);
                Object n = changes.getNewValue(SERVICE_DISABLED);
                if (o == null || n == null) return event;
                if (o.equals(n)) {
                    return event;
                } else if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                    return new ServiceEvent.Disabled(updated.getEntity(), changes);
                } else {
                    return new ServiceEvent.Enabled(updated.getEntity(), changes);
                }
            }
        }
        return event;
    }
}
