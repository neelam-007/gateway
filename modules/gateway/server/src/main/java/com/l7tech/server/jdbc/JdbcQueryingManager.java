package com.l7tech.server.jdbc;

import org.jetbrains.annotations.Nullable;
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
     * @param query:              the SQL query
     * @param schema:       the specific schema to override, for oracle only
     * @param maxRecords:         the maximum number of records allowed to return.
     * @param preparedStmtParams: the parameters of a prepared statement.
     * @return an object, which may be:
     *              String: (an error message)
     *              Integer: an integer (the number of records updated), or
     *              Map: a map of column names and values as an ordered list
     *                   column "name" of row 5 ==> map key= "name", list index = 5
     *                   column names are all lower case
     *              List: for stored procedure calls a list of sql result sets
     */

    Object performJdbcQuery(String connectionName, String query, @Nullable String schema, int maxRecords, List<Object> preparedStmtParams);

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * @param dataSource:         the data source to query
     * @param query:              the SQL query
     * @param schema:       the specific schema to override, for oracle only
     * @param maxRecords:         the maximum number of records allowed to return.
     * @param preparedStmtParams: the parameters of a prepared statement.
     * @return an object, which may be:
     *              String: (an error message)
     *              Integer: an integer (the number of records updated), or
     *              Map: a map of column names and values as an ordered list
     *                   column "name" of row 5 ==> map key= "name", list index = 5
     *                   column names are all lower case
     *              List: for stored procedure calls a list of sql result sets
     */
    Object performJdbcQuery(DataSource dataSource, String query, String schema, int maxRecords, List<Object> preparedStmtParams);

    /**
     * Get a result set (SqlRowSet object) from the mock JDBC database.
     *
     * @return a SqlRowSet object containing mock data.
     */
    SqlRowSet getMockSqlRowSet();
}
