package com.l7tech.server.cluster;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface ClusterPropertyManager extends EntityManager<ClusterProperty, EntityHeader> {
    ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException;
    String getProperty(String key) throws FindException;
}
