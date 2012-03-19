package com.l7tech.server.audit;

import com.l7tech.objectmodel.*;

import java.sql.SQLException;

/**
 * @author alex
 */
public interface AuditRecordManager extends SimpleAuditRecordManager {

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

    long getMinOid(long lowerLomit) throws SQLException;

    public int deleteRangeByOid(final long start, final long end) throws SQLException;

    /**
     * Get the max table space size in bytes for the current database.
     *
     * @return  Max table space size in bytes, or -1 if not defined
     * @throws FindException if an error was encountered and the value could not be retrieved
     */
    public long getMaxTableSpace() throws FindException;
    
    public long getCurrentUsage() throws FindException;
}
