/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 * @version $Revision$
 */
public class PersistenceEvent extends AdminEvent {
    public PersistenceEvent(Entity entity) {
        super(entity);
    }

    public PersistenceEvent(Entity entity, String note) {
        super(entity, note);
    }

    public Entity getEntity() {
        return (Entity)source;
    }

    public String toString() {
        return this.getClass().getName() + " [" + source.getClass().getName() + " #" + ((Entity)source).getOid() + "]";
    }
}
