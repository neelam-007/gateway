package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.metadata.CallMetaDataProvider;
import org.springframework.jdbc.core.metadata.CallMetaDataProviderFactory;
import org.springframework.jdbc.core.metadata.CallParameterMetaData;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * An implementation of the interface performing a JDBC query by using a JDBC connection from a connection pool.
 *
 * @author ghuang
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class JdbcQueryingManagerImpl implements JdbcQueryingManager, ApplicationListener {
    private static final Logger logger = Logger.getLogger(JdbcQueryingManagerImpl.class.getName());

    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private final Config config;

    /**
     * A cache of SimpleJdbcCall objects which are both cacheable and thread safe. Internally they use a DataSource which
     * means they do not hold onto java.sql.Connection objects are obtain them as needed from the DataSource.
     *
     * The cache key is generated from the JDBC Connection name (which is unique on a gateway and is linked to a user
     * account) and a procedure name.
     */
    private final static Map<String, Pair<SimpleJdbcCall, List<String>>> simpleJdbcCallCache = new ConcurrentHashMap<>();

    public JdbcQueryingManagerImpl(final JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   final Config config) {
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.config = config;
    }



    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
            if (JdbcConnection.class.equals(entityInvalidationEvent.getEntityClass())) {
                // remove entire cache
                // we cannot tell for sure the old and new name of an entity and if it was deleted we cannot get
                // the old name.
                simpleJdbcCallCache.clear();
            }
        }
    }

    /**
     * Perform a JDBC query that could be a select statement or a non-select statement.
     *
     * @param connectionName:     the name of a JdbcConnection entity to retrieve a dataSource (i.e., a connection pool)
     * @param query:              the SQL query
     * @param schema:         the schema to query for, empty for default
     * @param maxRecords:         the maximum number of records allowed to return.
     * @param preparedStmtParams: the parameters of a prepared statement.
     * @return an object, which may be a string (an error message), an integer (the number of records updated), or
     *         a SqlRowSet representing disconnected java.sql.ResultSet data (the result of a select statement).
     */
    @Override
    public Object performJdbcQuery(@Nullable String connectionName, @NotNull final String query, @Nullable String schema, final int maxRecords, final List<Object> preparedStmtParams) {
        if (connectionName == null || connectionName.isEmpty()) {
            logger.warning("Failed to perform querying since the JDBC connection name is not specified.");
            return "JDBC Connection Name is not specified.";
        }

        // Get a DataSource for creating a JdbcTemplate.
        DataSource dataSource;
        try {
            dataSource = jdbcConnectionPoolManager.getDataSource(connectionName);
        } catch (Exception e) {
            logger.warning("Failed to perform querying since " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)));
            return "Cannot retrieve a C3P0 DataSource.";
        }
        return performJdbcQuery(connectionName, dataSource, query, schema, maxRecords, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, List<Object> preparedStmtParams)
    {
        return performJdbcQuery(null, dataSource, query, schema, maxRecords, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@Nullable String connectionName, @NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, List<Object> preparedStmtParams) {
        // Create a JdbcTemplate and set the max rows.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setMaxRows(maxRecords);

        return performJdbcQuery(connectionName, jdbcTemplate, query, schema,preparedStmtParams);
    }

    @Override
    public void clearMetaDataCache(String connectionName, String query) {
        final String cacheKey = getCacheKey(connectionName, JdbcUtil.getName(query));
        simpleJdbcCallCache.remove(cacheKey);
    }

    protected Object performJdbcQuery(@Nullable String connectionName, JdbcTemplate jdbcTemplate, String query, @Nullable String schemaName, List<Object> preparedStmtParams)
    {
        // Query or update and return querying results.
        try {
            boolean isSelectQuery = query.toLowerCase().startsWith("select");
            boolean isStoredProcedureQuery = JdbcUtil.isStoredProcedure(query);
            if (isSelectQuery) {
                // Return a map of column names and arrays of values.
                return jdbcTemplate.query(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]),new QueryingManagerResultSetExtractor());
            } else if (isStoredProcedureQuery) {
                // Return a List of SqlRowSet representing disconnected java.sql.ResultSet (s) and OUT parameters.
                // Create or reuse an existing SimpleJdbcCall object.

                final boolean allowCaching = connectionName != null && config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA, true);

                final String procName = JdbcUtil.getName(query);

                // fyi two branches to make code more idiomatic
                if (allowCaching) {
                    final String uniqueKey = getCacheKey(connectionName, procName);
                    final JdbcCallHelper jdbcCallUtil;
                    final SimpleJdbcCall simpleJdbcCall;
                    if (simpleJdbcCallCache.containsKey(uniqueKey)) {
                        final Pair<SimpleJdbcCall, List<String>> pair = simpleJdbcCallCache.get(uniqueKey);
                        simpleJdbcCall = pair.left;
                        jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall, pair.right);
                    } else {
                        simpleJdbcCall = buildSimpleJdbcCall(jdbcTemplate, query, procName, schemaName);
                        final List<String> inParameters = getInParameters(schemaName, procName, simpleJdbcCall);
                        jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall, inParameters);

                        simpleJdbcCallCache.put(uniqueKey, new Pair<SimpleJdbcCall, List<String>>(simpleJdbcCall, Collections.unmodifiableList(inParameters)));
                    }

                    try {
                        return jdbcCallUtil.queryForRowSet(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
                    } catch (DataAccessException e) {
                        // remove cached simple jdbc call if call failed
                        simpleJdbcCallCache.remove(uniqueKey);
                        throw e;
                    }

                } else {
                    final SimpleJdbcCall simpleJdbcCall = buildSimpleJdbcCall(jdbcTemplate, query, procName, schemaName);
                    final List<String> inParameters = getInParameters(schemaName, procName, simpleJdbcCall);
                    final JdbcCallHelper jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall, inParameters);
                    return jdbcCallUtil.queryForRowSet(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
                }

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

    static List<String> getInParameters(@Nullable String schemaName, @NotNull String procName, @NotNull SimpleJdbcCall simpleJdbcCall) {
        List<String> inParameters;
        final boolean hasSchemaName = schemaName != null && !schemaName.trim().isEmpty();
        inParameters = getInParametersName(procName, hasSchemaName ? schemaName : null, simpleJdbcCall);
        return inParameters;
    }

    private SimpleJdbcCall buildSimpleJdbcCall(@NotNull JdbcTemplate jdbcTemplate, @NotNull String query, @NotNull String procName, @Nullable String schemaName) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);
        simpleJdbcCall.setProcedureName(procName);
        simpleJdbcCall.setFunction(query.toLowerCase().startsWith(JdbcCallHelper.SQL_FUNCTION));

        final boolean hasSchemaName = schemaName != null && !schemaName.trim().isEmpty();
        if (hasSchemaName) {
            simpleJdbcCall.setSchemaName(schemaName);
        }
        return simpleJdbcCall;
    }

    /**
     * Get the input parameter names of the procedure
     *
     *
     * @param procName
     * @param simpleJdbcCall
     * @return
     */
    private static List<String> getInParametersName(@NotNull final String procName, @Nullable final String schemaName, @NotNull SimpleJdbcCall simpleJdbcCall) {
        final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();
        callMetaDataContext.setProcedureName(procName);
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            callMetaDataContext.setSchemaName(schemaName);
        }
        final CallMetaDataProvider metaProvider = CallMetaDataProviderFactory.createMetaDataProvider(simpleJdbcCall.getJdbcTemplate().getDataSource(), callMetaDataContext);
        List<CallParameterMetaData> parameterMetaData = metaProvider.getCallParameterMetaData();
        List<String> parameterNames = new ArrayList<String>();
        for (final CallParameterMetaData meta : parameterMetaData) {                        // 1=procedureColumnIn, 2=procedureColumnInOut, 4=procedureColumnOut
            if (meta.getParameterType() == DatabaseMetaData.procedureColumnIn               // 1-IN parameter, 2-INOUT param in Mysql & Oracle , 4-OUT param in Mysql & Oracle,
                    || meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {  // but 2 is OUT param in MS SQL as well since it behaves as IN & OUT
                parameterNames.add(meta.getParameterName().replaceAll("@", ""));
            }
        }
        return parameterNames;
    }

    /**
     * WARNING - the key generated is based on the JDBC Connection name and the procedure name.
     * This is unique enough only if procedure / function overloading is not supported.
     * Currently this is not supported by Spring.
     *
     * @param connectionName unique JDBC Connection name
     * @param procName name of procedure or function being called.
     * @return unique cache key
     */
    private String getCacheKey(String connectionName, String procName) {
        return connectionName + "_" + procName;
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


    /**
     *  extracts into a  map of column names and values as an ordered list
        column "name" of row 5 ==> map key= "name", list index = 5
        column names are all lower case
     */
    private static class QueryingManagerResultSetExtractor implements ResultSetExtractor< Map<String,List<Object>>> {
        @Override
        public  Map<String,List<Object>> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
            Map<String,List<Object>> result = new HashMap<String,List<Object>>();
            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();
            while(resultSet.next()){
                for(int j = 1 ; j <= columnCount ; j++){
                    String colName = meta.getColumnLabel(j).toLowerCase();
                    List<Object> col  = result.get(colName);

                    if(col == null){
                        col = new ArrayList<Object>();
                        result.put(colName,col);
                    }

                    Object o ;
                    int type = resultSet.getMetaData().getColumnType(j);

                    if(type == Types.CLOB){
                        Clob clob = resultSet.getClob(j);
                        o = clob == null? null:clob.getSubString(1,(int)clob.length());
                    }
                    else if (type == Types.BLOB){
                        Blob blob = resultSet.getBlob(j);
                        o = blob == null? null : blob.getBytes(1,(int)blob.length());
                    }
                    else
                        o = resultSet.getObject(j);

                    col.add(o);
                }
            }
            return result;
        }
    }
}
