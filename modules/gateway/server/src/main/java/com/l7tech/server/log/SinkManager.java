package com.l7tech.server.log;

import com.l7tech.gateway.common.log.LogFileInfo;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.gateway.common.log.LogSinkQuery;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * Provides the ability to do CRUD operations on SinkConfiguration rows in the database.
 */
public interface SinkManager extends EntityManager<SinkConfiguration, EntityHeader>, RoleAwareEntityManager<SinkConfiguration> {

    /**
     * Get the file storage allocation for logs.
     *
     * @return The size in bytes.
     */
    long getMaximumFileStorageSpace();

    /**
     * Get the used file storage space in bytes.
     *
     * <p>This will calculate the space allocated for all currently enabled
     * log sinks.</p>
     *
     * @return The size in bytes.
     */
    long getReservedFileStorageSpace();

    /**
     * Test the given sinkConfiguration (useful for syslog)
     *
     * @param sinkConfiguration The configuration to test.
     * @param testMessage The message to send.
     * @return True if the message may have been sent
     */
    boolean test(SinkConfiguration sinkConfiguration, String testMessage);

    /**
     * Get the MessageSink used for publishing information.
     *
     * @return The publishing MessageSink
     */
    MessageSink getPublishingSink();

    /**
     * Get all log sink configurations in that node
     * @param nodeId node to list files for
     * @param sinkId the sink to list files for
     * @return a List of SinkConfiguration instances.  Never null.
     * @throws FindException if there is a problem reading from the database
     */
    Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, Goid sinkId) throws FindException;

    /**
     * Fetch the specified log data
     *
     * @param nodeId The node identifier
     * @param sinkId The sink identifier
     * @param file The file
     * @param query query data
     * @return The log data
     */
    LogSinkData getSinkLogs(String nodeId, Goid sinkId, String file,LogSinkQuery query) throws FindException;
}
