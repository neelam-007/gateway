package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

public class CassandraConnectionEntityManagerStub
        extends EntityManagerStub<CassandraConnection, EntityHeader> implements CassandraConnectionEntityManager {

    public CassandraConnectionEntityManagerStub(final CassandraConnection... entitiesIn) {
        super(entitiesIn);
    }

    public CassandraConnectionEntityManagerStub() {
        super();
    }

    @Override
    public CassandraConnection getCassandraConnectionEntity(String connectionName) throws FindException {
        return null;
    }
}
