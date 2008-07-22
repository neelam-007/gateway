package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;

/**
 * Provides the ability to do CRUD operations on SinkConfiguration rows in the database.
 */
public interface SinkManager extends EntityManager<SinkConfiguration, EntityHeader> {

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

}
