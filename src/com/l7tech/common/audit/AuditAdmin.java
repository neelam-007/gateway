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
 * The API for administering the SecureSpan Gateway's audit system.
 *
 * Note that the API does not permit the modification of audit records in any way,
 * and only permits their deletion using a process that generates a permanent audit record.
 *
 * @author alex
 * @version $Revision$
 */
public interface AuditAdmin extends GenericLogAdmin {
    /**
     * Retrieves the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @param oid the numeric object identifier of the record to retrieve
     * @return the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @throws FindException if there was a problem retrieving Audit records from the database
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves a collection of {@link AuditRecord}s matching the provided criteria.  May be empty, but never null.
     * @param criteria an {@link AuditSearchCriteria} describing the search criteria.  Must not be null.
     * @throws FindException if there was a problem retrieving Audit records from the database
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    Collection find(AuditSearchCriteria criteria) throws FindException, RemoteException;

    /**
     * Get the level below which the server will not record audit events of type {@link MessageSummaryAuditRecord}.
     * @return the level currently applicable. Never null.
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    Level serverMessageAuditThreshold() throws RemoteException;

    /**
     * Delete all sub-SEVERE AuditRecords that are more than 168 hours old (by default), while producing a new audit
     * record at {@link Level#SEVERE} documenting that this action has been performed (and by which administrator).
     *
     * {@link Level#SEVERE} AuditRecords can never be deleted.
     *
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    void deleteOldAuditRecords() throws RemoteException;

    /**
     * Create a context for downloading audit records.  The context will expire after ten minutes of inactivity.
     * @param chunkSizeInBytes number of bytes per download chunk.  If zero, default of 8192 will be used.
     * @return a OpaqueId for passing to downloadNextChunk().  never null.
     * @throws RemoteException if there is a problem preparing the download context.
     */
    OpaqueId downloadAllAudits(int chunkSizeInBytes) throws RemoteException;

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
     * Download the next chunk of audit records.
     * @param context the {@link OpaqueId} that was returned by a previous call to {@link #downloadAllAudits(int)}
     * @return the next {@link DownloadChunk}, or null if the last chunk has already been sent.
     * @throws RemoteException if there is a problem preparing the chunk.
     */
    DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException;

    /**
     * The minimum age (in hours) that an Audit record must be before it can be purged.
     * @return the minimum number of hours old that an AuditRecord must be before it can be purged.
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    int serverMinimumPurgeAge() throws RemoteException;
}
