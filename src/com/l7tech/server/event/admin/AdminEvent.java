/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;
import com.l7tech.server.audit.AdminAuditListener;
import com.l7tech.server.event.Event;

import java.util.logging.Level;

/**
 * Implementations are events in the lifecycle of a persistent {@link Entity}.
 * @author alex
 * @version $Revision$
 */
public abstract class AdminEvent extends Event {
    public AdminEvent(Object source) {
        super(source);
    }

    public AdminEvent(Object source, String note) {
        this(source);
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public Level getMinimumLevel() {
        return AdminAuditListener.DEFAULT_LEVEL;
    }

    protected String note;
}
