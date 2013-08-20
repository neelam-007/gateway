/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Config;

import java.util.Collection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditRecordManager extends GoidEntityManager<AuditRecord, AuditRecordHeader> {
    public static final String PROP_NAME = "name";
    public static final String PROP_MSG = "message";

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

    /**
     * Find {@link com.l7tech.gateway.common.audit.AuditRecordHeader}s that match the given criteria.
     * (This is the same as the above find() method except it returns headers only as opposed to entire records.
     * @param criteria criteria The search settings (must not be null)
     * @return The collection of audit records (not null)
     * @throws FindException If an error occurs
     */
    List<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException;

    /**
     * Get a validated config containing audit records.
     * //todo: implement for other properties
     * @return Config which validates values.
     */
    Config getAuditValidatedConfig();

    /**
     * Get a digest value for each audit record whose id is contained in auditRecordIds.
     *
     * @param auditRecordIds the list of audit records to get digests for. Cannot be null.
     * @return a map of audit record id to the digest. If the audit record does not exist, then this map will not contain
     * an entry for the record object id. Returned map may be empty but never null
     * @throws FindException any problems searching the db. Note FindExceptions are not thrown when records do not exist.
     */
    Map<String, byte[]> getDigestForAuditRecords(Collection<String> auditRecordIds) throws FindException;

    /**
     * Delete old audit records.
     *
     * <p>Note that if your request deletion of audits that are not of the minimum
     * permitted age for the system wide settings then the minAge value will be
     * silently ignored (as though -1 was used)</p>
     *
     * @param minAge The minimum age for deleted audits (millis, -1 for default)
     * @throws DeleteException If the deletion fails.
     */
    void deleteOldAuditRecords( long minAge ) throws DeleteException;

    /**
     * Find the count of records that match the given criteria.
     *
     * @param criteria The search settings (not null)
     * @return The count
     * @throws FindException If an error occurs
     */
    int findCount( final AuditSearchCriteria criteria ) throws FindException;

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
    Collection<AuditRecord> findPage( final SortProperty sortProperty, final boolean ascending, final int offset, final int count, final AuditSearchCriteria criteria ) throws FindException;

    long getMinMills(long lowerLimit) throws SQLException;

    public int deleteRangeByTime(final long start, final long end) throws SQLException;

    /**
     * Get the max table space size in bytes for the current database.
     *
     * @return  Max table space size in bytes, or -1 if not defined
     * @throws FindException if an error was encountered and the value could not be retrieved
     */
    public long getMaxTableSpace() throws FindException;
    
    public long getCurrentUsage() throws FindException;
}

