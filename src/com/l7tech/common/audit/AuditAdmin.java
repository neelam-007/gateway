/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.objectmodel.FindException;

import java.io.Serializable;
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
     * Get the level below which the server will not record audit events of type message.
     * @return the level currently applicable
     * @throws RemoteException
     */
    Level serverMessageAuditThreshold() throws RemoteException;

    /**
     * Delete all sub-SEVERE AuditRecords that are more than 168 hours old (by default), while producing new audit
     * record documenting that this action has been performed (and by which administrator).
     */
    void deleteOldAuditRecords() throws RemoteException;

    /**
     * Create a context for downloading audit IDs.  The context will expire after ten minutes of inactivity.
     * @return a OpaqueId for passing to downloadNextChunk().  never null.
     * @throws RemoteException if there is a problem preparing the download context.
     */
    OpaqueId downloadAllAudits() throws RemoteException;

    /** Represents a chunk of audits being downloaded. */
    public static final class DownloadChunk implements Serializable {
        public final long auditsDownloaded;
        public final long approxTotalAudits;
        public final byte[] chunk;

        public DownloadChunk(long auditsDownloaded, long approxTotalAudits, byte[] chunk) {
            this.auditsDownloaded = auditsDownloaded;
            this.approxTotalAudits = approxTotalAudits;
            this.chunk = chunk;
        }
    }

    /**
     * Download the next chunk of audit IDs.
     * @param context
     * @return
     * @throws RemoteException
     */
    DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException;

    /**
     * The minimum age (in hours) that an Audit record must be before it can be purged.
     * @return the number of hours old that an AuditRecord can be before it can be purged.
     * @throws RemoteException
     */
    int serverMinimumPurgeAge() throws RemoteException;
}
