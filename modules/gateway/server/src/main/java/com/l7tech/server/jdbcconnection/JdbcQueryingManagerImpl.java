package com.l7tech.server.jdbcconnection;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

/**
 * @author ghuang
 */
public class JdbcQueryingManagerImpl implements JdbcQueryingManager {

    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    public JdbcQueryingManagerImpl(JdbcConnectionPoolManager jdbcConnectionPoolManager) {
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
    }

    @Override
    public Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams) {
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
}
