/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public class Created extends AdminEvent {
    public Created(Entity entity, String note ) {
        super(entity, note );
    }

    public Created(Entity entity) {
        super(entity);
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof CreateListener)
            ((CreateListener)listener).entityCreated(this);
        else
            super.sendTo(listener);
    }
}
