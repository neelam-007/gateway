package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.server.event.AdminInfo.find;

/**
 * The implementation of the interface JdbcAdmin to manage JDBC Connection Entities, JDBC Connection Pooling, and JDBC Querying.
 *
 */
public class JdbcAdminImpl extends AsyncAdminMethodsImpl implements JdbcAdmin {
    private JdbcConnectionManager jdbcConnectionManager;
    private JdbcQueryingManager jdbcQueryingManager;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private Config config;

    public JdbcAdminImpl(JdbcConnectionManager jdbcConnectionManager,
                                   JdbcQueryingManager jdbcQueryingManager,
                                   JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   Config config ) {
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.jdbcQueryingManager = jdbcQueryingManager;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.config = config;
    }

    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return jdbcConnectionManager.getJdbcConnection(connectionName);
    }

    /**
     * Retrieve all JDBC Connection entities from the database.
     *
     * @return a list of JDBC Connection entities
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Override
    public List<JdbcConnection> getAllJdbcConnections() throws FindException {
        List<JdbcConnection> connections = new ArrayList<JdbcConnection>();
        connections.addAll(jdbcConnectionManager.findAll());
        return connections;
    }

    /**
     * Get the names of all JDBC Connection entities.
     *
     * @return a list of the names of all JDBC Connection entities.
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Override
    public List<String> getAllJdbcConnectionNames() throws FindException {
        List<JdbcConnection> connList = getAllJdbcConnections();
        List<String> nameList = new ArrayList<String>(connList.size());
        for (JdbcConnection conn: connList) {
            nameList.add(conn.getName());
        }
        return nameList;
    }

    /**
     * Save a JDBC Connection entity into the database.
     *
     * @param connection: the JDBC Connection entity to be saved.
     * @return a long, the saved entity object id.
     * @throws UpdateException: thrown when errors saving the JDBC Connection entity.
     */
    @Override
    public long saveJdbcConnection(JdbcConnection connection) throws UpdateException {
        jdbcConnectionManager.update(connection);
        return connection.getOid();
    }

    /**
     * Delete a JDBC Connection entity from the database.
     *
     * @param connection: the JDBC Connection entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the JDBC Connection entity.
     */
    @Override
    public void deleteJdbcConnection(JdbcConnection connection) throws DeleteException {
        jdbcConnectionManager.delete(connection);
    }

    /**
     * Test a JDBC Connection entity.
     *
     * @param connection: the JDBC Connection to be tested.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Override
    public AsyncAdminMethods.JobId<String> testJdbcConnection(JdbcConnection connection) {
        ComboPooledDataSource cpds = null ;

        String error = null;
        try {
            cpds = jdbcConnectionPoolManager.getTestConnectionPool(connection);
        }catch (Throwable e){
            error = e.getMessage();
        }


        final String finalError = error;
        final ComboPooledDataSource finalCpds = cpds;
        final FutureTask<String> connectTask = new FutureTask<String>( find( false ).wrapCallable( new Callable<String>(){
            @Override
            public String call() throws Exception {

                if(finalError != null) return finalError;

                Connection conn = null;
                try {
                    conn = finalCpds.getConnection();
                } catch (SQLException e) {
                    return "invalid connection properties setting. \n"  + e.getMessage();
                } catch (Throwable e) {
                    return "unexpected error, " + e.getClass().getSimpleName() + " thrown";
                } finally {
                    if (conn != null) try {
                        conn.close();
                    } catch (SQLException e) {
                        logger.warning("Cannot close a JDBC connection.");
                    }
                    finalCpds.close();
                }
                return "";
            }
        } ) );

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                connectTask.run();
            }
        }, 0L);

        return registerJob( connectTask, String.class );
    }

    /**
     * Test a JDBC query and see if it is a valid SQL statement.
     *
     * @param connectionName: the name of a JDBC Connection entity.
     * @param query: a SQL query statement.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Override
    public AsyncAdminMethods.JobId<String> testJdbcQuery(final String connectionName, final String query) {
        final FutureTask<String> queryTask = new FutureTask<String>( find( false ).wrapCallable( new Callable<String>(){
            @Override
            public String call() throws Exception {

                Object result = jdbcQueryingManager.performJdbcQuery(connectionName, query, 1, null);
                return (result instanceof String)? (String)result : null;
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob( queryTask, String.class );
    }

    /**
     * Get a property, default driver class list from the global cluster properties.  if failed to get its value,
     * then use the original driver class list defined in this interface.
     *
     * @return a list of driver classes.
     */
    @Override
    public List<String> getPropertyDefaultDriverClassList() {
        List<String> driverClassList = new ArrayList<String>();

        String defaultList = config.getProperty( ServerConfigParams.PARAM_JDBC_CONNECTION_DEFAULT_DRIVERCLASS_LIST );
        if (defaultList != null && !defaultList.isEmpty()) {
            StringTokenizer tokens = new StringTokenizer(defaultList, "\n");
            while (tokens.hasMoreTokens()) {
                String driverClass = tokens.nextToken();
                if (driverClass != null && !driverClass.isEmpty()) driverClassList.add(driverClass);
            }
        }

        if (driverClassList.isEmpty()) driverClassList.add(ORIGINAL_DRIVERCLASS_LIST);
        return driverClassList;
    }

    /**
     * Get a property, default maximum number of records returned by a query from the global cluser properties.  If failed
     * to get its value, then use the original maximum number of records returned by a query defined in this interface.
     *
     * @return an integer, the default maximum number of records returned by a query
     */
    @Override
    public int getPropertyDefaultMaxRecords() {
        return config.getIntProperty( ServerConfigParams.PARAM_JDBC_QUERY_MAXRECORDS_DEFAULT, ORIGINAL_MAX_RECORDS );
    }

    /**
     * Get a property, default minimum pool size.  If failed to get its value, then use the original minimum pool size
     * defined in this interface.
     *
     * @return an integer, the default minimum pool size.
     */
    @Override
    public int getPropertyDefaultMinPoolSize() {
        return config.getIntProperty( ServerConfigParams.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MINPOOLSIZE, ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE );
    }

    /**
     * Get a property, default maximum pool size.  If failed to get its value, then use the original maximum pool size
     * defined in this interface.
     *
     * @return an integer, the default maximum pool size.
     */
    @Override
    public int getPropertyDefaultMaxPoolSize() {
        return config.getIntProperty( ServerConfigParams.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MAXPOOLSIZE, ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE );
    }
}
