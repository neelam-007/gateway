package com.l7tech.gateway.common.log;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.LOG_SINK;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Provides a remote interface for creating, reading, updating and deleting
 * Gateway log sinks.
 *
 * @see SinkConfiguration
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Administrative
@Secured(types=LOG_SINK)
public interface LogSinkAdmin extends LogAccessAdmin {

    String  ROLE_NAME_TYPE_SUFFIX = "Log Sink";

    /**
     * Download all log sink records.
     *
     * @return a List of SinkConfiguration instances.  Never null.
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Administrative(background=true)
    @Secured(types=LOG_SINK, stereotype=FIND_ENTITIES)
    Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException;

    /**
     * Download a specific SinkConfiguration instance identified by its primary key.
     *
     * @param goid the object ID of the SinkConfiguration instance to download.  Required.
     * @return the requested SinkConfiguration instance.  Never null.
     * @throws FindException if no SinkConfiguration is found with the specified oid, or
     *                       if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=LOG_SINK, stereotype=FIND_ENTITIES)
    SinkConfiguration getSinkConfigurationByPrimaryKey(Goid goid) throws FindException;

    /**
     * Store the specified new or existing SinkConfiguration. If the specified {@link SinkConfiguration} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     *
     * @param sinkConfiguration  the log sink configuration to save.  Required.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated for some reason
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException;

    /**
     * Delete a specific SinkConfiguration instance identified by its primary key.
     *
     * @param goid the object ID of the SinkConfiguration instance to delete.  Required.
     * @throws DeleteException if there is a problem deleting the object
     * @throws FindException if the object cannot be found
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deleteSinkConfiguration(Goid goid) throws DeleteException, FindException;

    /**
     * Creates a new syslog sink based on the provided configuration and sends a test message to it.
     *
     * @param sinkConfiguration The sink configuration to use
     * @param message The test message contents
     * @return Whether the message was successfully sent or not
     */
    @Secured(stereotype = TEST_CONFIGURATION)
    boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message);

    /**
     * Access the reserved file system space.
     *
     * <p>The combined space for all logs must not exceed the allowed space.</p>
     *
     * <p>The reserved space will only include space allocated for enabled
     * sinks.</p>
     *
     * @return The reserved space in bytes.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    long getReservedFileSize();

    /**
     * Access the maximum file system space available for use.
     *
     * @return The maximum space in bytes.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    long getMaximumFileSize();

    @Override
    @Transactional(readOnly=true)
    @Secured(types=LOG_SINK, stereotype=ENTITY_OPERATION, relevantArg=1, otherOperation = "log-viewer")
    Collection<LogFileInfo> findAllFilesForSinkByNode( String nodeId, Goid sinkId ) throws FindException;

    @Override
    @Administrative(background=true)
    @Transactional(readOnly=true)
    @Secured(types=LOG_SINK, stereotype=ENTITY_OPERATION, relevantArg=1, otherOperation = "log-viewer")
    LogSinkData getSinkLogs( String nodeId, Goid sinkId, String file, LogSinkQuery query ) throws FindException;
}
