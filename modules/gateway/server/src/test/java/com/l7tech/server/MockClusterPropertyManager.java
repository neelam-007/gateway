package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.*;
import com.l7tech.server.cluster.ClusterPropertyManager;

/**
 * Mock CPM, not currently functional, should probably use serverconfig.properties to get default values.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class MockClusterPropertyManager
        extends EntityManagerStub<ClusterProperty,EntityHeader>
        implements ClusterPropertyManager
{
    public String getProperty(String key) throws FindException {
        return "";
    }

    public ClusterProperty putProperty(String key, String value) throws FindException, SaveException, UpdateException {
        throw new UpdateException("Not implemented");
    }

    public void update(ClusterProperty clusterProperty) throws UpdateException {
        throw new UpdateException("Not implemented");
    }

    public ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException {
        return null;
    }

    public ClusterProperty findByUniqueName(String name) throws FindException {
        return null;
    }

    public Class getImpClass() {
        return ClusterProperty.class;
    }

    public Class getInterfaceClass() {
        return ClusterProperty.class;
    }

    public EntityType getEntityType() {
        return EntityType.ANY;
    }

    public String getTableName() {
        return "cluster_properties";
    }
}
