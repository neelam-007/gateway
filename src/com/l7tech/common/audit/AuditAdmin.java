/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.objectmodel.FindException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Level;

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

    /**
     * Delete all sub-SEVERE AuditRecords that are more than 168 hours old (by default), while producing new audit
     * record documenting that this action has been performed (and by which administrator).
     */
    void deleteOldAuditRecords() throws RemoteException;

    interface RemoteBulkStream extends Remote {
        /** @return the next chunk of the stream, or null if the stream has finished. */
        byte[] nextChunk() throws RemoteException;
    }

    /** @return a RemoteBulkStream from which can be read a zip file containing the exported audit events along with a signature. */
    RemoteBulkStream downloadAllAudits() throws RemoteException;

    /**
     * Get the level below which the server will not record audit events of type message.
     * @return the level currently applicable
     * @throws RemoteException
     */
    Level serverMessageAuditThreshold() throws RemoteException;

    /**
     * The minimum age (in hours) that an Audit record must be before it can be purged.
     * @return the number of hours old that an AuditRecord can be before it can be purged.
     * @throws RemoteException
     */
    int serverMinimumPurgeAge() throws RemoteException;
}
