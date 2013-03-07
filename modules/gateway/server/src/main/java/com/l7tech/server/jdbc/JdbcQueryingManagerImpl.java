package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.*;
import org.jboss.cache.util.concurrent.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.metadata.*;
import org.springframework.jdbc.core.simple.AbstractJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the interface performing a JDBC query by using a JDBC connection from a connection pool.
 *
 * @author ghuang
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class JdbcQueryingManagerImpl implements JdbcQueryingManager, PropertyChangeListener, ApplicationListener {
    private static final Logger logger = Logger.getLogger(JdbcQueryingManagerImpl.class.getName());

    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private final JdbcConnectionManager jdbcConnectionManager;
    private final Config config;
    private final TimeSource timeSource;
    private final ManagedTimer downloadMetaDataTimer = new ManagedTimer("JDBC Query Manager meta data cache timer");
    private final ManagedTimer cleanUpTimer = new ManagedTimer("JDBC Query Manager meta data clean up timer");

    private final AtomicReference<MetaDataCacheTask> currentCacheTask = new AtomicReference<MetaDataCacheTask>();
    private final AtomicReference<MetaDataCleanUpExceptionsTask> currentCleanUpTask = new AtomicReference<MetaDataCleanUpExceptionsTask>();

    private final Map<CachedMetaDataKey, CachedMetaDataValue> simpleJdbcCallCache = new ConcurrentHashMap<CachedMetaDataKey, CachedMetaDataValue>();

    /**
     * Set of unique procedures / functions to manage meta data for.
     */
    private final static Set<CachedMetaDataKey> dbObjectsToCacheMetaDataFor = new ConcurrentHashSet<CachedMetaDataKey>();

    public JdbcQueryingManagerImpl(final JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   final JdbcConnectionManager jdbcConnectionManager,
                                   final Config config,
                                   final TimeSource timeSource) {
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.config = config;
        this.timeSource = timeSource;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {

        if (applicationEvent instanceof ReadyForMessages) {
            doStart();
        } else if (applicationEvent instanceof Stopped) {
            doStop();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final boolean cachingAllowed = isCachingAllowed();
        synchronized (currentCacheTask) {
            final String propertyName = evt.getPropertyName();
            final boolean enableCache = config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_TASK_ENABLED, true) && cachingAllowed;
            if (propertyName.equals(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_TASK_ENABLED) || propertyName.equals(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED)) {
                if (enableCache) {
                    createAndStartMetaDataTask();
                } else {
                    stopCurrentTaskIfRunning();
                }
            } else if (propertyName.equals(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_REFRESH_INTERVAL)) {
                if (enableCache) {
                    // only reconfigure the task if the cache is actually enabled.
                    createAndStartMetaDataTask();
                }
            }else if (propertyName.equals(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_CLEANUP_REFRESH_INTERVAL)) {
                createAndStartCleanUpTask();
            }

            if (!cachingAllowed) {
                // clear the cache
                simpleJdbcCallCache.clear();
            }
        }
    }

    @Override
    public void registerQueryForPossibleCaching(@NotNull String connectionName, @NotNull String query, @Nullable String schemaName) {
        if (JdbcUtil.isStoredProcedure(query) && !Syntax.isAnyVariableReferenced(connectionName)) {
            dbObjectsToCacheMetaDataFor.add(getCacheKey(connectionName, query, Option.optional(schemaName)));
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
    public Object performJdbcQuery(@Nullable final String connectionName, @NotNull final String query, @Nullable final String schema, final int maxRecords, final int timeoutSeconds, @NotNull final List<Object> preparedStmtParams) {
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
        return performJdbcQuery(connectionName, dataSource, query, schema, maxRecords, timeoutSeconds, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@Nullable String connectionName, @NotNull final String query, @Nullable String schema, final int maxRecords, @NotNull final List<Object> preparedStmtParams) {
        return performJdbcQuery(connectionName, query, schema, maxRecords, 0, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, int timeoutSeconds, @NotNull List<Object> preparedStmtParams) {
        return performJdbcQuery(null, dataSource, query, schema, maxRecords, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, final int maxRecords, @NotNull List<Object> preparedStmtParams) {
        return performJdbcQuery(dataSource, query, schema, maxRecords, 0, preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@Nullable final String connectionName, @NotNull final DataSource dataSource, @NotNull final String query, @Nullable final String schema, final int maxRecords, final int timeoutSeconds, @NotNull final List<Object> preparedStmtParams) {
        // Ideally we would not construct the JdbcTemplate until we know it is needed. This instance may be ignored if a cached JdbcCallHelper is found
        return performJdbcQuery(connectionName, buildJdbcTemplate(dataSource, maxRecords, timeoutSeconds), query, Option.optional(schema), preparedStmtParams);
    }

    @Override
    public Object performJdbcQuery(@Nullable String connectionName, @NotNull DataSource dataSource, @NotNull String query, @Nullable String schema, int maxRecords, @NotNull List<Object> preparedStmtParams) {
        return performJdbcQuery(connectionName, dataSource, query, schema, maxRecords, 0, preparedStmtParams);
    }

    /**
     * Method signature accepting a JdbcTemplate is to support testing only.
     */
    protected Object performJdbcQuery(@Nullable String connectionName, JdbcTemplate jdbcTemplate, String query, @NotNull Option<String> schemaName, @NotNull List<Object> preparedStmtParams)
            throws DataAccessException {
        // Query or update and return querying results.
        try {
            boolean isSelectQuery = query.toLowerCase().startsWith("select");
            boolean isStoredProcedureQuery = JdbcUtil.isStoredProcedure(query);
            if (isSelectQuery) {
                // Return a map of column names and arrays of values.
                return jdbcTemplate.query(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]), new QueryingManagerResultSetExtractor());
            } else if (isStoredProcedureQuery) {
                // Return a List of SqlRowSet representing disconnected java.sql.ResultSet (s) and OUT parameters.
                // Create or reuse an existing SimpleJdbcCall object.

                final boolean allowCaching = connectionName != null && isCachingAllowed();

                final JdbcCallHelper jdbcCallHelper;
                if (allowCaching) {
                    jdbcCallHelper = getAndCacheIfNeededCallHelper(connectionName, jdbcTemplate, query, schemaName);
                } else {
                    final String procName = JdbcUtil.getName(query);
                    final SimpleJdbcCall simpleJdbcCall = buildSimpleJdbcCall(jdbcTemplate, query, schemaName);
                    final List<String> inParameters = getInParameters(schemaName, procName, simpleJdbcCall);
                    jdbcCallHelper = new JdbcCallHelper(simpleJdbcCall, inParameters);
                }

                return jdbcCallHelper.queryForRowSet(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
            } else {
                // Return an integer representing the number of rows updated.
                return jdbcTemplate.update(query, preparedStmtParams.toArray(new Object[preparedStmtParams.size()]));
            }
        } catch (DataAccessException e) {
            logger.warning("Failed to perform querying since " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)));

            if (ExceptionUtils.causedBy(e, CannotGetJdbcConnectionException.class)) {
                return "Could not get JDBC Connection.";
            } else if (ExceptionUtils.causedBy(e, BadSqlGrammarException.class)) {
                return "Bad SQL Grammar: " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            } else {
                return ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            }
        }
    }

    /**
     * Try and get a cached JdbcCallHelper instance. If one is not available it will be created and added to the cache.
     * @param jdbcTemplate Must contain a value for queryTimeout. The value it contains will be used to update the cached JdbcTemplate if found.
     */
    private JdbcCallHelper getAndCacheIfNeededCallHelper(String connectionName, JdbcTemplate jdbcTemplate, String query, Option<String> schemaName)
            throws DataAccessException {

        final JdbcCallHelper jdbcCallUtil;
        final CachedMetaDataKey uniqueKey = getCacheKey(connectionName, query, schemaName);
        final SimpleJdbcCall simpleJdbcCall;
        if (simpleJdbcCallCache.containsKey(uniqueKey)) {
            final CachedMetaDataValue cachedMetaDataValue = simpleJdbcCallCache.get(uniqueKey);
            final Either<DataAccessException, SimpleJdbcCall> either = cachedMetaDataValue.cachedData;
            if (either.isLeft()) {
                throw either.left();
            }
            simpleJdbcCall = either.right();
            // update the cached timeout in case the caller has changed it's value
            simpleJdbcCall.getJdbcTemplate().setQueryTimeout(jdbcTemplate.getQueryTimeout());
            jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall, cachedMetaDataValue.inParameters.some());

            // record time of cache hit - not used anywhere yet.
            final AtomicLong accessTime = cachedMetaDataValue.lastUseTime;
            accessTime.set(timeSource.currentTimeMillis());

        } else {
            // cache miss, most likely because the connection name references a context variable
            synchronized (uniqueKey.toString().intern()) {
                // Any concurrent requests for the same key must wait to avoid the database being bombarded for meta data.
                // double check locking
                if (!simpleJdbcCallCache.containsKey(uniqueKey)) {
                    updateCache(connectionName, query, jdbcTemplate, schemaName);
                    // record this key for maintenance by the background cache task
                    registerQueryForPossibleCaching(connectionName, query, schemaName.isSome() ? schemaName.some() : null);
                }
            }
            // There is now guaranteed to be a cache hit for the uniqueKey
            final CachedMetaDataValue cachedMetaDataValue = simpleJdbcCallCache.get(uniqueKey);
            final Either<DataAccessException, SimpleJdbcCall> either = cachedMetaDataValue.cachedData;
            if (either.isLeft()) {
                // now throw the exception.... and again and again until the cache is updated
                throw either.left();
            }

            jdbcCallUtil = new JdbcCallHelper(either.right(), cachedMetaDataValue.inParameters.some());
        }
        return jdbcCallUtil;
    }

    static List<String> getInParameters(@NotNull Option<String> schemaName, @NotNull String procName, @NotNull SimpleJdbcCall simpleJdbcCall) {
        final boolean hasSchemaName = schemaName.isSome() && !schemaName.some().trim().isEmpty();
        return getInParametersName(procName, hasSchemaName ? schemaName.some() : null, simpleJdbcCall);
    }

    /**
     * Does not throw DataAccessException. Any exceptions are cached and are managed by a separate clean up task.
     */
    private void updateCache(@NotNull final String connectionName,
                             @NotNull final String query,
                             @NotNull final JdbcTemplate jdbcTemplate,
                             @NotNull final Option<String> schemaName) {

        final CachedMetaDataKey cacheKey = getCacheKey(connectionName, query, schemaName);
        try {
            final SimpleJdbcCall simpleJdbcCall = buildSimpleJdbcCall(jdbcTemplate, query, schemaName);
            final List<String> inParameters;

            //todo - compile and getInParameters will obtain the same meta data. - reduce to a single call.

            // Compile the simple JDBC call so it's ready for use. Any further calls to compile will not cause database traffic.
            simpleJdbcCall.compile();

            inParameters = getInParameters(schemaName, cacheKey.procName, simpleJdbcCall);
            final Either<DataAccessException, SimpleJdbcCall> rightEither = Either.<DataAccessException, SimpleJdbcCall>right(simpleJdbcCall);

            final CachedMetaDataValue cacheValue = new CachedMetaDataValue(rightEither, inParameters, timeSource.currentTimeMillis());
            simpleJdbcCallCache.put(cacheKey, cacheValue);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Updated meta data for connection '" + connectionName + "' for procedure / function '" + cacheKey.procName + "' "
                        + ((schemaName.isSome()) ? " in schema '" + schemaName.some() + "'" : ""));
            }
        } catch (DataAccessException e) {
            // This code block can create a queue of requests all waiting for meta data
            // if we do not record a failure object, then each request in turn will try and get meta data
            // this makes the wait time a real issue as it's multiplied by the number of requests
            // this can appear to make the gateway appear to be non responsive if all incoming
            // IO threads become blocked. For these reasons we will record a failed meta data
            // attempt to stop waiting threads from attempting to re-download the same meta data.
            // Due to the 'stress' on the database we will not allow all requests to simply proceed

            final Either<DataAccessException, SimpleJdbcCall> leftEither = Either.left(e);
            final CachedMetaDataValue cacheValue = new CachedMetaDataValue(leftEither, timeSource.currentTimeMillis());
            simpleJdbcCallCache.put(cacheKey, cacheValue);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Updated meta data with failed entry for connection '" + connectionName + "' for procedure / function '" + cacheKey.procName + "' "
                        + ((schemaName.isSome()) ? " in schema '" + schemaName.some() + "'" : ""));
            }
        }
    }

    /**
     * Custom SimpleJdbcCall for SSG-6544 to reflectively fix incorrect java.sql.Types meta data returned from
     * Data Direct driver for Oracle. This class will fix Clob meta data for any driver type.
     * These classes require intimate knowledge of spring-jdbc-3.0.7.RELEASE source code and will most likely
     * break if spring is upgraded. This is a stop gap until the data direct drivers are patched by Progress software.
     */
    private static class SimpleJdbcCallWithReturnClobSupport extends SimpleJdbcCall {

        public SimpleJdbcCallWithReturnClobSupport(JdbcTemplate jdbcTemplate) throws NoSuchFieldException, IllegalAccessException {
            super(jdbcTemplate);

            // override default value for instance variable to custom value which can correct the meta data
            final Field contextField = AbstractJdbcCall.class.getDeclaredField("callMetaDataContext");
            contextField.setAccessible(true);
            contextField.set(this, new ClobTypeFixingCallMetaDataContext());
        }
    }

    private static class ClobTypeFixingCallMetaDataContext extends CallMetaDataContext {

        @Override
        public void initializeMetaData(DataSource dataSource) throws RuntimeException {
            super.initializeMetaData(dataSource);

            //Before any clients can access the meta data, fix it for CLOB java.sql.Types
            final GenericCallMetaDataProvider genericProvider;
            try {
                genericProvider = (GenericCallMetaDataProvider) metaDataProviderField.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            final List<CallParameterMetaData> callParameterMetaData;
            try {
                callParameterMetaData = (List<CallParameterMetaData>) listOfCallParamMetaDataField.get(genericProvider);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // fix each param meta data
            for (CallParameterMetaData paramMetaData : callParameterMetaData) {
                final String typeName = paramMetaData.getTypeName();
                if (typeName != null && "CLOB".equalsIgnoreCase(typeName)) {
                    final int sqlType = paramMetaData.getSqlType();
                    if (2005 != sqlType) {
                        // This value is part of the JDK JDBC specification, any non 2005 value is an error.
                        try {
                            sqlTypeField.set(paramMetaData, 2005);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        //- PRIVATE
        private final Field metaDataProviderField;
        private final Field listOfCallParamMetaDataField;
        private final Field sqlTypeField;

        private ClobTypeFixingCallMetaDataContext() throws NoSuchFieldException {
            metaDataProviderField = CallMetaDataContext.class.getDeclaredField("metaDataProvider");
            metaDataProviderField.setAccessible(true);
            listOfCallParamMetaDataField = GenericCallMetaDataProvider.class.getDeclaredField("callParameterMetaData");
            listOfCallParamMetaDataField.setAccessible(true);
            sqlTypeField = CallParameterMetaData.class.getDeclaredField("sqlType");
            sqlTypeField.setAccessible(true);
        }
    }

    /**
     * Build a SimpleJdbcCall which IS NOT compiled.
     */
    private static SimpleJdbcCall buildSimpleJdbcCall(@NotNull JdbcTemplate jdbcTemplate, @NotNull String query, @NotNull Option<String> schemaName) throws DataAccessException {
        final SimpleJdbcCall simpleJdbcCall;
        try {
            simpleJdbcCall = new SimpleJdbcCallWithReturnClobSupport(jdbcTemplate);
        } catch (Exception e) {
            throw new DataAccessException("Unexpected error initializing custom JDBC template: " + ExceptionUtils.getMessage(e), e){
                // exception is abstract -> subclass it
            };
        }
        final String procName = JdbcUtil.getName(query);
        simpleJdbcCall.setProcedureName(procName);
        simpleJdbcCall.setFunction(query.toLowerCase().startsWith(JdbcCallHelper.SQL_FUNCTION));

        if (schemaName.isSome() && !schemaName.some().isEmpty()) {
            simpleJdbcCall.setSchemaName(schemaName.some());
        }

        return simpleJdbcCall;
    }

    /**
     * Build a JdbcTemplate which always has a query time out set and max rows if the value is >= 0.
     *
     * The query timeout will never be set larger than the system wide query timeout, but the input timeoutSeconds will
     * be when it is > 0 and < system wide timeout.
     */
    private JdbcTemplate buildJdbcTemplate(@NotNull final DataSource dataSource, final int maxRecords, final int timeoutSeconds){
        // Create a JdbcTemplate and set the max rows.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        if (maxRecords >= 0) {
            jdbcTemplate.setMaxRows(maxRecords);
        }

        final int maxTimeOutToUse = getMaxQueryTimeout(timeoutSeconds);
        assert maxTimeOutToUse > 0;
       // There is always a query timeout. Never allow any query without one.
        jdbcTemplate.setQueryTimeout(maxTimeOutToUse);

        return jdbcTemplate;
    }

    private int getMaxQueryTimeout(int timeoutSeconds) {
        final int maxTimeOutToUse;
        {// do not accidentally let the wrong timeout leak out of scope
            final int maxSystemTimeout = config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_TIME_OUT, 300);
            if (timeoutSeconds < maxSystemTimeout && timeoutSeconds > 0) {
                maxTimeOutToUse = timeoutSeconds;
            } else {
                maxTimeOutToUse = maxSystemTimeout;
            }
        }
        return maxTimeOutToUse;
    }

    /**
     * Get the input parameter names of the procedure
     *
     *
     * @param procName name of procedure / function to get in parameters for
     * @param simpleJdbcCall SimpleJdbcCall used only for access to it's JdbcTemplate, so a DataSource can be retrieved
     * @return List of in parameter names.
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
     * WARNING - the key generated is based on the JDBC Connection name, procedure name and optional schema.
     * .
     * This is unique enough only if procedure / function overloading is not supported.
     * Currently this is not supported by Spring.
     *
     *
     * @param connectionName unique JDBC Connection name
     * @param query full sql query as typed into the JDBC Query assertion which contains the procedure or function being called.
     * @param schemaName optional schema name
     * @return unique cache key
     */
    private static CachedMetaDataKey getCacheKey(@NotNull String connectionName, @NotNull String query, @NotNull Option<String> schemaName) {
        return new CachedMetaDataKey(connectionName, query, schemaName);
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
     * If the task is currently running it will be stopped first.
     */
    private void createAndStartMetaDataTask() {
        stopCurrentTaskIfRunning();
        assert currentCacheTask.get() == null;
        final long refreshInterval = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_REFRESH_INTERVAL, 600000L);
        final MetaDataCacheTask newTask = new MetaDataCacheTask(jdbcConnectionPoolManager);
        currentCacheTask.set(newTask);
        downloadMetaDataTimer.schedule(currentCacheTask.get(), 1000L, refreshInterval);
        logger.info("Starting the JDBC Query meta data cache task with refresh interval of " + refreshInterval + " milliseconds");
    }

    private void stopCurrentTaskIfRunning() {
        final MetaDataCacheTask currentTask = currentCacheTask.get();
        if (currentTask != null) {
            currentTask.cancel();
            currentCacheTask.set(null);
            logger.info("Cancelled JDBC Query meta data cache task");
        }
    }

    private void createAndStartCleanUpTask() {
        stopCleanUpTaskIfRunning();
        assert currentCleanUpTask.get() == null;
        final long refreshInterval = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_CLEANUP_REFRESH_INTERVAL, 60000L);
        final MetaDataCleanUpExceptionsTask newTask = new MetaDataCleanUpExceptionsTask();
        currentCleanUpTask.set(newTask);
        cleanUpTimer.schedule(currentCleanUpTask.get(), 1000L, refreshInterval);
        logger.info("Starting the JDBC Query meta data cache clean up task with refresh interval of " + refreshInterval + " milliseconds");
    }

    private void stopCleanUpTaskIfRunning() {
        final MetaDataCleanUpExceptionsTask currentTask = currentCleanUpTask.get();
        if (currentTask != null) {
            currentTask.cancel();
            currentCleanUpTask.set(null);
            logger.info("Cancelled JDBC Query meta data cache clean up task");
        }
    }

    private boolean isCachingAllowed() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED, true);
    }

    private void doStart() {
        final boolean enableCacheTask = config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_TASK_ENABLED, true);
        if (enableCacheTask) {
            createAndStartMetaDataTask();
            createAndStartCleanUpTask();
        }

    }

    private void doStop() {
        stopCurrentTaskIfRunning();
        stopCleanUpTaskIfRunning();
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

    /**
     * Cache key for cached meta data. This class may be placed into Sets and Maps as a key
     *
     * The key is based on the unique JDBC Connection name and a procedure name and an optional schema name.
     *
     * This class is immutable.
     */
    private static class CachedMetaDataKey {

        // - PUBLIC

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CachedMetaDataKey that = (CachedMetaDataKey) o;

            if (!connectionName.equals(that.connectionName)) return false;
            if (!procName.equals(that.procName)) return false;
            if (!schemaNameOption.equals(that.schemaNameOption)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = connectionName.hashCode();
            result = 31 * result + procName.hashCode();
            result = 31 * result + schemaNameOption.hashCode();
            return result;
        }

        /**
         * The query is not part of toString
         * @return String representation that can be used to synchronize on so long as {@link String#intern()} is used first
         */
        @Override
        public String toString() {
            return "CachedMetaDataKey{" +
                    "connectionName='" + connectionName + '\'' +
                    ", procName='" + procName + '\'' +
                    ", schemaName=" + (schemaNameOption.isSome() ? schemaNameOption.some() : "") +
                    '}';
        }

        // - PRIVATE

        private final String connectionName;
        private final String procName;
        private final Option<String> schemaNameOption;

        /**
         * Query is stored as additional data only. It is not part of the key
         *
         * Warning: Do not include query in toString, equals or hashCode. Two queries may represent the same procedure
         * / function but may be expressed differently e.g. literal versus dynamic parameters in query string
         */
        private final String query;


        /**
         * Create a new cache key. The key will ultimately be the unique combination of the connection name, procedure
         * or function name and the optional schema name. The query IS NOT part of the key.
         *
         * @param connectionName   unique JDBC connection name
         * @param query            the SQL query containing the procedure or function name
         * @param schemaNameOption the optional schema name
         * @throws IllegalArgumentException if the procedure / function name cannot be extracted from the query
         */
        private CachedMetaDataKey(@NotNull String connectionName, @NotNull String query, @NotNull Option<String> schemaNameOption)
                throws IllegalArgumentException {
            this.connectionName = connectionName;
            this.query = query;
            this.procName = JdbcUtil.getName(query);
            if (this.procName.trim().isEmpty()) {
                throw new IllegalArgumentException("Query produced an empty procedure / function name");
            }
            this.schemaNameOption = schemaNameOption;
        }

    }

    /**
     * Cached data, not intended for use as a key in a collection.
     *
     * Caches SimpleJdbcCall objects which are both cacheable and thread safe. Internally they use a DataSource which
     * means they do not hold onto java.sql.Connection objects are obtain them as needed from the DataSource.
     *
     * Warning: This class is mutable
     */
    private static class CachedMetaDataValue {
        // - PRIVATE

        private final Either<DataAccessException, SimpleJdbcCall> cachedData;
        private final Option<List<String>> inParameters;
        /**
         * Either last access time for a right either or time of exception for a left either.
         * //todo - make use of this for expiring items from the cache
         */
        private final AtomicLong lastUseTime;

        private CachedMetaDataValue(@NotNull Either<DataAccessException, SimpleJdbcCall> cachedData, @NotNull List<String> inParameters, @NotNull Long lastUseTime) {
            this.cachedData = cachedData;
            this.inParameters = Option.optional(inParameters);
            this.lastUseTime = new AtomicLong(lastUseTime);
        }

        private CachedMetaDataValue(@NotNull Either<DataAccessException, SimpleJdbcCall> cachedData, @NotNull Long lastUseTime) {
            this.cachedData = cachedData;
            this.lastUseTime = new AtomicLong(lastUseTime);
            inParameters = Option.optional(null);
        }

    }

    private class MetaDataCacheTask extends ManagedTimerTask {

        private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
        private boolean isCancelled;

        private MetaDataCacheTask(@NotNull JdbcConnectionPoolManager jdbcConnectionPoolManager) {
            this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        }

        /**
         * Support a very eager cancel due to the duration this task may take to complete.
         */
        @Override
        public boolean cancel() {
            isCancelled = true;
            return super.cancel();
        }

        @Override
        protected void doRun() {
            final Set<CachedMetaDataKey> keysToMaintain = new HashSet<CachedMetaDataKey>(dbObjectsToCacheMetaDataFor);
            logger.fine("Task to cache JDBC function / procedure meta data is starting. There are " + keysToMaintain.size() + " unique items to update.");

            // record time as it may take a long time to run.
            final long startTime = System.currentTimeMillis();
            for (CachedMetaDataKey key : keysToMaintain) {
                final String connectionName = key.connectionName;
                try {

                    final DataSource dataSource = jdbcConnectionPoolManager.getDataSource(connectionName);
                    final int timeoutSeconds = config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_MAX_TIME_OUT, 120);
                    updateCache(connectionName, key.query, buildJdbcTemplate(dataSource, 0, timeoutSeconds), key.schemaNameOption);
                    if (isCancelled) {
                        // eagerly cancel
                        logger.info("JDBC Query meta data cache task has been cancelled.");
                        break;
                    }
                } catch (NamingException e) {
                    logger.warning("Could not get data source for connection: '" + connectionName+"' due to: " + ExceptionUtils.getMessage(e));
                } catch (SQLException e) {
                    logger.warning("Could not get data source for connection: '" + connectionName + "' due to: " + ExceptionUtils.getMessage(e));
                }
            }
            logger.fine("Task to cache JDBC function / procedure meta data has finished. Task took " + (System.currentTimeMillis() - startTime) +" milliseconds to complete.");
        }
    }

    /**
     * Allowed failed meta data calls to be retried sooner than the manage meta data task by clearing them from
     * the cache faster.
     *
     * If a JDBC Connection entity name is removed or modified then any cached entries will be cleared
     */
    private class MetaDataCleanUpExceptionsTask extends ManagedTimerTask {
        @Override
        protected void doRun() {
            final Set<String> validEntityNames = new HashSet<String>();
            try {
                final Collection<EntityHeader> allHeaders = jdbcConnectionManager.findAllHeaders();
                for (EntityHeader header : allHeaders) {
                    final String name = header.getName();
                    validEntityNames.add(name);
                }
            } catch (FindException e) {
                logger.warning("Could not check list of JDBC Connections for cache maintenance: " + ExceptionUtils.getMessage(e));
            }

            final Set<CachedMetaDataKey> keysToRemove = new HashSet<CachedMetaDataKey>();

            final long maxExceptionAge = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_CLEANUP_REFRESH_INTERVAL, 60000L);
            for (Map.Entry<CachedMetaDataKey, CachedMetaDataValue> entry : simpleJdbcCallCache.entrySet()) {

                final CachedMetaDataValue value = entry.getValue();
                final CachedMetaDataKey key = entry.getKey();
                final Either<DataAccessException, SimpleJdbcCall> either = value.cachedData;
                if (either.isLeft()) {
                    // it's an exception, see hold old it is
                    final Long age = timeSource.currentTimeMillis() - value.lastUseTime.get();
                    if (age > maxExceptionAge) {
                        keysToRemove.add(key);
                    }
                }

                // if the jdbc connection entity no longer exists then remove items from the cache
                if (!validEntityNames.contains(key.connectionName)) {
                    keysToRemove.add(key);
                }
            }

            for (CachedMetaDataKey key : keysToRemove) {
                simpleJdbcCallCache.remove(key);
            }
        }
    }

}
