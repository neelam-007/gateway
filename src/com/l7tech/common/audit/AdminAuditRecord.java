/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
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
    public static final char ACTION_CREATED = 'C';
    public static final char ACTION_UPDATED = 'U';
    public static final char ACTION_DELETED = 'D';

    public AdminAuditRecord(Level level, String nodeId, PersistenceEvent event, String adminLogin, String ip) {
        super(level, nodeId, ip, null);
        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");
        this.adminLogin = adminLogin;

        final Entity entity = event.getEntity();
        this.entityClassname = entity.getClass().getName();
        this.entityOid = entity.getOid();
        int ppos = entityClassname.lastIndexOf(".");
        String localClassname = ppos >= 0 ? entityClassname.substring(ppos+1) : entityClassname;
        StringBuffer msg = new StringBuffer(localClassname);
        msg.append(" #").append(entityOid);
        if (entity instanceof NamedEntity) {
            msg.append(" (");
            msg.append(((NamedEntity)entity).getName());
            msg.append(")");
        } else {
            msg.append(entityOid);
        }
        if (event instanceof Created) {
            action = ACTION_CREATED;
            msg.append(" created");
        } else if (event instanceof Deleted) {
            action = ACTION_DELETED;
            msg.append(" deleted");
        } else if (event instanceof Updated) {
            action = ACTION_UPDATED;
            msg.append(" updated");
        }

        String note = event.getNote();
        if (note != null && note.length() > 0) {
            msg.append(" (").append(note).append(")");
        }
        this.setMessage(msg.toString());
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
