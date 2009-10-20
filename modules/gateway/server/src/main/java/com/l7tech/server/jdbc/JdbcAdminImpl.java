package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.server.ServerConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author ghuang
 */
public class JdbcAdminImpl implements JdbcAdmin {
    private JdbcConnectionManager jdbcConnectionManager;
    private JdbcQueryingManager jdbcQueryingManager;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private ServerConfig serverConfig;

    public JdbcAdminImpl(JdbcConnectionManager jdbcConnectionManager,
                                   JdbcQueryingManager jdbcQueryingManager,
                                   JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   ServerConfig serverConfig) {
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.jdbcQueryingManager = jdbcQueryingManager;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.serverConfig = serverConfig;
    }

    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return jdbcConnectionManager.getJdbcConnection(connectionName);
    }

    @Override
    public List<JdbcConnection> getAllJdbcConnections() throws FindException {
        List<JdbcConnection> connections = new ArrayList<JdbcConnection>();
        connections.addAll(jdbcConnectionManager.findAll());
        return connections;
    }

    @Override
    public List<String> getAllJdbcConnectionNames() throws FindException {
        List<JdbcConnection> connList = getAllJdbcConnections();
        List<String> nameList = new ArrayList<String>(connList.size());
        for (JdbcConnection conn: connList) {
            nameList.add(conn.getName());
        }
        return nameList;
    }

    @Override
    public long saveJdbcConnection(JdbcConnection connection) throws UpdateException {
        jdbcConnectionManager.update(connection);
        return connection.getOid();
    }

    @Override
    public void deleteJdbcConnection(JdbcConnection connection) throws DeleteException {
        jdbcConnectionManager.delete(connection);
    }

    @Override
    public String testJdbcConnection(JdbcConnection connection) {
        return jdbcConnectionPoolManager.testJdbcConnection(connection);
    }

    @Override
    public Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams) {
        return jdbcQueryingManager.performJdbcQuery(connectionName, query, maxRecords, preparedStmtParams);
    }

    @Override
    public String testJdbcQuery(String connectionName, String query) {
        Object result = jdbcQueryingManager.performJdbcQuery(connectionName, query, 1, null);
        return (result instanceof String)? (String)result : null;
    }

    @Override
    public List<String> getPropertyDefaultDriverClassList() {
        List<String> driverClassList = new ArrayList<String>();

        String defaultList = serverConfig.getProperty(ServerConfig.PARAM_JDBC_CONNECTION_DEFAULT_DRIVERCLASS_LIST);
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

    @Override
    public int getPropertyDefaultMaxRecords() {
        try {
            String defaultMax = serverConfig.getProperty(ServerConfig.PARAM_JDBC_QUERY_MAXRECORDS_DEFAULT);
            return Integer.parseInt(defaultMax);
        } catch (Exception e) {
            return ORIGINAL_MAX_RECORDS;
        }
    }

    @Override
    public int getPropertyDefaultMinPoolSize() {
        try {
            String defaultMin = serverConfig.getProperty(ServerConfig.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MINPOOLSIZE);
            return Integer.parseInt(defaultMin);
        } catch (Exception e) {
            return ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE;
        }
    }

    @Override
    public int getPropertyDefaultMaxPoolSize() {
        try {
            String defaultMax = serverConfig.getProperty(ServerConfig.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MAXPOOLSIZE);
            return Integer.parseInt(defaultMax);
        } catch (Exception e) {
            return ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE;
        }
    }
}
