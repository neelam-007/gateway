package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Simple (non-database specific) manager for audit records
 */
public interface SimpleAuditRecordManager extends EntityManager<AuditRecord, AuditRecordHeader> {

    /**
     * Find {@link AuditRecordHeader}s that match the given criteria.
     * (This is the same as the above find() method except it returns headers only as opposed to entire records.
     * @param criteria criteria The search settings (must not be null)
     * @return The collection of audit records (not null)
     * @throws FindException If an error occurs
     */
    List<AuditRecordHeader> findHeaders( AuditSearchCriteria criteria ) throws FindException;

    /**
     * Find the count of records that match the given criteria.
     *
     * @param criteria The search settings (not null)
     * @return The count
     * @throws FindException If an error occurs
     */
    int findCount( AuditSearchCriteria criteria ) throws FindException;

    /**
     * Find a "page" of audit records that match the given criteria.
     *
     * <p>The records returned as part of the page will be determined by the
     * sort property and order.</p>
     *
     * @param sortProperty The property to sort by.
     * @param ascending True to sort in ascending order
     * @param offset The start record offset
     * @param count The "page" size
     * @param criteria The search settings (not null)
     * @return The page of records (maximum number is "count")
     * @throws FindException if an error occurs
     */
    Collection<AuditRecord> findPage( SortProperty sortProperty, boolean ascending, int offset, int count, AuditSearchCriteria criteria ) throws FindException;

    /**
     * Get a digest value for each audit record whose id is contained in auditRecordIds.
     *
     * @param auditRecordIds the list of audit records to get digests for. Cannot be null.
     * @return a map of audit record id to the digest. If the audit record does not exist, then this map will not contain
     * an entry for the record object id. Returned map may be empty but never null
     * @throws FindException any problems searching the db. Note FindExceptions are not thrown when records do not exist.
     */
    Map<Long, byte[]> getDigestForAuditRecords( Collection<Long> auditRecordIds ) throws FindException;

    enum SortProperty {
        TIME("millis"), MESSAGE("message"), LEVEL("strLvl");
        private final String name;
        SortProperty( String name ) {
            this.name = name;
        }
        String getPropertyName() {
            return name;
        }
    }
}
