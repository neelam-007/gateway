/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel.event;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistenceEvent extends Event {
    public PersistenceEvent(Entity entity) {
        super(entity);
    }

    public PersistenceEvent(Entity entity, String note) {
        this(entity);
        this.note = note;
    }

    public Entity getEntity() {
        return (Entity)source;
    }

    public String getNote() {
        return note;
    }

    private String note;
}
