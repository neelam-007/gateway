/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 * @version $Revision$
 */
public class Created extends PersistenceEvent {
    public Created(Entity entity, String note ) {
        super(entity, note );
    }

    public Created(Entity entity) {
        super(entity);
    }
}
