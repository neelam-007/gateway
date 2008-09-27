/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.gateway.common.audit;

import static com.l7tech.gateway.common.security.rbac.EntityType.AUDIT_RECORD;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.util.OpaqueId;
import com.l7tech.gateway.common.logging.GenericLogAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.admin.Administrative;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
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
@Administrative
public interface AuditAdmin extends GenericLogAdmin {
    /**
     * Retrieves the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @param oid the numeric object identifier of the record to retrieve
     * @return the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @throws FindException if there was a problem retrieving Audit records from the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    @Administrative(licensed=false)
    AuditRecord findByPrimaryKey(long oid) throws FindException;

    /**
     * Retrieves a collection of {@link AuditRecord}s matching the provided criteria.  May be empty, but never null.
     * @param criteria an {@link AuditSearchCriteria} describing the search criteria.  Must not be null.
     * @throws FindException if there was a problem retrieving Audit records from the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false)
    Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException;

    /**
     * Retrieves a collection of {@link AuditRecordHeader}s matching the provided criteria.  May be empty, but never null.
     * (This is the same as the above method except it retrives AuditRecordHeaders instead of entire AuditRecords.)
     * @param criteria
     * @return criteria an {@link AuditSearchCriteria} describing the search criteria.  Must not be null.
     * @throws FindException if there was a problem retrieving Audit records from the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false)
    Collection<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException;

    /**
     * Get the level below which the server will not record audit events of type {@link MessageSummaryAuditRecord}.
     * @return the level currently applicable. Never null.
     */
    @Transactional(propagation=SUPPORTS)
    @Administrative(licensed=false)
    Level serverMessageAuditThreshold();

    /**
     * Get the level below which the server will not record audit detail records.
     * @return the level currently applicable. Never null.
     */
    @Transactional(propagation=SUPPORTS)
    @Administrative(licensed=false)
    Level serverDetailAuditThreshold();

    /**
     * Delete all sub-SEVERE AuditRecords that are more than 168 hours old (by default), while producing a new audit
     * record at {@link Level#SEVERE} documenting that this action has been performed (and by which administrator).
     *
     * {@link Level#SEVERE} AuditRecords can never be deleted.
     *
     * @throws DeleteException if there was a problem when deleting old audit records
     */
    @Secured(stereotype=DELETE_MULTI)
    @Administrative(licensed=false)
    void deleteOldAuditRecords() throws DeleteException;

    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false)
    Collection<AuditRecord> findAuditRecords(String nodeid, Date startMsgDate, Date endMsgDate, int size)
                                      throws FindException;

    /**
     * Get the date for the last acknowledged audit event.
     *
     * @return The date or null if not set
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    Date getLastAcknowledgedAuditDate();

    /**
     * Set and return the date for the last acknowledged audit event.
     *
     * <p>The acknowledged time is updated to the current server time.</p>
     *
     * @return The date or null if not set
     */
    @Administrative(licensed=false)
    Date markLastAcknowledgedAuditDate();

    /**
     * Create a context for downloading audit records.  The context will expire after ten minutes of inactivity.
     * @param fromTime          minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime            maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids       OIDs of services (thus filtering to service events only); null for no service filtering
     * @param chunkSizeInBytes  number of bytes per download chunk.  If zero, default of 8192 will be used.
     * @return a OpaqueId for passing to downloadNextChunk().  never null.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false)
    OpaqueId downloadAllAudits(long fromTime,
                               long toTime,
                               long[] serviceOids,
                               int chunkSizeInBytes);

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
     * @param context the {@link OpaqueId} that was returned by a previous call to {@link #downloadAllAudits}
     * @return the next {@link DownloadChunk}, or null if the last chunk has already been sent.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false)
    DownloadChunk downloadNextChunk(OpaqueId context);

    /**
     * The minimum age (in hours) that an Audit record must be before it can be purged.
     * @return the minimum number of hours old that an AuditRecord must be before it can be purged.
     */
    @Transactional(propagation=SUPPORTS)
    @Administrative(licensed=false)
    int serverMinimumPurgeAge();
}
