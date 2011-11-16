package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for SSG active connectors.
 */
public interface SsgActiveConnectorManager extends EntityManager<SsgActiveConnector, EntityHeader> {

    /**
     * Find active connectors of the given type.
     *
     * @param type The connector type (required)
     * @return The collection of connectors (never null)
     * @throws FindException If an error occurs
     */
    @NotNull
    Collection<SsgActiveConnector> findSsgActiveConnectorsByType( @NotNull String type ) throws FindException;
}
