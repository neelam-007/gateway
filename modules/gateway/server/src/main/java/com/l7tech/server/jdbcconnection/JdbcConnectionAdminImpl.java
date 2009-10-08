package com.l7tech.server.jdbcconnection;

import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.server.ServerConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author: ghuang
 */
public class JdbcConnectionAdminImpl implements JdbcConnectionAdmin {
    private JdbcConnectionManager jdbcConnectionManager;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private ServerConfig serverConfig;

    public JdbcConnectionAdminImpl(JdbcConnectionManager jdbcConnectionManager,
                                   JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   ServerConfig serverConfig) {
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.serverConfig = serverConfig;
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
    public boolean testConnection(JdbcConnection connection) {
        return jdbcConnectionPoolManager.createDataSource(connection);
    }

    @Override
    public void createDataSource(JdbcConnection connection) {
        jdbcConnectionPoolManager.createDataSource(connection);
    }

    @Override
    public Object performJdbcQuery(String connectionName, String query, int maxRecords) {
        if (connectionName == null || connectionName.isEmpty()) return "JDBC Connection Name is not specified.";
        else if (query == null || query.isEmpty()) return "SQL Query is not specified.";

        Connection conn;
        try {
            conn = jdbcConnectionPoolManager.getJdbcConnection(connectionName);
        } catch (Exception e) {
            return "Cannot get a connection from the C3P0 Connection pool.";
        }

        Statement stmt;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            return "Cannnot create a SQL statement.";
        }

        try {
            stmt.setMaxRows(maxRecords);
        } catch (SQLException e) {
            return "The SQL statement cannot set a maximum number of records, " + maxRecords + ".";
        }

        try {
            if (query.toLowerCase().startsWith("select")) {
                ResultSet rs = stmt.executeQuery(query);
                return rs;
            } else {
                int num = stmt.executeUpdate(query);
                return num;
            }
        } catch (SQLException e) {
            return "Invalid SQL statement.";
        }
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
