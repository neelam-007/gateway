/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.Component;

import java.util.logging.Level;
import java.io.OutputStream;
import java.io.IOException;

/**
 * SystemAuditRecords are generated for system-level events that are not necessarily triggered by a particular
 * administrator or as a result of a client request.
 *
 * @author alex
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
     * @param component the {@link Component} that was involved in the event
     * @param message a human-readable log message describing what happened
     * @param action a short description of the action that was happening when the event was generated
     * @param identityProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link com.l7tech.identity.IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     * @param ip the IP address of the entity that caused this AuditRecord to be created. It could be that of a cluster node, an administrative workstation or a web service requestor.
     */
    public SystemAuditRecord(Level level, String nodeId, Component component, String message, boolean alwaysAudit, long identityProviderOid, String userName, String userId, String action, String ip) {
        super(level, nodeId, ip, identityProviderOid, userName, userId, component.getName(), message);
        this.componentId = component.getId();
        this.alwaysAudit = alwaysAudit;
        this.action = action;
    }

    /**
     * The code for the component this audit record relates to
     * @see com.l7tech.gateway.common.Component#getId()
     */
    public int getComponentId() {
        return componentId;
    }

    public boolean alwaysAudit() {
        return alwaysAudit;
    }

    /**
     * Gets a short description of the action that was happening when the event was generated
     * @return a short description of the action that was happening when the event was generated
     */
    public String getAction() {
        return action;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAction( String action ) {
        this.action = action;
    }

    private String action;
    private int componentId;
    private boolean alwaysAudit;

    @Override
    public void serializeOtherProperties(OutputStream out, boolean includeAllOthers) throws IOException {
        // component_id:action

        out.write(Integer.toString(componentId).getBytes());
        out.write(SERSEP.getBytes());

        if (action != null) out.write(action.getBytes());
        out.write(SERSEP.getBytes());
    }
}
