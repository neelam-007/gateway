/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditRecordManager extends EntityManager {
    public static final Level[] LEVELS_IN_ORDER = { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };
    public static final String PROP_TIME = "millis";
    public static final String PROP_LEVEL = "strLvl";
    public static final String PROP_OID = "oid";
    public static final String PROP_NODEID = "nodeId";

    AuditRecord findByPrimaryKey(long oid) throws FindException;
    Collection find(AuditSearchCriteria criteria) throws FindException;

    long save(AuditRecord rec) throws SaveException;

    void deleteOldAuditRecords() throws DeleteException;
}
