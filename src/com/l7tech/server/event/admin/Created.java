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
public class Created<ET extends Entity> extends PersistenceEvent<ET> {
    public Created(ET entity, String note ) {
        super(entity, note );
    }

    public Created(ET entity) {
        super(entity);
    }
}
