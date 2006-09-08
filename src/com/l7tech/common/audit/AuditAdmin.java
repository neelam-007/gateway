/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.audit;

import static com.l7tech.common.security.rbac.EntityType.AUDIT_RECORD;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * The API for administering the SecureSpan Gateway's audit system.
 *
 * Note that the API does not permit the modification of audit records in any way,
 * and only permits their deletion using a process that generates a permanent audit record.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=AUDIT_RECORD)
public interface AuditAdmin extends GenericLogAdmin {
    /**
     * Retrieves the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @param oid the numeric object identifier of the record to retrieve
     * @return the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @throws FindException if there was a problem retrieving Audit records from the database
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Retrieves a collection of {@link AuditRecord}s matching the provided criteria.  May be empty, but never null.
     * @param criteria an {@link AuditSearchCriteria} describing the search criteria.  Must not be null.
     * @throws FindException if there was a problem retrieving Audit records from the database
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection find(AuditSearchCriteria criteria) throws FindException, RemoteException;

    /**
     * Get the level below which the server will not record audit events of type {@link MessageSummaryAuditRecord}.
     * @return the level currently applicable. Never null.
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Transactional(propagation=SUPPORTS)
    Level serverMessageAuditThreshold() throws RemoteException;

    /**
     * Get the level below which the server will not record audit detail records.
     * @return the level currently applicable. Never null.
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Transactional(propagation=SUPPORTS)
    Level serverDetailAuditThreshold() throws RemoteException;

    /**
     * Delete all sub-SEVERE AuditRecords that are more than 168 hours old (by default), while producing a new audit
     * record at {@link Level#SEVERE} documenting that this action has been performed (and by which administrator).
     *
     * {@link Level#SEVERE} AuditRecords can never be deleted.
     *
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Secured(stereotype=DELETE_MULTI)
    void deleteOldAuditRecords() throws RemoteException, DeleteException;

    @Secured(stereotype=FIND_ENTITIES)
    Collection<AuditRecord> findAuditRecords(String nodeid, long startMsgNumber, long endMsgNumber,
                                             Date startMsgDate, Date endMsgDate, int size)
                                      throws RemoteException, FindException;

    /**
     * Create a context for downloading audit records.  The context will expire after ten minutes of inactivity.
     * @param chunkSizeInBytes number of bytes per download chunk.  If zero, default of 8192 will be used.
     * @return a OpaqueId for passing to downloadNextChunk().  never null.
     * @throws RemoteException if there is a problem preparing the download context.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
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
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException;

    /**
     * The minimum age (in hours) that an Audit record must be before it can be purged.
     * @return the minimum number of hours old that an AuditRecord must be before it can be purged.
     * @throws RemoteException if there was a problem communicating with the Gateway
     */
    @Transactional(propagation=SUPPORTS)
    int serverMinimumPurgeAge() throws RemoteException;
}
