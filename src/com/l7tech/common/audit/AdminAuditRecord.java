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

    public String getAdminLogin() {
        return adminLogin;
    }

    public String getEntityClassname() {
        return entityClassname;
    }

    public long getEntityOid() {
        return entityOid;
    }

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

    protected String adminLogin;
    protected String entityClassname;
    protected long entityOid;
    protected char action;
}
