/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel.event;

import com.l7tech.server.event.GenericListener;

import java.util.EventListener;
import java.util.EventObject;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class Event extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     */
    public Event( Object source ) {
        super( source );
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof GenericListener) ((GenericListener)listener).receive(this);
    }
}
