package com.l7tech.server.cluster;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface ClusterPropertyManager extends GoidEntityManager<ClusterProperty, EntityHeader> {
    ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException;
    String getProperty(String key) throws FindException;

    /**
     * Set a cluster property with the specified key to the specified value, either creating a new cluster
     * property or updating the value of an existing one as appropriate.
     *
     * @param key   the key
     * @param value the new value
     * @return a ClusterProperty instance that has been saved to the database.  Never null.
     * @throws FindException  if there is a problem searching for an existing row with this key
     * @throws SaveException  if there is a problem saving a new cluster property with this key
     * @throws UpdateException if there is a problem updating an existing cluster property with this key
     */
    public ClusterProperty putProperty(String key, String value) throws FindException, SaveException, UpdateException;
}
