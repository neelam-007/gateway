/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.objectmodel.FindException;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditAdmin extends GenericLogAdmin {
    /**
     * Retrieves the {@link AuditRecord} with the given oid.
     */
    AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves a collection of {@link AuditRecord}s matching the provided criteria.
     */ 
    Collection find(AuditSearchCriteria criteria) throws FindException, RemoteException;
}
