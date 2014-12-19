package com.l7tech.server.cassandra;

import com.ca.datasources.cassandra.CassandraQueryManager;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private LicenseManager licenseManager;

    private final CassandraConnectionEntityManager cassandraEntityManager;
    private final SecurePasswordManager securePasswordManager;
    private final CassandraConnectionManager cassandraConnectionManager;
    private final CassandraQueryManager cassandraQueryManager;

    public CassandraConnectionManagerAdminImpl(CassandraConnectionEntityManager cassandraEntityManager,
                                               SecurePasswordManager securePasswordManager,
                                               CassandraConnectionManager cassandraConnectionManager,
                                               CassandraQueryManager cassandraQueryManager) {
        this.cassandraEntityManager = cassandraEntityManager;
        this.securePasswordManager = securePasswordManager;
        this.cassandraConnectionManager = cassandraConnectionManager;
        this.cassandraQueryManager = cassandraQueryManager;
    }

    @Override
    public CassandraConnection getCassandraConnection(String connectionName) throws FindException {
        checkLicense();
        return cassandraEntityManager.findByUniqueName(connectionName);
    }

    @Override
    public List<CassandraConnection> getAllCassandraConnections() throws FindException {
        checkLicense();
        List<CassandraConnection> connections = new ArrayList<>();
        connections.addAll(cassandraEntityManager.findAll());
        return connections;
    }

    @Override
    public List<String> getAllCassandraConnectionNames() throws FindException {
        checkLicense();
        List<String> names = new ArrayList<>();
        for (CassandraConnection entity : getAllCassandraConnections()) {
            names.add(entity.getName());
        }
        return names;
    }

    @Override
    public Goid saveCassandraConnection(CassandraConnection connection) throws UpdateException, SaveException {
        checkLicense();
        Goid goid = null;
        if (connection.getGoid().equals(Goid.DEFAULT_GOID)) {
            goid = cassandraEntityManager.save(connection);
        } else {
            // Update cached connection if exists
            cassandraConnectionManager.updateConnection(connection);

            cassandraEntityManager.update(connection);
            goid = connection.getGoid();
        }
        return goid;
    }

    @Override
    public void deleteCassandraConnection(CassandraConnection connection) throws DeleteException {
        checkLicense();
        // Remove old cached connection
        cassandraConnectionManager.removeConnection(connection);

        cassandraEntityManager.delete(connection);
    }

    @Override
    public JobId<String> testCassandraConnection(final CassandraConnection connection) {
        checkLicense();
        final FutureTask<String> connectTask = new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {

                try {
                    cassandraConnectionManager.testConnection(connection);
                } catch (FindException | ParseException e) {
                    return "Invalid username or password setting. \n" + ExceptionUtils.getMessage(e);
                } catch (NoHostAvailableException e) {
                    return "Cannot connect to any Cassandra Server.";
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
    public JobId<String> testCassandraQuery(final String connectionName, final String query, final long queryTimeout) {
        checkLicense();
        final FutureTask<String> queryTask = new FutureTask<>(find(true).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    CassandraConnectionHolder testConnection = cassandraConnectionManager.getConnection(connectionName);
                    if(testConnection == null || testConnection.getSession() == null) {
                        return "Unable to establish connection to Cassandra server";
                    }
                    cassandraQueryManager.testQuery(testConnection.getSession(), query, queryTimeout);
                } catch (Exception e) {
                    return "Test query failed: " + ExceptionUtils.getMessage(e);
                }
                return "";
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob(queryTask, String.class);
    }

    private void checkLicense() {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_CASSANDRA);
        } catch ( LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new LicenseRuntimeException(e);
        }
    }
}
