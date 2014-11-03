package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.HibernateEntityManager;

/**
 * Created by yuri on 10/31/14.
 */
public class CassandraConnectionEntityManagerImpl extends HibernateEntityManager<CassandraConnection, EntityHeader> implements CassandraConnectionEntityManager{

    @Override
    public Class<? extends PersistentEntity> getImpClass() {
        return CassandraConnection.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    @Override
    public CassandraConnection getCassandraConnectionEntity(String connectionName) throws FindException {
        return findByUniqueName(connectionName);
    }

}
