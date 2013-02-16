package com.l7tech.server.jdbc;

import org.jetbrains.annotations.NotNull;
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
     * Request that the meta data for this query is cached.
     *
     * If the query is not a function or procedure call then nothing will be cached. Whether or not caching is performed
     * is determined by system configuration. If there is a chance that caching may be used.
     *
     * If connectionName references a variable then nothing is registered.
     *
     * @param connectionName Unique JDBC Connection name
     * @param query Query to execute as typed into the JDBC Query Assertion
     * @param schemaName optional name of the schema the
     */
    void registerQueryForPossibleCaching(@NotNull String connectionName, @NotNull String query, @Nullable String schemaName);

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * @param connectionName:     the name of a JdbcConnection entity to retrieve a dataSource (i.e., a connection pool). //todo This should not be nullable.
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

    Object performJdbcQuery(@Nullable String connectionName, @NotNull String query, @Nullable String schema, int maxRecords, @NotNull List<Object> preparedStmtParams);

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * This version of performJdbcQuery should not be used by message traffic code which may be calling procedures / functions
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
    Object performJdbcQuery(@NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, @NotNull List<Object> preparedStmtParams);

    /**
    * See {@link #performJdbcQuery(javax.sql.DataSource, String, String, int, java.util.List)}, this adds a connection name
    * so we can track the JDBCConnection entity the DataSource is associated with.
    *
    * @param connectionName the unique JDBCConnection entity name
    */
    Object performJdbcQuery(@Nullable String connectionName, @NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, @NotNull List<Object> preparedStmtParams);

    /**
     * Get a result set (SqlRowSet object) from the mock JDBC database.
     *
     * @return a SqlRowSet object containing mock data.
     */
    SqlRowSet getMockSqlRowSet();
}
