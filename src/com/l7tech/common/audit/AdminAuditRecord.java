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
import net.jini.export.ServerContext;
import net.jini.io.context.ClientSubject;

import java.rmi.server.ServerNotActiveException;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;

/**
 * An {@link AuditRecord} that describes a single administrative action.
 * <p>
 * By default, one of these will be created by {@link com.l7tech.server.audit.AdminAuditFactory}
 * each time an administrator creates, deletes or updates a persistent {@link com.l7tech.objectmodel.Entity}.
 *
 * @author alex
 * @version $Revision$
 */
public class AdminAuditRecord extends AuditRecord {
    public AdminAuditRecord(Level level, PersistenceEvent event) {
        super(level);
        try {
            ClientSubject clientSubject = (ClientSubject)ServerContext.getServerContextElement(ClientSubject.class);
            if (clientSubject != null) {
                Set principals = clientSubject.getClientSubject().getPrincipals();
                if (principals != null && !principals.isEmpty()) {
                    adminLogin = ((Principal)principals.iterator().next()).getName();
                }
            }
        } catch ( ServerNotActiveException e ) {
            throw new RuntimeException(e);
        }

        if (adminLogin == null) throw new IllegalStateException("Couldn't determine current administrator login");

        final Entity entity = event.getEntity();
        this.entityClass = entity.getClass();
        this.entityOid = entity.getOid();
        StringBuffer msg = new StringBuffer(entityClass.getName());
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

    protected String adminLogin;
    protected Class entityClass;
    protected long entityOid;
}
