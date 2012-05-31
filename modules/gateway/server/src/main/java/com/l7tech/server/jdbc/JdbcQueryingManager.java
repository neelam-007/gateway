package com.l7tech.server.jdbc;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.util.List;

/**
 * An interface to perform a JDBC query by using a JDBC connection from a connection pool.
 *
 * @author ghuang
 */
public interface JdbcQueryingManager {

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * @param connectionName:     the name of a JdbcConnection entity to retrieve a dataSource (i.e., a connection pool)
     * @param dataSource:         dataSource if joining an existing transaction
     * @param query:              the SQL query
     * @param maxRecords:         the maximum number of records allowed to return.
     * @param preparedStmtParams: the parameters of a prepared statement.
     * @return an object, which may be a string (an error message), an integer (the number of records updated), or
     *         a SqlRowSet representing disconnected java.sql.ResultSet data (the result of a select statement).
     */
    Object performJdbcQuery(String connectionName, DataSource dataSource, String query, int maxRecords, List<Object> preparedStmtParams);

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * @param connectionName:     the name of a JdbcConnection entity to retrieve a dataSource (i.e., a connection pool)
     * @param query:              the SQL query
     * @param maxRecords:         the maximum number of records allowed to return.
     * @param preparedStmtParams: the parameters of a prepared statement.
     * @return an object, which may be a string (an error message), an integer (the number of records updated), or
     *         a SqlRowSet representing disconnected java.sql.ResultSet data (the result of a select statement).
     */
    Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams);

    /**
     * Get a result set (SqlRowSet object) from the mock JDBC database.
     *
     * @return a SqlRowSet object containing mock data.
     */
    SqlRowSet getMockSqlRowSet();
}
