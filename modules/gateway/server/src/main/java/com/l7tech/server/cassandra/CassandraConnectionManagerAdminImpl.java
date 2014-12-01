package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.server.event.AdminInfo.find;

/**
 * Created by yuri on 10/31/14.
 */
public class CassandraConnectionManagerAdminImpl extends AsyncAdminMethodsImpl implements CassandraConnectionManagerAdmin {

    private final CassandraConnectionEntityManager cassandraEntityManager;
    private SecurePasswordManager securePasswordManager;
    private CassandraConnectionManager cassandraConnectionManager;

    public CassandraConnectionManagerAdminImpl(CassandraConnectionEntityManager cassandraEntityManager,
                                               SecurePasswordManager securePasswordManager,
                                               CassandraConnectionManager cassandraConnectionManager) {
        this.cassandraEntityManager = cassandraEntityManager;
        this.securePasswordManager = securePasswordManager;
        this.cassandraConnectionManager = cassandraConnectionManager;
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
        for (CassandraConnection entity : getAllCassandraConnections()) {
            names.add(entity.getName());
        }
        return names;
    }

    @Override
    public Goid saveCassandraConnection(CassandraConnection connection) throws UpdateException, SaveException {
        Goid goid = null;
        if (connection.getGoid().equals(Goid.DEFAULT_GOID)) {
            goid = cassandraEntityManager.save(connection);
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
    public JobId<String> testCassandraConnection(final CassandraConnection connection) {

        final FutureTask<String> connectTask = new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {

                try {
                    cassandraConnectionManager.testConnection(connection);

                } catch (FindException | ParseException e) {
                    return "Invalid username or password setting. \n" + ExceptionUtils.getMessage(e);
                } catch (Throwable e) {
                    return "Unexpected error, " + e.getClass().getSimpleName() + " thrown";
                }
                return "";
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                connectTask.run();
            }
        }, 0L);

        return registerJob(connectTask, String.class);
    }

    @Override
    public JobId<String> testCassandraQuery(String connectionName, String query, @Nullable String schemaName, int queryTimeout) {
        return null;
    }
}
