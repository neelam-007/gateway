package com.l7tech.server.transport;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.gateway.common.transport.SsgConnector;

/**
 * Interface that provides the ability to do CRUD operations on SsgConnector rows in the database.
 */
public interface SsgConnectorManager extends EntityManager<SsgConnector, EntityHeader> {
    /**
     * Translate a bind address that references an interface tag into a concrete IP address.
     *
     * @param bindAddress either an IP address or the name of a configured interface tag, ie "vmnet8"
     * @return dotted decimal IP for the corresponding local interface, ie "192.168.1.1"
     */
    String translateBindAddress(String bindAddress, int port) throws ListenerException;
}
