/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.objectmodel.Goid;

import java.util.logging.Level;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An {@link AuditRecord} that describes a single administrative action.
 * <p>
 * By default, one of these will be created by com.l7tech.server.audit.AdminAuditListener
 * each time an administrator creates, deletes or updates a persistent {@link com.l7tech.objectmodel.Entity}.
 *
 * @author alex
 * @version $Revision$
 */
public class AdminAuditRecord extends AuditRecord {
    public static final char ACTION_CREATED = 'C';
    public static final char ACTION_UPDATED = 'U';
    public static final char ACTION_DELETED = 'D';
    public static final char ACTION_LOGIN = 'L';
    public static final char ACTION_LOGOUT = 'X';
    public static final char ACTION_OTHER = 'O';

    /**
     * Constructs a new AdminAuditRecord.
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param entityGoid the GOID of the entity that this record concerns
     * @param entityClassname the classname of the entity that this record concerns
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param action a character indicating the type of event that generated this record. See {@link #ACTION_CREATED}, {@link #ACTION_UPDATED} and {@link #ACTION_DELETED}
     * @param msg a short description of the event that generated the AuditRecord
     * @param identityProviderOid the GOID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the administrator authenticated
     * @param adminLogin the login ID of the administrator who triggered the event
     * @param adminId  userId the GOID or DN of the administrator
     * @param ip the IP address of the management workstation that was used to trigger the event that caused this AuditRecord to be created
     */
    public AdminAuditRecord(
            Level level,
            String nodeId,
            Goid entityGoid,
            String entityClassname,
            String name,
            char action,
            String msg,
            Goid identityProviderOid,
            String adminLogin,
            String adminId,
            String ip) {
        super(level, nodeId, ip, identityProviderOid, adminLogin, adminId, name, msg);
        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");
        this.entityGoid = entityGoid;
        this.entityClassname = entityClassname;
        this.action = action;
    }


    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public AdminAuditRecord() {
    }

    /**
     * Gets the classname of the entity that this record concerns
     * @return the classname of the entity that this record concerns
     */
    public String getEntityClassname() {
        return entityClassname;
    }

    /**
     * Gets the GOID of the entity that this record concerns
     * @return the GOID of the entity that this record concerns
     */
    public Goid getEntityGoid() {
        return entityGoid;
    }

    /**
     * Gets a character indicating the type of event that generated this record.
     * @return a character indicating the type of event that generated this record.
     */
    public char getAction() {
        return action;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setEntityClassname(String entityClassname) {
        this.entityClassname = entityClassname;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setEntityGoid( Goid entityGoid ) {
        this.entityGoid = entityGoid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setAction(char action) {
        this.action = action;
    }

    /** the classname of the entity that this record concerns */
    protected String entityClassname;
    /** the GOID of the entity that this record concerns */
    protected Goid entityGoid;
    /** a character indicating the type of event that generated this record. */
    protected char action;

    @Override
    public void serializeOtherProperties(OutputStream out, boolean includeAllOthers) throws IOException {
        // entity_class:entity_id:action

        if (entityClassname != null) out.write(entityClassname.getBytes());
        out.write(SERSEP.getBytes());

        if(entityGoid != null) out.write(Goid.toString(entityGoid).getBytes());
        out.write(SERSEP.getBytes());

        out.write(action);
        out.write(SERSEP.getBytes());
    }
}
