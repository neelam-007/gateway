/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ArrayUtils;

/** @author alex */
public class ServiceEnablementEvent extends GoidEntityInvalidationEvent {
    private final boolean enabled;

    /**
     * Create an EntityInvalidationEvent
     *
     * @param source      The source of invalidation (not usually of interest)
     * @param entityIds   The ids of the invalidated entities
     */
    public ServiceEnablementEvent(final Object source, final Goid[] entityIds, boolean enabled) {
        super(source, PublishedService.class, entityIds, ArrayUtils.fill(new char[entityIds.length], 'U'));
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
