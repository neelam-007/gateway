/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class SystemAuditRecord extends AuditRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    protected SystemAuditRecord() {
    }

    public SystemAuditRecord(Level level, String nodeId, Component component, String action, String ip) {
        super(level, nodeId, ip, component.getName(), component.getName() + " " + action);
        this.component = component.getCode();
        this.action = action;
    }

    /**
     * The code for the component this audit record relates to
     * @see {@link com.l7tech.common.Component#getCode()} 
     */
    public String getComponent() {
        return component;
    }

    public String getAction() {
        return action;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setComponent( String component ) {
        this.component = component;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAction( String action ) {
        this.action = action;
    }

    private String action;
    private String component;
}
