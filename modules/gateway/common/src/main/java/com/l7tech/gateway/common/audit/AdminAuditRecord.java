/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.audit;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.Table;
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
@Entity
@Table(name="audit_admin")
@OnDelete(action= OnDeleteAction.CASCADE)
public class AdminAuditRecord extends AuditRecord {
    public static final char ACTION_CREATED = 'C';
    public static final char ACTION_UPDATED = 'U';
    public static final char ACTION_DELETED = 'D';
    public static final char ACTION_LOGIN = 'L';
    public static final char ACTION_OTHER = 'O';

    /**
     * Constructs a new AdminAuditRecord.
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param entityOid the OID of the entity that this record concerns
     * @param entityClassname the classname of the entity that this record concerns
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param action a character indicating the type of event that generated this record. See {@link #ACTION_CREATED}, {@link #ACTION_UPDATED} and {@link #ACTION_DELETED}
     * @param msg a short description of the event that generated the AuditRecord
     * @param identityProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the administrator authenticated
     * @param adminLogin the login ID of the administrator who triggered the event
     * @param adminId  userId the OID or DN of the administrator
     * @param ip the IP address of the management workstation that was used to trigger the event that caused this AuditRecord to be created
     */
    public AdminAuditRecord(
            Level level,
            String nodeId,
            long entityOid,
            String entityClassname,
            String name,
            char action,
            String msg,
            long identityProviderOid,
            String adminLogin,
            String adminId,
            String ip) {
        super(level, nodeId, ip, identityProviderOid, adminLogin, adminId, name, msg);
        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");
        this.entityOid = entityOid;
        this.entityClassname = entityClassname;
        this.action = action;
    }


    /** @deprecated to be called only for serialization and persistence purposes! */
    public AdminAuditRecord() {
    }

    /**
     * Gets the classname of the entity that this record concerns
     * @return the classname of the entity that this record concerns
     */
    @Column(name="entity_class", length=255)
    public String getEntityClassname() {
        return entityClassname;
    }

    /**
     * Gets the OID of the entity that this record concerns
     * @return the OID of the entity that this record concerns
     */
    @Column(name="entity_id")
    public long getEntityOid() {
        return entityOid;
    }

    /**
     * Gets a character indicating the type of event that generated this record.
     * @return a character indicating the type of event that generated this record.
     */
    @Column(name="action")
    public char getAction() {
        return action;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setEntityClassname(String entityClassname) {
        this.entityClassname = entityClassname;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setEntityOid( long entityOid ) {
        this.entityOid = entityOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAction(char action) {
        this.action = action;
    }

    /** the classname of the entity that this record concerns */
    protected String entityClassname;
    /** the OID of the entity that this record concerns */
    protected long entityOid;
    /** a character indicating the type of event that generated this record. */
    protected char action;

    public void serializeOtherProperties(OutputStream out, boolean includeAllOthers) throws IOException {
        // entity_class:entity_id:action

        if (entityClassname != null) out.write(entityClassname.getBytes());
        out.write(SERSEP.getBytes());

        out.write(Long.toString(entityOid).getBytes());
        out.write(SERSEP.getBytes());

        out.write(action);
        out.write(SERSEP.getBytes());
    }
}
