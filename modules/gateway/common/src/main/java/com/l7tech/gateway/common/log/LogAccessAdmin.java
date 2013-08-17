package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Administrative API for read access to Gateway logs.
 *
 * WARNING
 * WARNING
 * WARNING If you change this API you MUST update "cluster-servlet.xml" to
 * WARNING permit the classes used in the parameters (node to node whitelist)
 * WARNING
 * WARNING
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
    Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, Goid sinkId) throws FindException;

    /**
     * Get log data from the specified node/sink/file.
     *
     * @param nodeId  node to get sink configurations from
     * @param sinkId  associated log sink id
     * @param file the file associated with the log sink to read
     * @param query  query data
     * @return The log data
     */
    @Transactional(readOnly=true, noRollbackFor = Throwable.class)
    LogSinkData getSinkLogs(String nodeId, Goid sinkId, String file, LogSinkQuery query) throws FindException;

}
