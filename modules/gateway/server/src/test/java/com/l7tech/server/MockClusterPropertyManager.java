package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.*;
import com.l7tech.server.cluster.ClusterPropertyManager;

/**
 * Mock CPM, not very functional, should probably use serverconfig.properties to get default values.
 */
public class MockClusterPropertyManager
        extends EntityManagerStub<ClusterProperty,EntityHeader>
        implements ClusterPropertyManager
{
    public MockClusterPropertyManager() {
        super();
    }

    public MockClusterPropertyManager( final ClusterProperty... entitiesIn ) {
        super(entitiesIn);
    }

    @Override
    public String getProperty( final String key ) throws FindException {
        ClusterProperty prop = findByUniqueName(key);
        if (prop != null) {
            return prop.getValue();
        }
        return null;
    }

    @Override
    public ClusterProperty putProperty( final String key,
                                        final String value ) throws FindException, SaveException, UpdateException {
        ClusterProperty prop = findByUniqueName(key);
        if (prop == null) {
            prop = new ClusterProperty(key, value);
            long oid = save(prop);
            if (oid != prop.getOid()) prop.setOid(oid);
            return prop;
        }

        prop.setValue(value);
        update(prop);
        return prop;
    }

    @Override
    public ClusterProperty getCachedEntityByName(String name, int maxAge) throws FindException {
        return findByUniqueName(name);
    }

    @Override
    public Class<ClusterProperty> getImpClass() {
        return ClusterProperty.class;
    }

    @Override
    public Class<ClusterProperty> getInterfaceClass() {
        return ClusterProperty.class;
    }

}
