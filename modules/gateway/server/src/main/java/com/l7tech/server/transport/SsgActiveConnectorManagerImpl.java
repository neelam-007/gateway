package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Hibernate entity manager for SSG active connectors.
 */
public class SsgActiveConnectorManagerImpl
        extends HibernateEntityManager<SsgActiveConnector, EntityHeader>
        implements SsgActiveConnectorManager
{
    @NotNull
    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType( @NotNull final String type ) throws FindException {
       return findByPropertyMaybeNull( "type", type );
    }

    @Override
    public Class<SsgActiveConnector> getImpClass() {
        return SsgActiveConnector.class;
    }
}
