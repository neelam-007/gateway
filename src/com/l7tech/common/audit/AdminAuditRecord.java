/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.event.Created;
import com.l7tech.objectmodel.event.Deleted;
import com.l7tech.objectmodel.event.PersistenceEvent;
import com.l7tech.objectmodel.event.Updated;

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
    public AdminAuditRecord(Level level, String nodeId, PersistenceEvent event, String adminLogin) {
        super(level, nodeId, null);
        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");
        this.adminLogin = adminLogin;

        final Entity entity = event.getEntity();
        this.entityClassname = entity.getClass().getName();
        this.entityOid = entity.getOid();
        StringBuffer msg = new StringBuffer(entityClassname);
        msg.append(" ");
        msg.append(entityOid);
        if (event instanceof Created) {
            msg.append(" created");
        } else if (event instanceof Deleted) {
            msg.append(" deleted");
        } else if (event instanceof Updated) {
            msg.append(" updated");
        }

        String note = event.getNote();
        if (note != null && note.length() > 0) {
            msg.append(" (").append(note).append(")");
        }
        this.message = msg.toString();
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

    protected String adminLogin;
    protected String entityClassname;
    protected long entityOid;
}
