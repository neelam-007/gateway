/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;
import com.l7tech.server.event.EntityChangeSet;

import java.util.EventListener;

/**
 * Fired when a persistent {@link Entity} has been updated.
 * <p>
 * The {@link #source} is the updated object, and the included {@link com.l7tech.server.event.EntityChangeSet} gives access
 * to the old and new values on a per-property basis.
 *  
 * @author alex
 * @version $Revision$
 */
public class Updated extends AdminEvent {
    public Updated(Entity entity, EntityChangeSet changes, String note ) {
        super(entity, note );
        this.changeSet = changes;
    }

    public Updated(Entity original, EntityChangeSet changes) {
        this(original, changes, null);
    }

    public Class getListenerClass() {
        return UpdateListener.class;
    }

    public EntityChangeSet getChangeSet() {
        return changeSet;
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof UpdateListener)
            ((UpdateListener)listener).entityUpdated(this);
        else
            super.sendTo(listener);
    }

    private final EntityChangeSet changeSet;
}
