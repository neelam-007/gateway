package com.l7tech.common.log;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.common.security.rbac.Secured;
import static com.l7tech.common.security.rbac.MethodStereotype.FIND_ENTITIES;
import static com.l7tech.common.security.rbac.MethodStereotype.SAVE_OR_UPDATE;
import static com.l7tech.common.security.rbac.MethodStereotype.DELETE_BY_ID;
import static com.l7tech.common.security.rbac.EntityType.LOG_SINK;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.util.Collection;

/**
 * Provides a remote interface for creating, reading, updating and deleting
 * Gateway log sinks.
 *
 * @see SinkConfiguration
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=LOG_SINK)
public interface LogSinkAdmin {
    /**
     * Download all log sink records.
     *
     * @return a List of SinkConfiguration instances.  Never null.
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=LOG_SINK, stereotype=FIND_ENTITIES)
    Collection<SinkConfiguration> findAllSinkConfigurations() throws FindException;

    /**
     * Download a specific SinkConfiguration instance identified by its primary key.
     *
     * @param oid the object ID of the SinkConfiguration instance to download.  Required.
     * @return the requested SinkConfiguration instance.  Never null.
     * @throws FindException if no SinkConfiguration is found with the specified oid, or
     *                       if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=LOG_SINK, stereotype=FIND_ENTITIES)
    SinkConfiguration getSinkConfigurationByPrimaryKey(long oid) throws FindException;

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
    long saveSinkConfiguration(SinkConfiguration sinkConfiguration) throws SaveException, UpdateException;

    /**
     * Delete a specific SinkConfiguration instance identified by its primary key.
     *
     * @param oid the object ID of the SinkConfiguration instance to delete.  Required.
     * @throws DeleteException if there is a problem deleting the object
     * @throws FindException if the object cannot be found
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deleteSinkConfiguration(long oid) throws DeleteException, FindException;

    /**
     * Creates a new syslog sink based on the provided configuration and sends a test message to it.
     *
     * @param sinkConfiguration The sink configuration to use
     * @param message The test message contents
     * @return Whether the message was successfully sent or not
     */
    boolean sendTestSyslogMessage(SinkConfiguration sinkConfiguration, String message);
}
