package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.FindException;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Administrative API for read access to Gateway logs.
 */
@Transactional(propagation=REQUIRED)
public interface LogAccessAdmin {

    /**
     * Download all sink files for the sink in the node
     *
     * @param nodeId  node to get sink configurations from
     * @param sinkId  sink id to get files for
     * @return a collection of log file information.  Never null.
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true, noRollbackFor = Throwable.class)
    Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, long sinkId) throws FindException;

    /**
     * Get log data from the specified node/sink/file.
     *
     * @param nodeId  node to get sink configurations from
     * @param sinkId  associated log sink id
     * @param file the file associated with the log sink to read
     * @param startPosition  -1 for the first chunk
     * @param fromEnd true to offset from the end of the file
     * @return The log data
     */
    @Transactional(readOnly=true, noRollbackFor = Throwable.class)
    LogSinkData getSinkLogs(String nodeId, long sinkId, String file, long startPosition, boolean fromEnd) throws FindException;

}
