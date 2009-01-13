package com.l7tech.server.audit;

/**
 * @author jbufu
 */
public interface ArchiveReceiver {

    /**
     * Is this receiver available for use?
     *
     * @return true if enabled.
     */
    boolean isEnabled();

    /**
     * Archives the provided audit record range. The records must be archived in order,
     * so that partially successfull transfers are not retried..
     *
     * @param startOid  The object id of the first audit record to be archived.
     * @param endOid    The object id of the last audit record to be archived. 
     * @return          ExportedInfo metadata about the current archive job, or null if it failed.
     */
    AuditExporter.ExportedInfo archiveRecords(long startOid, long endOid);

    /**
     * Flushes the ArchiveReceiver's buffers, if any.
     *
     * All data that was sent to an ArchiveReceiver before a flush() call that returns true
     * can be considered safe to delete.
     *
     * @return true if the flush operation was successfull, false if it failed for any reason.
     */
    boolean flush();
}

