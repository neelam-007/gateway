/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.objectmodel.Entity;

/**
 * Fired when a {@link com.l7tech.objectmodel.GoidEntity} has been updated.
 * <p>
 * The {@link #source} is the updated object, and the included {@link com.l7tech.server.event.EntityChangeSet} gives access
 * to the old and new values on a per-property basis.
 */
public class Updated<ET extends Entity> extends PersistenceEvent<ET> {
    public Updated(ET entity, EntityChangeSet changes, String note ) {
        super(entity, note );
        this.changeSet = changes;
    }
                                                
    public Updated(ET original, EntityChangeSet changes) {
        this(original, changes, null);
    }

    public EntityChangeSet getChangeSet() {
        return changeSet;
    }

    private final EntityChangeSet changeSet;
}
