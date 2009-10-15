package com.l7tech.server.jdbcconnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
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

        // Get a raw connection for querying
        Connection conn;
        try {
            conn = jdbcConnectionPoolManager.getRawConnection(connectionName);
        } catch (Exception e) {
            return "Cannot get a connection from a C3P0 Connection pool.";
        }

        // Create a prepared statement
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(query);
        } catch (SQLException e) {
            return "error creating a SQL prepared statement.";
        }

        // Set parameters
        try {
            for (int i = 0; i < preparedStmtParams.size(); i++) {
                pstmt.setObject(i+1, preparedStmtParams.get(i));
            }
        } catch (SQLException e) {
            return "error setting a parameter in a SQL prepared statement.";
        }

        // Set max number of returned records
        try {
            pstmt.setMaxRows(maxRecords);
        } catch (SQLException e) {
            return "error setting a maximum number of records, " + maxRecords + " in a SQL prepared statement";
        }

        // Query and return the result
        try {
            if (query.toLowerCase().startsWith("select")) {
                // Return a ResultSet
                return pstmt.executeQuery();
            } else {
                // Return an integer
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            return "an invalid SQL prepared statement";
        }
    }
}
