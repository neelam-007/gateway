/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditRecordManager extends EntityManager {
    public static Level[] LEVELS_IN_ORDER = { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };
    public static String PROP_MILLIS = "millis";
    public static String PROP_STRLVL = "strlvl";

    AuditRecord findByPrimaryKey(long oid) throws FindException;
    Collection find(Date fromTime, Date toTime, Level fromLevel, Level toLevel, Class[] recordClasses, int maxRecords) throws FindException;

    long save(AuditRecord rec) throws SaveException;

}
