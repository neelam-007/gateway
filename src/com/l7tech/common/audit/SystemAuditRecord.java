/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * SystemAuditRecords are generated for system-level events that are not necessarily triggered by a particular
 * administrator or as a result of a client request.
 *
 * @author alex
 * @version $Revision$
 */
public class SystemAuditRecord extends AuditRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    protected SystemAuditRecord() {
    }

    /**
     * Constructs a new SystemAuditRecord.
     *
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param ip the IP address of the entity that caused this AuditRecord to be created. It could be that of a cluster node, an administrative workstation or a web service requestor.
     * @param component the {@link Component} that was involved in the event
     * @param action a short description of the action that was happening when the event was generated
     */
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

    /**
     * Gets a short description of the action that was happening when the event was generated
     * @return a short description of the action that was happening when the event was generated
     */
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
