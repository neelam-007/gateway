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
    public Updated(Entity original, Entity updated, String note ) {
        super(updated, note );
        this.original = original;
    }

    public Updated(Entity original, Entity updated) {
        this(original, updated, null);
    }

    public Entity getOriginal() {
        return original;
    }

    public Entity getUpdated() {
        return (Entity)source;
    }

    public Class getListenerClass() {
        return UpdateListener.class;
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof UpdateListener)
            ((UpdateListener)listener).entityUpdated(this);
        else
            super.sendTo(listener);
    }

    protected Entity original;
}
