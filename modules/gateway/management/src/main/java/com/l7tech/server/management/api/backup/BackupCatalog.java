/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.backup;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.NodeConfig;

import java.util.Map;

/**
 * A catalog, for backup/restore purposes, of {@link com.l7tech.objectmodel.Entity configuration/monitoring entities},
 * {@link com.l7tech.server.management.config.node.NodeConfig service node configurations},
 * {@link com.l7tech.server.management.config.host.HostConfig host configurations} etc.
 *  
 * @author alex
 */
public interface BackupCatalog {
    /**
     * @return an Iterable of headers for all entities in the catalog
     */
    Iterable<EntityHeader> getAllHeaders();

    /**
     * @return a Map, keyed by EntityType, of iterables, each consisting of headers for all entities of that type in the
     *         catalog.
     */
    Map<EntityType, Iterable<EntityHeader>> getHeadersByType();

    /**
     * @return an optional backed-up Node (null if this catalog does not contain a Node)
     */
    NodeConfig getNodeConfig();

    /**
     * @return an optional backed-up Host (null if this catalog does not contain a Host)
     */
    HostConfig getHostConfig();
}
