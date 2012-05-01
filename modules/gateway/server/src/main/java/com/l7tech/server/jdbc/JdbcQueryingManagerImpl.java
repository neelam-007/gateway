package com.l7tech.server.jdbc;

import com.l7tech.util.ExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.util.List;
import java.util.logging.Logger;

/**
 * An implementation of the interface performing a JDBC query by using a JDBC connection from a connection pool.
 *
 * @author ghuang
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class JdbcQueryingManagerImpl implements JdbcQueryingManager {
    private static final Logger logger = Logger.getLogger(JdbcQueryingManagerImpl.class.getName());

    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    private final static String CALL = "call"; //standard stored procedure call for MySQL & Oracle
    private final static String EXEC = "exec"; //exec/execute - stored procedure call for MS SQL & T-SQL
    private final static String FUNC = "func"; //keyword to indicate that we are calling a function instead of a stored procedure

    public JdbcQueryingManagerImpl(JdbcConnectionPoolManager jdbcConnectionPoolManager) {
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
    }

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
    @Override
    public Object performJdbcQuery(String connectionName, final String query, final int maxRecords, final List<Object> preparedStmtParams) {
        if (connectionName == null || connectionName.isEmpty()) {
            logger.warning("Failed to perform querying since the JDBC connection name is not specified.");
            return "JDBC Connection Name is not specified.";
        } else if (query == null || query.isEmpty()) {
            logger.warning("Failed to perform querying since the SQL query is not specified.");
            return "SQL Query is not specified.";
        }

        // Get a DataSource for creating a JdbcTemplate.
        DataSource dataSource;
        try {
            dataSource = jdbcConnectionPoolManager.getDataSource(connectionName);
        } catch (Exception e) {
            logger.warning("Failed to perform querying since " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)));
            return "Cannot retrieve a C3P0 DataSource.";
        }

        // Create a JdbcTemplate and set the max rows.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setMaxRows(maxRecords);

        // Query or update and return querying results.
        try {
            boolean isSelectQuery = query.toLowerCase().startsWith("select");
            boolean isStoredProcedureQuery = query.toLowerCase().startsWith(CALL)
                    || query.toLowerCase().startsWith(EXEC)
                    || query.toLowerCase().startsWith(FUNC);
            if (isSelectQuery) {
                // Return a SqlRowSet representing disconnected java.sql.ResultSet data.
                return jdbcTemplate.queryForRowSet(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
            } else if (isStoredProcedureQuery) {
                // Return a List of SqlRowSet representing disconnected java.sql.ResultSet (s) and OUT parameters.
                final SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
                final JdbcCallHelper jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall);
                return jdbcCallUtil.queryForRowSet(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
            } else {
                // Return an integer representing the number of rows updated.
                return jdbcTemplate.update(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
            }
        } catch (DataAccessException e) {
            logger.warning("Failed to perform querying since " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)));

            if (ExceptionUtils.causedBy(e, CannotGetJdbcConnectionException.class)) {
                return "Could not get JDBC Connection.";
            } else if (ExceptionUtils.causedBy(e, BadSqlGrammarException.class)) {
                return "Bad SQL Grammar.";
            } else {
                return ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            }
        }
    }

    /**
     * Get a result set (SqlRowSet object) from the mock JDBC database.
     * Note: this method is only for testing purpose, so it returns null in this class.
     *
     * @return a SqlRowSet object containing mock data.
     */
    @Override
    public SqlRowSet getMockSqlRowSet() {
        return null;
    }
}
