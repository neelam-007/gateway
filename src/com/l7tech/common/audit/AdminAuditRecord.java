/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * An {@link AuditRecord} that describes a single administrative action.
 * <p>
 * By default, one of these will be created by {@link com.l7tech.server.audit.AdminAuditListener}
 * each time an administrator creates, deletes or updates a persistent {@link com.l7tech.objectmodel.Entity}.
 *
 * @author alex
 * @version $Revision$
 */
public class AdminAuditRecord extends AuditRecord {
    public static final char ACTION_CREATED = 'C';
    public static final char ACTION_UPDATED = 'U';
    public static final char ACTION_DELETED = 'D';

    /**
     * Constructs a new AdminAuditRecord.
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param ip the IP address of the management workstation that was used to trigger the event that caused this AuditRecord to be created
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param msg a short description of the event that generated the AuditRecord
     * @param entityOid the OID of the entity that this record concerns
     * @param entityClassname the classname of the entity that this record concerns
     * @param action a character indicating the type of event that generated this record. See {@link #ACTION_CREATED}, {@link #ACTION_UPDATED} and {@link #ACTION_DELETED}
     * @param adminLogin the login ID of the administrator who triggered the event
     */
    public AdminAuditRecord(Level level, String nodeId, long entityOid, String entityClassname, String name, char action, String msg, String adminLogin, String ip) {
        super(level, nodeId, ip, name, msg);
        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");
        this.adminLogin = adminLogin;
        this.entityOid = entityOid;
        this.entityClassname = entityClassname;
        this.action = action;
    }


    /** @deprecated to be called only for serialization and persistence purposes! */
    public AdminAuditRecord() {
    }

    /**
     * The login ID of the administrator who triggered the event
     * @return the login ID of the administrator who triggered the event
     * @see com.l7tech.identity.IdentityAdmin#findUserByLogin(long, String)
     */
    public String getAdminLogin() {
        return adminLogin;
    }

    /**
     * Gets the classname of the entity that this record concerns
     * @return the classname of the entity that this record concerns
     */
    public String getEntityClassname() {
        return entityClassname;
    }

    /**
     * Gets the OID of the entity that this record concerns
     * @return the OID of the entity that this record concerns
     */
    public long getEntityOid() {
        return entityOid;
    }

    /**
     * Gets a character indicating the type of event that generated this record.
     * @return a character indicating the type of event that generated this record.
     */
    public char getAction() {
        return action;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAdminLogin( String adminLogin ) {
        this.adminLogin = adminLogin;
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

    /** the login ID of the administrator who triggered the event */
    protected String adminLogin;
    /** the classname of the entity that this record concerns */
    protected String entityClassname;
    /** the OID of the entity that this record concerns */
    protected long entityOid;
    /** a character indicating the type of event that generated this record. */
    protected char action;
}
