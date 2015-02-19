package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.metadata.*;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
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
    private final AtomicReference<MetaDataCacheCleanUpTask> currentCleanUpTask = new AtomicReference<MetaDataCacheCleanUpTask>();

    private final Map<CachedMetaDataKey, CachedMetaDataValue> simpleJdbcCallCache = new ConcurrentHashMap<CachedMetaDataKey, CachedMetaDataValue>();

    /**
     * Set of unique procedures / functions to manage meta data for.
     */
    private final Set<CachedMetaDataKey> dbObjectsToCacheMetaDataFor = new ConcurrentHashSet<CachedMetaDataKey>();

    //The thread pool used to execute metadata retrieval tasks. This must be dynamically created because it's running depends on the PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED cluster property
    private ThreadPool jdbcMetadataRetrievalThreadPool;
    public static final String DOWNLOAD_TASK_LABEL = "JDBC meta data download task ";
    public static final String CLEAN_UP_TASK_LABEL = "JDBC cache clean up task ";

    public JdbcQueryingManagerImpl(final JdbcConnectionPoolManager jdbcConnectionPoolManager,
                                   final JdbcConnectionManager jdbcConnectionManager,
                                   final Config config,
                                   final TimeSource timeSource) {
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.config = validated(config);
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
        final String propertyName = evt.getPropertyName();
        synchronized (currentCacheTask) {
            final boolean cacheMetadataTaskEnabled = config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, true);
            switch (propertyName) {
                case ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED:
                    if (cacheMetadataTaskEnabled) {
                        createAndStartMetaDataTask();
                    } else {
                        stopCurrentTaskIfRunning();
                    }
                    break;
                case ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL:
                    if (cacheMetadataTaskEnabled) {
                        // only reconfigure the task if the cache is actually enabled.
                        createAndStartMetaDataTask();
                    }
                    break;
                case ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL:
                    createAndStartCleanUpTask();
                    break;
            }
        }
        //update the jdbcMetadataRetrievalThreadPool number of threads
        if (propertyName.equals(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY) && jdbcMetadataRetrievalThreadPool != null) {
            //noinspection SynchronizeOnNonFinalField
            synchronized (jdbcMetadataRetrievalThreadPool){
                int numberCacheConcurrencyThreads = getNumberCacheConcurrencyThreads();
                jdbcMetadataRetrievalThreadPool.setCorePoolSize(numberCacheConcurrencyThreads);
                jdbcMetadataRetrievalThreadPool.setMaxPoolSize(numberCacheConcurrencyThreads);
            }
        }
    }

    @Override
    public void registerQueryForPossibleCaching(@NotNull String connectionName, @NotNull String query, @Nullable String schemaName) {
        if (JdbcUtil.isStoredProcedure(query) && !Syntax.isAnyVariableReferenced(connectionName) && (schemaName == null || !Syntax.isAnyVariableReferenced(schemaName))) {
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
            logger.warning("Failed to perform querying since " + ExceptionUtils.getMessage(e) + " " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e)));
            return "Cannot retrieve a C3P0 DataSource: " + ExceptionUtils.getMessage(e) + " " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
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
                return "Could not get JDBC Connection: " + ExceptionUtils.getMessage(e) + " " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            } else if (ExceptionUtils.causedBy(e, BadSqlGrammarException.class)) {
                return "Bad SQL Grammar: " + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e));
            } else if (ExceptionUtils.causedBy(e, InvalidDataAccessApiUsageException.class)
                    && e.getMessage().contains("Unable to determine the correct call signature for")
                    && e.getMessage().contains("package name should be specified separately using '.withCatalogName(")) {
                return "The database object either does not exist or the SQL query contains the object's schema e.g. myschema.myobject";
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
            logger.log(Level.FINEST, "Cache hit for key: {0}", uniqueKey);
            CachedMetaDataValue cachedMetaDataValue = simpleJdbcCallCache.get(uniqueKey);

            // Check to see if the cache has expired.
            final long maxStaleAgeMillis = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_STALE_TIMEOUT, 1800) * 1000;
            Long age = timeSource.currentTimeMillis() - cachedMetaDataValue.cachedTime.get();
            if (maxStaleAgeMillis > 0 && age > maxStaleAgeMillis) {
                // if it has then re-cache it.
                synchronized (uniqueKey.toString().intern()) {
                    // Synchronization required for updateCache when cached item has expired.
                    // This is needed to avoid all message processing threads for the unique cache key attempting to
                    // download the same meta during the time it takes for this task to complete.
                    // double check locking
                    age = timeSource.currentTimeMillis() - cachedMetaDataValue.cachedTime.get();
                    if (age > maxStaleAgeMillis) {
                        logger.log(Level.FINEST, "Cache expired - too old - for key: {0}", uniqueKey);
                        updateCache(connectionName, query, jdbcTemplate, schemaName);
                        cachedMetaDataValue = simpleJdbcCallCache.get(uniqueKey);
                    }
                }
            }

            final Either<DataAccessException, SimpleJdbcCall> either = cachedMetaDataValue.cachedData;
            if (either.isLeft()) {
                throw either.left();
            }
            simpleJdbcCall = either.right();
            // update the cached timeout in case the caller has changed it's value
            simpleJdbcCall.getJdbcTemplate().setQueryTimeout(jdbcTemplate.getQueryTimeout());
            jdbcCallUtil = new JdbcCallHelper(simpleJdbcCall, cachedMetaDataValue.inParameters.some());

            // record time of cache hit
            final AtomicLong accessTime = cachedMetaDataValue.lastUseTime;
            accessTime.set(timeSource.currentTimeMillis());

        } else {
            final CachedMetaDataValue cachedMetaDataValue;
            // cache miss, most likely because the connection name references a context variable
            synchronized (uniqueKey.toString().intern()) {
                // Any concurrent requests for the same key must wait to avoid the database being bombarded for meta data.
                // double check locking
                if (!simpleJdbcCallCache.containsKey(uniqueKey)) {
                    logger.log(Level.FINEST, "Cache miss for key: {0}", uniqueKey);
                    updateCache(connectionName, query, jdbcTemplate, schemaName);
                    // record this key for maintenance by the background cache task
                    registerQueryForPossibleCaching(connectionName, query, schemaName.isSome() ? schemaName.some() : null);
                }
                // There is now guaranteed to be a cache hit for the uniqueKey
                cachedMetaDataValue = simpleJdbcCallCache.get(uniqueKey);
            }
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
        updateCache(connectionName, query, jdbcTemplate, schemaName, null);
    }

    /**
     * Does not throw DataAccessException. Any exceptions are cached and are managed by a separate clean up task.
     */
    private void updateCache(@NotNull final String connectionName,
                             @NotNull final String query,
                             @NotNull final JdbcTemplate jdbcTemplate,
                             @NotNull final Option<String> schemaName,
                             @Nullable final Long lastUsedTime) {

        final CachedMetaDataKey cacheKey = getCacheKey(connectionName, query, schemaName);
        try {
            final SimpleJdbcCall simpleJdbcCall = buildSimpleJdbcCall(jdbcTemplate, query, schemaName);
            final List<String> inParameters;

            //todo - compile and getInParameters will obtain the same meta data. - reduce to a single call.

            // Compile the simple JDBC call so it's ready for use. Any further calls to compile will not cause database traffic.
            simpleJdbcCall.compile();

            inParameters = getInParameters(schemaName, cacheKey.procName, simpleJdbcCall);
            final Either<DataAccessException, SimpleJdbcCall> rightEither = Either.<DataAccessException, SimpleJdbcCall>right(simpleJdbcCall);

            long currentTime = timeSource.currentTimeMillis();
            final CachedMetaDataValue cacheValue = new CachedMetaDataValue(rightEither, inParameters, lastUsedTime != null ? lastUsedTime : currentTime, currentTime);
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
            long currentTime = timeSource.currentTimeMillis();
            final CachedMetaDataValue cacheValue = new CachedMetaDataValue(leftEither, lastUsedTime != null ? lastUsedTime : currentTime, currentTime);
            simpleJdbcCallCache.put(cacheKey, cacheValue);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Updated meta data with failed entry for connection '" + connectionName + "' for procedure / function '" + cacheKey.procName + "' "
                        + ((schemaName.isSome()) ? " in schema '" + schemaName.some() + "'" : ""));
            }
        }
    }


    /**
     * Build a SimpleJdbcCall which IS NOT compiled.
     */
    private static SimpleJdbcCall buildSimpleJdbcCall(@NotNull JdbcTemplate jdbcTemplate, @NotNull String query, @NotNull Option<String> schemaName) throws DataAccessException {
        final SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate);

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
            final int maxSystemTimeout = config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 300);
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
        final long refreshInterval = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, 600000L);
        if (refreshInterval > 0) {
            createAndStartJDBCMetadataRetrievalThreadPool();
            final MetaDataCacheTask newTask = new MetaDataCacheTask(jdbcConnectionPoolManager);
            currentCacheTask.set(newTask);
            downloadMetaDataTimer.schedule(currentCacheTask.get(), 1000L, refreshInterval);
            logger.info(DOWNLOAD_TASK_LABEL + "is starting with refresh interval of " + refreshInterval + " milliseconds");
        }
    }

    private void stopCurrentTaskIfRunning() {
        final MetaDataCacheTask currentTask = currentCacheTask.get();
        if (currentTask != null) {
            currentTask.cancel();
            currentCacheTask.set(null);
            logger.info(DOWNLOAD_TASK_LABEL + "is stopping");
        }
        stopJDBCMetadataRetrievalThreadPoolIfRunning();
    }

    private void createAndStartJDBCMetadataRetrievalThreadPool() {
        int numThreads = getNumberCacheConcurrencyThreads();
        jdbcMetadataRetrievalThreadPool = new ThreadPool(
                "JDBC Metadata Retrieval Thread Pool", //poolName
                numThreads, //corePoolSize core size of the thread pool. Always <= of the maximum pool size.
                numThreads, //maxPoolSize maximum size the pool may reach. Must be >= corePoolSize.
                Integer.MAX_VALUE, //maxQueuedTasks maximum number of tasks which can be queued before a new thread is created.
                30000l, //keepAliveTime how long threads above the core size can idle before being terminating.
                java.util.concurrent.TimeUnit.MILLISECONDS, //timeUnit time unit which applies to keep alive times.
                true, //allowCoreThreadTimeOuts if true, then core threads are allowed to terminate when idle.
                null, //rejectedExecutionHandler if null the default ThreadPoolExecutor.AbortPolicy is used, which will throw when maximum threads have been created and the bound queue is full.
                null, //beforeExecute hook to run before a task is executed, can be null.
                null); //afterExecute hook to run after a task is executed, can be null.
        jdbcMetadataRetrievalThreadPool.start();
    }

    private void stopJDBCMetadataRetrievalThreadPoolIfRunning() {
        if(jdbcMetadataRetrievalThreadPool != null) {
            jdbcMetadataRetrievalThreadPool.shutdown();
            // Thread pools cannot be stopped and started so we set it to null.
            jdbcMetadataRetrievalThreadPool = null;
        }
    }

    private void createAndStartCleanUpTask() {
        stopCleanUpTaskIfRunning();
        assert currentCleanUpTask.get() == null;
        final long cleanUpInterval = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, 60000L);
        if (cleanUpInterval > 0) {
            final MetaDataCacheCleanUpTask newTask = new MetaDataCacheCleanUpTask();
            currentCleanUpTask.set(newTask);
            cleanUpTimer.schedule(currentCleanUpTask.get(), 1000L, cleanUpInterval);
            logger.info(CLEAN_UP_TASK_LABEL + "starting with refresh interval of " + cleanUpInterval + " milliseconds");
        }
    }

    private void stopCleanUpTaskIfRunning() {
        final MetaDataCacheCleanUpTask currentTask = currentCleanUpTask.get();
        if (currentTask != null) {
            currentTask.cancel();
            currentCleanUpTask.set(null);
            logger.info(CLEAN_UP_TASK_LABEL + "is stopping");
        }
    }

    private boolean isCachingAllowed() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_ENABLED, true);
    }

    private int getNumberCacheConcurrencyThreads() {
        return config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, 10);
    }

    private void doStart() {
        final boolean enableCacheTask = config.getBooleanProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, true);
        if (enableCacheTask) {
            createAndStartMetaDataTask();
        }
        createAndStartCleanUpTask();
   }

    private void doStop() {
        stopCurrentTaskIfRunning();
        stopCleanUpTaskIfRunning();
    }

    private static Config validated(final Config config) {
        final ValidatedConfig vc = new ValidatedConfig(config, logger);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, Long.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, Long.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, 1);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, 200);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, Long.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 1);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, Integer.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, Integer.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_STALE_TIMEOUT, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_STALE_TIMEOUT, Long.MAX_VALUE);


        return vc;
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
        private final AtomicLong lastUseTime;
        private AtomicLong cachedTime;

        private CachedMetaDataValue(@NotNull Either<DataAccessException, SimpleJdbcCall> cachedData, @NotNull List<String> inParameters, @NotNull Long lastUseTime, @NotNull Long cachedTime) {
            this.cachedData = cachedData;
            this.inParameters = Option.optional(inParameters);
            this.lastUseTime = new AtomicLong(lastUseTime);
            this.cachedTime = new AtomicLong(cachedTime);
        }

        private CachedMetaDataValue(@NotNull Either<DataAccessException, SimpleJdbcCall> cachedData, @NotNull Long lastUseTime, @NotNull Long cachedTime) {
            this.cachedData = cachedData;
            this.lastUseTime = new AtomicLong(lastUseTime);
            inParameters = Option.optional(null);
            this.cachedTime = new AtomicLong(cachedTime);
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
            logger.fine(DOWNLOAD_TASK_LABEL + "is starting. There are " + keysToMaintain.size() + " unique items to update.");

            //This will be a list of all the tasks being run in this process
            final ArrayList<Future<Void>> metadataRetrievalTasks = new ArrayList<>(keysToMaintain.size());
            // record time as it may take a long time to run.
            final long startTime = System.currentTimeMillis();
            for (final CachedMetaDataKey key : keysToMaintain) {
                if (isCancelled) {
                    // eagerly cancel
                    logger.info(DOWNLOAD_TASK_LABEL + "has been cancelled.");
                    break;
                }
                final String connectionName = key.connectionName;
                try {
                    Future<Void> future = jdbcMetadataRetrievalThreadPool.submitTask(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                        try {

                            final DataSource dataSource = jdbcConnectionPoolManager.getDataSource(connectionName);
                            final int timeoutSeconds = config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, 120);
                            //need to preserve the last access time if the item is already in cache.
                            Long lastUseTime = null;
                            if (simpleJdbcCallCache.containsKey(key)) {
                                lastUseTime = simpleJdbcCallCache.get(key).lastUseTime.get();
                            }
                            synchronized (key.toString().intern()) {
                                // Synchronization around updateCache.
                                // This is needed to avoid downloading the same meta during the time it takes for this task to complete.
                                // This can happen when a single message processing thread and the this thread update cache for the same function, which has a very low chance of actually happening.
                                //TODO: investigate the use of a minimum refresh age based on the CachedMetaDataValue.cachedTime, this should prevent re-caching metadata if it was just cached.
                                updateCache(connectionName, key.query, buildJdbcTemplate(dataSource, 0, timeoutSeconds), key.schemaNameOption, lastUseTime);
                            }
                        } catch (NamingException | SQLException e) {
                            logger.warning(DOWNLOAD_TASK_LABEL + ": Could not get data source for connection: '" + connectionName + "' due to: " + ExceptionUtils.getMessage(e));
                        }
                        return null;
                        }
                    });
                    // Add the metadata retrieval task to the list of tasks run in this process
                    metadataRetrievalTasks.add(future);
                } catch (ThreadPool.ThreadPoolShutDownException e) {
                    logger.warning(DOWNLOAD_TASK_LABEL + "thread pool was shutdown: " + ExceptionUtils.getMessage(e));
                } catch (Exception e) {
                    // This will catch any other exceptions that may have occurred.
                    logger.warning(DOWNLOAD_TASK_LABEL + "thread pool exception occurred submitting a task: " + ExceptionUtils.getMessage(e));
                }
            }
            //Wait till all tasks complete
            for (Future<Void> task : metadataRetrievalTasks) {
                try {
                    if (!isCancelled) {
                        //A timeout here should not be needed. JDBC calls should automatically fail if they take linger then ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MAX_TIME_OUT
                        task.get();
                    } else {
                        //Do not want to interrupt here, may cause unexpected side effects
                        task.cancel(false);
                        //Should wait for all tasks to complete before exiting the MetadataCacheTask
                        task.get();
                    }
                } catch (InterruptedException e) {
                    logger.warning(DOWNLOAD_TASK_LABEL + "was interrupted while processing: " + ExceptionUtils.getMessage(e));
                } catch (ExecutionException e) {
                    logger.warning(DOWNLOAD_TASK_LABEL + "threw an exception: " + ExceptionUtils.getMessage(e));
                }
            }

            logger.fine(DOWNLOAD_TASK_LABEL + "has finished. Task took " + (System.currentTimeMillis() - startTime) + " milliseconds to complete.");
        }
    }

    /**
     * Allowed failed meta data calls to be retried sooner than the manage meta data task by clearing them from
     * the cache faster. This is also remove all metadata from the cache that have not been used for a long time (default is 31 days)
     *
     * If a JDBC Connection entity name is removed or modified then any cached entries will be cleared
     */
    private class MetaDataCacheCleanUpTask extends ManagedTimerTask {
        @Override
        protected void doRun() {
            final long startTime = System.currentTimeMillis();
            logger.fine(CLEAN_UP_TASK_LABEL + "is starting. There are " + simpleJdbcCallCache.size() + " items in the cache.");
            final Set<String> validEntityNames = new HashSet<>();
            try {
                final Collection<EntityHeader> allHeaders = jdbcConnectionManager.findAllHeaders();
                for (EntityHeader header : allHeaders) {
                    final String name = header.getName();
                    validEntityNames.add(name);
                }
            } catch (FindException e) {
                logger.warning(CLEAN_UP_TASK_LABEL + ": Could not check list of JDBC Connections for cache maintenance: " + ExceptionUtils.getMessage(e));
            }

            final Map<CachedMetaDataKey, String> keysToRemove = new HashMap<>();
            final Set<CachedMetaDataKey> keysToRemoveFromDBObjectsToCacheMetaDataFor = new HashSet<>();

            final long cacheKeyNoUsageExpiration = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, 2678400L) * 1000L;
            for (Map.Entry<CachedMetaDataKey, CachedMetaDataValue> entry : simpleJdbcCallCache.entrySet()) {

                final CachedMetaDataValue value = entry.getValue();
                final CachedMetaDataKey key = entry.getKey();
                final Either<DataAccessException, SimpleJdbcCall> either = value.cachedData;
                final Long age = timeSource.currentTimeMillis() - value.lastUseTime.get();

                if (either.isLeft()) {
                    keysToRemove.put(key, "errored");
                }

                // if the jdbc connection entity no longer exists then remove items from the cache
                if (!validEntityNames.contains(key.connectionName)) {
                    keysToRemove.put(key, "connection no longer exists");
                }

                //remove items not used for a long time.
                if (cacheKeyNoUsageExpiration > 0 && age > cacheKeyNoUsageExpiration) {
                    keysToRemoveFromDBObjectsToCacheMetaDataFor.add(key);
                    keysToRemove.put(key, "unused");
                }
            }

            logger.fine(CLEAN_UP_TASK_LABEL + ": There are " + keysToRemoveFromDBObjectsToCacheMetaDataFor.size() + " items which will no longer be managed as there has been no usage.");
            for (CachedMetaDataKey key : keysToRemoveFromDBObjectsToCacheMetaDataFor) {
                dbObjectsToCacheMetaDataFor.remove(key);
                logger.log(Level.FINE, CLEAN_UP_TASK_LABEL + ": Cache removed from managed keys - unused - for key: {0}", key);
            }

            logger.fine(CLEAN_UP_TASK_LABEL + ": There are " + keysToRemove.size() + " items in the cache which will be removed.");
            for (CachedMetaDataKey key : keysToRemove.keySet()) {
                simpleJdbcCallCache.remove(key);
                logger.log(Level.FINE, CLEAN_UP_TASK_LABEL + ": Cache expired - {0} - for key: {1}", new Object[]{keysToRemove.get(key), key});
            }

            logger.fine(CLEAN_UP_TASK_LABEL + "has finished. Task took " + (System.currentTimeMillis() - startTime) + " milliseconds to complete.");
        }
    }

}
