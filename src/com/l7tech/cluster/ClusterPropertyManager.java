package com.l7tech.cluster;

import com.l7tech.objectmodel.*;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface ClusterPropertyManager extends EntityManager<ClusterProperty, EntityHeader> {
    ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException;
    String getProperty(String key) throws FindException;
    void update(ClusterProperty clusterProperty) throws UpdateException;
}
