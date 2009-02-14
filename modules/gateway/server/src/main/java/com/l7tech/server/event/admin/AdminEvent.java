/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import com.l7tech.server.audit.AdminAuditConstants;
import org.springframework.context.ApplicationEvent;

import java.util.logging.Level;

/**
 * Implementations are events in the lifecycle of a {@link com.l7tech.objectmodel.PersistentEntity}.
 * @author alex
 * @version $Revision$
 */
public abstract class AdminEvent extends ApplicationEvent {
    private boolean system;
    private boolean auditIgnore;

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
        return AdminAuditConstants.DEFAULT_LEVEL;
    }

    protected String note;

    /**
     * Set this as a "system" event.  Events flagged as system events do not get audited.
     * The PersistenceEventInterceptor sets this flag on certain events to suppress excess auditing.
     *
     * @param system true if this event should not result in an audit record.
     */
    public void setSystem(boolean system) {
        this.system = system;
    }

    /**
     * @return true if the event should nto result in an audit record.
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * @return true if the audit listener should ignore this event.
     */
    public boolean isAuditIgnore() {
        return auditIgnore;
    }

    /**
     * @param auditIgnore  true if the audit listener should ignore this event.
     */
    public void setAuditIgnore(boolean auditIgnore) {
        this.auditIgnore = auditIgnore;
    }
}
