/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.audit.AdminAuditRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.event.Created;
import com.l7tech.objectmodel.event.Deleted;
import com.l7tech.objectmodel.event.PersistenceEvent;
import com.l7tech.objectmodel.event.Updated;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminAuditFactory {
    public static AdminAuditRecord created(Level level, Entity entity, String note) {
        PersistenceEvent event = new Created(entity, note);
        return new AdminAuditRecord(level, event);
    }

    public static AdminAuditRecord updated(Level level, Entity original, Entity updated, String note) {
        PersistenceEvent event = new Updated(original, updated, note);
        return new AdminAuditRecord(level, event);
    }

    public static AdminAuditRecord deleted(Level level, Entity entity, String note) {
        PersistenceEvent event = new Deleted(entity, note);
        return new AdminAuditRecord(level, event);
    }
}
