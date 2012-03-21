package com.l7tech.gateway.common.audit;

import static com.l7tech.objectmodel.EntityType.AUDIT_RECORD;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.util.OpaqueId;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
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
public interface AuditAdmin extends AsyncAdminMethods{

    /**
     * Retrieves the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @param oid the numeric object identifier of the record to retrieve
     * @return the {@link AuditRecord} with the given oid, or null if no such record exists.
     * @throws FindException if there was a problem retrieving Audit records from the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    @Administrative(licensed=false)
    AuditRecord findByPrimaryKey(long oid) throws FindException;

    /**
     * Retrieves a collection of {@link AuditRecordHeader}s matching the provided criteria.  May be empty, but never null.
     * (This is the same as the above method except it retrives AuditRecordHeaders instead of entire AuditRecords.)
     * @param criteria
     * @return criteria an {@link AuditSearchCriteria} describing the search criteria.  Must not be null.
     * @throws FindException if there was a problem retrieving Audit records from the database
     */
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false, background = true)
    List<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException;

    /**
     * Get digests for audit records.
     *
     * @param auditRecordIds The set of audit records to retrieve digests for. Cannot be null.
     * @return Digests for the requested audit records. May not contain records for each id requested.
     * @throws FindException any problems searching.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    @Administrative(licensed=false, background = true)
    Map<Long, byte[]> getDigestsForAuditRecords(Collection<Long> auditRecordIds) throws FindException;

    /**
     * Checks if there are any audits found given the date and level to search for.
     * @param date  Starting date consider to be new audits
     * @param level The auditing level to be queried for
     * @return The date of the first available audit or 0 if none are available.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false, background = true)
    long hasNewAudits(Date date, Level level);

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

    /**
     * Signals the Audit Archiver to start the archive job immediately
     * (if it's not already currently being run by a node).
     */
    @Secured(types=AUDIT_RECORD, stereotype = DELETE_MULTI)
    void doAuditArchive();

    /**
     * Retrieves the Audit Archiver FTP destination configured for the cluster.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    ClusterProperty getFtpAuditArchiveConfig();

    /**
     * Saves / updates the Audit Archiver FTP destination with the provided config.
     *
     * @return  true if successfull, false if the save operation failed
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    void setFtpAuditArchiveConfig(ClusterProperty prop) throws UpdateException;

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

    /**
     * Check if the cluster property, "audit.signing" is enabled or not.
     * 
     * @return true if "audit.signing" is set as true.
     * @throws FindException thrown when the property cannot be found.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false, background = true)
    boolean isSigningEnabled() throws FindException;

    /**
     * Get how many audit records the gateway will get digests for at one time,
     * based on the value of cluster property "audit.validateSignature.maxrecords".
     *
     * @return max number of records for one request.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false, background = true)
    int getMaxDigestRecords();

    /**
     * Check if an audit viewer policy is available.
     *
     * @return true if available
     */
    @Transactional(readOnly = true)
    boolean isAuditViewerPolicyAvailable();

    /**
     * Invoke the audit viewer policy.
     *
     * @param auditRecordId audit record id
     * @param isRequest true if request should be input to audit viewer policy. Otherwise response is used.
     * @return String output from the AV policy or null if policy did not complete successfully.
     * @throws FindException if AV policy cannot be accessed.
     * @throws AuditViewerPolicyNotAvailableException if audit viewer policy is not available
     */
    @Secured(stereotype = ENTITY_OPERATION, otherOperation = "audit-viewer policy", relevantArg = 0)
    String invokeAuditViewerPolicyForMessage(long auditRecordId, boolean isRequest)
            throws FindException, AuditViewerPolicyNotAvailableException;

    /**
     * Invoke the audit viewer policy for a particular audit detail belonging to an audit record.
     *
     * This is intended to be used to invoke the audit viewer policy against audits which are created at message
     * processing time via the 'Add Audit Details' assertion, as such the detail message id is not required as it
     * is assumed to be either {@link AssertionMessages#USERDETAIL_WARNING} or {@link AssertionMessages#USERDETAIL_INFO},
     * although the other user detail messages with lower levels are also supported.
     *
     * The ordinal is used to pick the correct audit detail from the set of audit details for the
     * audit record.
     *
     * @param auditRecordId id of the audit record
     * @param ordinal position of the audit detail record to process within the set of audit details for the audit record.
     * @return String output of the audit policy or null if policy did not complete successfully.
     * @throws FindException if AV policy cannot be accessed.
     * @throws AuditViewerPolicyNotAvailableException if audit viewer policy is not available
     */
    @Secured(stereotype = ENTITY_OPERATION, otherOperation = "audit-viewer policy", relevantArg = 0)
    String invokeAuditViewerPolicyForDetail(long auditRecordId, long ordinal) throws FindException, AuditViewerPolicyNotAvailableException;

    /**
     * All user audit detail message ids.
     */
    Set<Integer> USER_DETAIL_MESSAGES = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(
            AssertionMessages.USERDETAIL_WARNING.getId(),
            AssertionMessages.USERDETAIL_INFO.getId(),
            AssertionMessages.USERDETAIL_FINE.getId(),
            AssertionMessages.USERDETAIL_FINER.getId(),
            AssertionMessages.USERDETAIL_FINEST.getId())));

    /**
     * Get all entity class names except those ignored or no-audit entities.
     * @return: the list of entity class names.
     */
    @Transactional(readOnly=true)
    Collection<String> getAllEntityClassNames();

    /**
     * Get the configured refresh period for the log type.
     *
     * @return the configured refresh period (seconds).
     */
    @Transactional(propagation= Propagation.SUPPORTS)
    int getSystemLogRefresh();

    /**
     * Find all jdbc audit sink entity instances.
     *
     * @return a collection of instances.  May be empty but never null.
     * @throws FindException if there is a problem reading the database
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<JdbcAuditSink> findAllJdbcAuditSinks() throws FindException;

    /**
     * Save a new or changed jdbc audit sink entity.
     *
     * @param entity the entity to save.  Required.
     * @return the oid assigned to the entity (useful when saving a new one)
     * @throws com.l7tech.objectmodel.SaveException if t here is a problem saving the entity
     * @throws UpdateException if there is a problem updatingan existing entity
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveJdbcAuditSink(JdbcAuditSink entity) throws SaveException, UpdateException;

    /**
     * Delete a jdbc audit sink entity.
     *
     * @param entity the entity to delete.  Required.
     * @throws DeleteException if entity cannot be deleted
     * @throws FindException if entity cannot be located before deletion
     */
    @Secured(stereotype=DELETE_ENTITY)
    void deleteJdbcAuditSink(JdbcAuditSink entity) throws DeleteException, FindException;

    /**
     * Test a audit sink configuration
     *
     * @param sinkConfig: the audit sink configuration to be tested.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    @Secured(types = EntityType.GENERIC, stereotype = FIND_ENTITIES)
    AsyncAdminMethods.JobId<String> testJdbcAuditSink(JdbcAuditSink sinkConfig);

    /**
     * create schema for specified audit sink config
     *
     * @param sinkConfig: the audit sink configuration to  create schema for.
     * @return created schema
     */
    @Transactional(readOnly=true)
    @Secured(types = EntityType.GENERIC, stereotype = FIND_ENTITIES)
    String createJdbcAuditSinkSchema(JdbcAuditSink sinkConfig);
}