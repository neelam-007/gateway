/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.alert;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;

/**
 * A configuration record for an audit alert action.
 */
public abstract class Notification extends NamedEntityImp {
    private Set events;

    public Set getEvents() {
        return events;
    }

    public void setEvents(Set events) {
        this.events = events;
    }
}
