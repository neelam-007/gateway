package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.objectmodel.PropertySearchableEntityManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for SSG active connectors.
 */
public interface SsgActiveConnectorManager extends GoidEntityManager<SsgActiveConnector, SsgActiveConnectorHeader>, PropertySearchableEntityManager<SsgActiveConnectorHeader> {

    /**
     * Find active connectors of the given type.
     *
     * @param type The connector type (required)
     * @return The collection of connectors (never null)
     * @throws FindException If an error occurs
     */
    @NotNull
    Collection<SsgActiveConnector> findSsgActiveConnectorsByType( @NotNull String type ) throws FindException;

    /**
     * Find an entity by old oid
     * @param oid the old oid to search for
     * @return the jms connection object if found.  null if not
     * @throws FindException
     */
    public SsgActiveConnector findByOldOid(long oid) throws FindException ;
}
