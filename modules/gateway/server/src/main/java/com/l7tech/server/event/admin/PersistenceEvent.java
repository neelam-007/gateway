/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Entity;

import java.text.MessageFormat;

@SuppressWarnings({"unchecked"})
public class PersistenceEvent<ET extends Entity> extends AdminEvent {
    public PersistenceEvent(ET entity) {
        super(entity);
    }

    public PersistenceEvent(ET entity, String note) {
        super(entity, note);
    }

    public ET getEntity() {
        return (ET) source;
    }

    public String toString() {
        return MessageFormat.format("{0} [{1} #{2}]", this.getClass().getName(), source.getClass().getName(), ((ET) source).getId());
    }
}
