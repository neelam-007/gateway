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

import java.util.Collection;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public interface AuditRecordManager extends EntityManager<AuditRecord, AuditRecordHeader> {
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
     * Find audit records that match the given criteria.
     *
     * @param criteria The search settings (must not be null)
     * @return The collection of audit records (not null)
     * @throws FindException If an error occurs
     */
    Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException;

    /**
     * Find {@link com.l7tech.gateway.common.audit.AuditRecordHeader}s that match the given criteria.
     * (This is the same as the above find() method except it returns headers only as opposed to entire records.
     * @param criteria criteria The search settings (must not be null)
     * @return The collection of audit records (not null)
     * @throws FindException If an error occurs
     */
    Collection<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException;

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

    long getMinOid(long lowerLomit) throws SQLException;

    public int deleteRangeByOid(final long start, final long end) throws SQLException;

    public long getMaxTableSpace() throws FindException;
    
    public long getCurrentUsage() throws FindException;
}
