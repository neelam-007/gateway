package com.l7tech.server.transport;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.common.transport.SsgConnector;

/**
 * Implementation of {@link SsgConnectorManager}.
 */
public class SsgConnectorManagerImpl
        extends HibernateEntityManager<SsgConnector, EntityHeader>
        implements SsgConnectorManager
{
    public SsgConnectorManagerImpl() {
    }

    public Class<? extends Entity> getImpClass() {
        return SsgConnector.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgConnector.class;
    }

    public String getTableName() {
        return "connector";
    }

    public EntityType getEntityType() {
        return EntityType.CONNECTOR;
    }
}
