/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;
import com.l7tech.server.event.Event;

/**
 * Implementations are events in the lifecycle of a persistent {@link Entity}.
 * @author alex
 * @version $Revision$
 */
public abstract class AdminEvent extends Event {
    public AdminEvent(Entity entity) {
        super(entity);
    }

    public AdminEvent(Entity entity, String note) {
        this(entity);
        this.note = note;
    }

    public Entity getEntity() {
        return (Entity)source;
    }

    public String getNote() {
        return note;
    }

    public String toString() {
        return this.getClass().getName() + " [" + source.getClass().getName() + " #" + ((Entity)source).getOid() + "]";
    }

    protected String note;
}
