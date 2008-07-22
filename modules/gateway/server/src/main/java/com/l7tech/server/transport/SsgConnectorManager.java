package com.l7tech.server.transport;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.gateway.common.transport.SsgConnector;

/**
 * Interface that provides the ability to do CRUD operations on SsgConnector rows in the database.
 */
public interface SsgConnectorManager extends EntityManager<SsgConnector, EntityHeader> {
}
