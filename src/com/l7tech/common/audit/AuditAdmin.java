/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.objectmodel.FindException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditAdmin extends Remote {
    /**
     * Retrieves the {@link AuditRecord} with the given oid.
     */
    AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves a collection of {@link AuditRecord}s matching the provided criteria.
     */ 
    Collection find(Date fromTime, Date toTime, Level fromLevel, Level toLevel, Class[] recordClasses, int maxRecords) throws FindException, RemoteException;
}
