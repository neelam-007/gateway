/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel.event;

import com.l7tech.objectmodel.Entity;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public class Updated extends PersistenceEvent {
    public Updated(Entity entity, EntityChangeSet changes, String note ) {
        super(entity, note );
        this.changeSet = changes;
    }

    public Updated(Entity original, EntityChangeSet changes) {
        this(original, changes, null);
    }

    public Entity getOriginal() {
        return (Entity)source;
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
