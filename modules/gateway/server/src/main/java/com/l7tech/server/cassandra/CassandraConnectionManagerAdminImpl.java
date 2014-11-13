package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuri on 10/31/14.
 */
public class CassandraConnectionManagerAdminImpl extends AsyncAdminMethodsImpl implements CassandraConnectionManagerAdmin {

    private final CassandraConnectionEntityManager cassandraEntityManager;

    public CassandraConnectionManagerAdminImpl(CassandraConnectionEntityManager cassandraEntityManager) {
        this.cassandraEntityManager = cassandraEntityManager;
    }
    @Override
    public CassandraConnection getCassandraConnection(String connectionName) throws FindException {
        return cassandraEntityManager.findByUniqueName(connectionName);
    }

    @Override
    public List<CassandraConnection> getAllCassandraConnections() throws FindException {
        List<CassandraConnection> connections = new ArrayList<>();
        connections.addAll(cassandraEntityManager.findAll());
        return connections;
    }

    @Override
    public List<String> getAllCassandraConnectionNames() throws FindException {
        List<String> names = new ArrayList<>();
        for(CassandraConnection entity : getAllCassandraConnections()){
            names.add(entity.getName());
        }
        return names;
    }

    @Override
    public Goid saveCassandraConnection(CassandraConnection connection) throws UpdateException, SaveException {
        Goid goid = null;
        if (connection.getGoid().equals(Goid.DEFAULT_GOID)) {
            goid =  cassandraEntityManager.save(connection);
        } else {
            cassandraEntityManager.update(connection);
            goid = connection.getGoid();
        }
        return goid;
    }

    @Override
    public void deleteCassandraConnection(CassandraConnection connection) throws DeleteException {
        cassandraEntityManager.delete(connection);
    }

    @Override
    public JobId<String> testCassandraConnection(CassandraConnection connection) {
        return null;
    }

    @Override
    public JobId<String> testCassandraQuery(String connectionName, String query, @Nullable String schemaName, int queryTimeout) {
        return null;
    }
}
