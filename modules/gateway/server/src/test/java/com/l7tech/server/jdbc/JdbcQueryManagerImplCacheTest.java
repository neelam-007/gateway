package com.l7tech.server.jdbc;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.util.Option;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * This was created: 3/5/13 as 4:02 PM
 *
 * @author Victor Kazakov
 */
public class JdbcQueryManagerImplCacheTest {
    //The connection name
    protected final String ConnectionName = "MySQLConnection";
    private JdbcQueryingManager jdbcQueryingManager;
    private Map<String, String> configProperties = new HashMap<>();
    private MockConfig mockConfig = new MockConfig(configProperties);
    private DatabaseMetaData databaseMetaData;
    private String returnValueParameterName = "RETURN_VALUE";
    private Connection connection;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Sets up mocks and jdbc objects
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        DataSource dataSource = Mockito.mock(DataSource.class);

        JdbcConnectionManager jdbcConnectionManager = Mockito.spy(new JdbcConnectionManagerImpl());

        Mockito.doReturn(Collections.emptyList()).when(jdbcConnectionManager).findAll();
        Mockito.doReturn(new EntityHeaderSet<>((new EntityHeader(null, null, ConnectionName, null)))).when(jdbcConnectionManager).findAllHeaders();

        jdbcConnectionPoolManager = Mockito.spy(new JdbcConnectionPoolManager(jdbcConnectionManager));

        jdbcConnectionPoolManager.afterPropertiesSet();

        Mockito.doReturn(dataSource).when(jdbcConnectionPoolManager).getDataSource(Matchers.eq(ConnectionName));

        jdbcQueryingManager = Mockito.spy(new JdbcQueryingManagerImpl(jdbcConnectionPoolManager, jdbcConnectionManager, mockConfig, new TimeSource()));

        connection = Mockito.mock(Connection.class);

        databaseMetaData = Mockito.mock(DatabaseMetaData.class);
        //needs to be one of "MySQL","Microsoft SQL Server","Oracle","PostgreSQL" These are the databases that support functions.
        Mockito.doReturn("MySQL").when(databaseMetaData).getDatabaseProductName();
        Mockito.doReturn("MyDatabaseUsername").when(databaseMetaData).getUserName();

        Mockito.doReturn(databaseMetaData).when(connection).getMetaData();

        Mockito.doReturn(connection).when(dataSource).getConnection();

        startJDBCMetadataRetrievalThreadPool();
    }

    @After
    public void after() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        stopJDBCMetadataRetrievalThreadPool();
    }

    /**
     * Starts the jdbcMetadataRetrievalThreadPool
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void startJDBCMetadataRetrievalThreadPool() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = JdbcQueryingManagerImpl.class.getDeclaredMethod("createAndStartJDBCMetadataRetrievalThreadPool");
        m.setAccessible(true);
        m.invoke(jdbcQueryingManager);
    }

    /**
     * Stops the jdbcMetadataRetrievalThreadPool
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void stopJDBCMetadataRetrievalThreadPool() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = JdbcQueryingManagerImpl.class.getDeclaredMethod("stopJDBCMetadataRetrievalThreadPoolIfRunning");
        m.setAccessible(true);
        m.invoke(jdbcQueryingManager);
    }

    @Test
    public void testVoidFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(1);

    }

    @Test
    public void testStringReturnVoidFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testStringReturnSingleParameterFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction ?";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testStringReturnMultiParameterFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction(?, ?)";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testCallingMultipleFunctions() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        String functionName1 = "myFunction1";
        String query1 = "func myFunction1(?, ?)";
        String myReturnValue1 = "myReturnValue1";
        mockFunction(functionName1, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue1).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        String functionName2 = "myFunction2";
        String query2 = "func myFunction2(?, ?)";
        String myReturnValue2 = "myReturnValue2";
        mockFunction(functionName2, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue2).map());
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        validateCached(query1);
        validateCached(query2);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        verifyNumberGetProcedureCalls(2);
    }

    @Test
    public void testCallingMultipleFunctionsNoCache() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED, "false");

        String functionName1 = "myFunction1";
        String query1 = "func myFunction1(?, ?)";
        String myReturnValue1 = "myReturnValue1";
        mockFunction(functionName1, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue1).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        String functionName2 = "myFunction2";
        String query2 = "func myFunction2(?, ?)";
        String myReturnValue2 = "myReturnValue2";
        mockFunction(functionName2, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValue2).map());
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        validateCached(query1, false);
        validateCached(query2, false);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        verifyNumberGetProcedureCalls(6);
    }

    @Test
    public void testErrorGettingFunctionMetadata() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        String errorString = "My Test Error";
        String query = "func myFunction";
        mockMetadataError(errorString);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);

        fixMockMetadataError();

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);
    }

    @Test
    public void testErrorGettingFunctionMetadataAfterSuccess() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        String errorString = "My Test Error";
        mockMetadataError(errorString);
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        fixMockMetadataError();

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(1);

    }

    @Test
    public void testErrorGettingFunctionMetadataCacheRemoval() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_CLEANUP_REFRESH_INTERVAL, "0");

        String errorString = "My Test Error";
        String functionName = "myFunction";
        String query = "func myFunction";
        mockMetadataError(errorString);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);

        runMetaDataCacheCleanUpTask();

        validateCached(query, false);

        fixMockMetadataError();
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        runMetaDataCacheCleanUpTask();

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(1);

    }

    @Test
    public void testVoidProcedure() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String procedureName = "myProcedure";
        String query = "call myProcedure";
        mockFunction(procedureName, Collections.<Parameter>emptyList(), null);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(null, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(null, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(null, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testStringReturnVoidProcedure() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String procedureName = "myProcedure";
        Map<String, Object> myReturnValues = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue").map();
        String query = "call myProcedure";
        mockFunction(procedureName, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues.get("paramOut")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(myReturnValues, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(myReturnValues, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateProcedureReturn(myReturnValues, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testStringReturnSingleParameterProcedure() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String procedureName = "myProcedure";
        Map<String, Object> myReturnValues = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue").map();
        String query = "call myProcedure ?";
        mockFunction(procedureName, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues.get("paramOut")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateProcedureReturn(myReturnValues, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateProcedureReturn(myReturnValues, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateProcedureReturn(myReturnValues, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testStringReturnMultiParameterProcedure() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String procedureName = "myProcedure";
        Map<String, Object> myReturnValues = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue").map();
        String query = "call myProcedure(?, ?)";
        mockFunction(procedureName, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues.get("paramOut")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testMultiReturnMultiParameterProcedure() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String procedureName = "myProcedure";
        Map<String, Object> myReturnValues = CollectionUtils.<String, Object>mapBuilder().put("paramOut1", "myReturnValue1").put("paramOut2", "myReturnValue2").put("paramOut3", "myReturnValue3").map();
        String query = "call myProcedure(?, ?)";
        mockFunction(procedureName, Arrays.asList(new Parameter("paramOut1", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("paramOut2", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("paramOut3", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues.get("paramOut1")).put(3, myReturnValues.get("paramOut2")).put(5, myReturnValues.get("paramOut3")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        validateCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues, rtn);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testCallingMultipleProcedures() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        String procedureName1 = "myProcedure1";
        String query1 = "call myProcedure1(?, ?)";
        Map<String, Object> myReturnValues1 = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue1").map();
        mockFunction(procedureName1, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues1.get("paramOut")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        String procedureName2 = "myProcedure2";
        String query2 = "call myProcedure2(?, ?)";
        Map<String, Object> myReturnValues2 = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue2").map();
        mockFunction(procedureName2, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues2.get("paramOut")).map());
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        validateCached(query1);
        validateCached(query2);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        verifyNumberGetProcedureCalls(2);
    }

    @Test
    public void testCallingMultipleProceduresNoCache() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED, "false");

        String procedureName1 = "myProcedure1";
        String query1 = "call myProcedure1(?, ?)";
        Map<String, Object> myReturnValues1 = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue1").map();
        mockFunction(procedureName1, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues1.get("paramOut")).map());
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        String procedureName2 = "myProcedure2";
        String query2 = "call myProcedure2(?, ?)";
        Map<String, Object> myReturnValues2 = CollectionUtils.<String, Object>mapBuilder().put("paramOut", "myReturnValue2").map();
        mockFunction(procedureName2, Arrays.asList(new Parameter("paramOut", DatabaseMetaData.procedureColumnOut, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), CollectionUtils.<Integer, Object>mapBuilder().put(1, myReturnValues2.get("paramOut")).map());
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        validateCached(query1, false);
        validateCached(query2, false);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues1, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateProcedureReturn(myReturnValues2, rtn);

        verifyNumberGetProcedureCalls(6);
    }

    @Test
    public void testMetaDataCacheTaskNoCacheItems() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException, SQLException {
        runMetaDataCacheTask();

        String functionName = "myFunction";
        String query = "func myFunction";

        validateCached(query, false);

        mockFunction(functionName, Collections.<Parameter>emptyList(), null);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        verifyNumberGetProcedureCalls(1);
    }

    @Test
    public void testMetaDataCacheTaskCacheItemsLazyAdded() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);
        verifyNumberGetProcedureCalls(1);

        runMetaDataCacheTask();

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(2);
    }

    @Test
    public void testMetaDataCacheTaskCacheItemsManuallyAdded() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_CLEANUP_REFRESH_INTERVAL, "0");

        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, query, null);
        runMetaDataCacheTask();

        validateCached(query);
        verifyNumberGetProcedureCalls(1);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);
        validateCached(query);

        verifyNumberGetProcedureCalls(1);
        runMetaDataCacheTask();

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(2);
    }

    @Test
    public void testMetaDataCacheTaskCacheItemsExpiredStale() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_STALE_TIMEOUT, "-1");

        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, query, null);
        runMetaDataCacheTask();

        validateCached(query);
        verifyNumberGetProcedureCalls(1);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);
        validateCached(query);
        verifyNumberGetProcedureCalls(2);

        runMetaDataCacheTask();

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(4);
    }

    @Test
    public void testMetaDataCacheTaskCacheItemsExpiredUnused() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_NO_USAGE_EXPIRATION, "-1");

        String functionName = "myFunction";
        String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, query, null);
        runMetaDataCacheTask();
        validateCached(query);
        runMetaDataCacheCleanUpTask();

        validateCached(query, false);
        verifyNumberGetProcedureCalls(1);

        runMetaDataCacheTask();
        validateCached(query, false);
        verifyNumberGetProcedureCalls(1);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);
        validateCached(query);
        verifyNumberGetProcedureCalls(2);

        runMetaDataCacheCleanUpTask();

        validateCached(query, false);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(3);
    }

    @Test
    public void testMetadataRetrievalThreadPool() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        int numFunctions = 100;
        ArrayList<String> functionNames = new ArrayList<>(numFunctions * 2);
        ArrayList<String> queries = new ArrayList<>(numFunctions * 2);
        for (int i = 0; i < numFunctions; i++) {
            functionNames.add("myFunction" + i);
            queries.add("func myFunction" + i);
            mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
            jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
        }

        runMetaDataCacheTask();

        for (int i = 0; i < numFunctions; i++) {
            validateCached(queries.get(i));
        }
        verifyNumberGetProcedureCalls(numFunctions);

        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, queries.get(0), null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);
        verifyNumberGetProcedureCalls(numFunctions);

        for (int i = numFunctions; i < numFunctions * 2; i++) {
            functionNames.add("myFunction" + i);
            queries.add("func myFunction" + i);
            mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
            jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
        }

        runMetaDataCacheTask();

        for (int i = numFunctions; i < numFunctions * 2; i++) {
            validateCached(queries.get(i));
        }
        // *3 because the first numFunctions are processed twice
        verifyNumberGetProcedureCalls(numFunctions * 3);
    }

    @Test(timeout = 10000)
    public void testMetadataRetrievalThreadPoolRunMetaDataCacheTaskWithLocks() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {
        int numFunctions = 2;
        ArrayList<String> functionNames = new ArrayList<>(numFunctions * 2);
        ArrayList<String> queries = new ArrayList<>(numFunctions * 2);
        //mock all functions and register them for caching
        for (int i = 0; i < numFunctions; i++) {
            functionNames.add("myFunction" + i);
            queries.add("func myFunction" + i);
            mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
            jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
        }

        CountDownLatch lock = null;
        Future<Void> metadataTask;
        try {
            //lock function 0
            lock = lockObject(getCacheKey(ConnectionName, queries.get(0)).toString().intern());
            // run the metadata cache task. This needs to be done in a separate thread because it will not return until function0 is unlocked
            metadataTask = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runMetaDataCacheTask();
                    return null;
                }
            });

            //TODO: is there a better way then waiting 2 seconds?
            Thread.sleep(2000);
            //make sure function0 has not yet been cached and function1 has been
            validateCached(queries.get(0), false);
            validateCached(queries.get(1));
        } finally {
            if (lock != null)
                //unlock function0
                lock.countDown();
        }

        //make sure the metadatacache task completes
        metadataTask.get();

        //function0 should now have been cached.
        validateCached(queries.get(0));
        verifyNumberGetProcedureCalls(numFunctions);
    }

    @Test(timeout = 10000)
    public void testMetadataRetrievalThreadPoolRunMetaDataCacheTaskWithLocksMultipleTimes() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {
        int numFunctions = 2;
        ArrayList<String> functionNames = new ArrayList<>(numFunctions * 2);
        ArrayList<String> queries = new ArrayList<>(numFunctions * 2);
        //mock functions and register them for caching
        for (int i = 0; i < numFunctions; i++) {
            functionNames.add("myFunction" + i);
            queries.add("func myFunction" + i);
            mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
            jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
        }

        CountDownLatch lock = null;
        Future<Void> metadataTask;
        try {
            //lock query 0
            lock = lockObject(getCacheKey(ConnectionName, queries.get(0)).toString().intern());
            // run the metadata cache task. This needs to be done in a separate thread because it will not return until function0 is unlocked
            metadataTask = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runMetaDataCacheTask();
                    return null;
                }
            });

            //TODO: is there a better way then waiting 2 seconds?
            Thread.sleep(2000);
            validateCached(queries.get(0), false);
            validateCached(queries.get(1));

            //mock some more functions and register them for caching
            for (int i = numFunctions; i < numFunctions * 2; i++) {
                functionNames.add("myFunction" + i);
                queries.add("func myFunction" + i);
                mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
                jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
            }

            // run the metadata task again, this time it should not attempt to process functions 0 because it is currently being processed and it should process function 1,2, and 3
            runMetaDataCacheTask();
            validateCached(queries.get(0), false);
            validateCached(queries.get(1));
            validateCached(queries.get(2));
            validateCached(queries.get(3));

        } finally {
            if (lock != null)
                //unlock function0
                lock.countDown();
        }

        //make sure the metadatacache task completes
        metadataTask.get();

        //function0 should now have been cached.
        validateCached(queries.get(0));
        //function 0 should have been processed once, function 1,2, and 3 should all have been processed twice.
        verifyNumberGetProcedureCalls((numFunctions * 3) - 1);
    }

    @Test(timeout = 10000)
    public void testMetadataRetrievalThreadPoolRunMetaDataCacheTaskWithLocksMultipleTimes2() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {
        int numFunctions = 2;
        ArrayList<String> functionNames = new ArrayList<>(numFunctions * 2);
        ArrayList<String> queries = new ArrayList<>(numFunctions * 2);
        //mock functions and register them for caching
        for (int i = 0; i < numFunctions; i++) {
            functionNames.add("myFunction" + i);
            queries.add("func myFunction" + i);
            mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
            jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
        }

        CountDownLatch lock = null;
        CountDownLatch lock2 = null;
        Future<Void> metadataTask;
        try {
            //lock function 0 and 1
            lock = lockObject(getCacheKey(ConnectionName, queries.get(0)).toString().intern());
            lock2 = lockObject(getCacheKey(ConnectionName, queries.get(1)).toString().intern());
            // run the metadata cache task. This needs to be done in a separate thread because it will not return until function0 and 1 are unlocked
            metadataTask = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runMetaDataCacheTask();
                    return null;
                }
            });

            //TODO: is there a better way then waiting 2 seconds?
            Thread.sleep(2000);
            validateCached(queries.get(0), false);
            validateCached(queries.get(1), false);

            //mock some more functions and register them for caching
            for (int i = numFunctions; i < numFunctions * 2; i++) {
                functionNames.add("myFunction" + i);
                queries.add("func myFunction" + i);
                mockFunction(functionNames.get(i), Collections.<Parameter>emptyList(), null);
                jdbcQueryingManager.registerQueryForPossibleCaching(ConnectionName, queries.get(i), null);
            }

            // run the metadata task again, this time it should not attempt to process functions 0 and 1 because it is currently being processed and it should process function 2 and 3
            runMetaDataCacheTask();
            validateCached(queries.get(0), false);
            validateCached(queries.get(1), false);
            validateCached(queries.get(2));
            validateCached(queries.get(3));

        } finally {
            //unlock function 0 and 1
            if (lock != null)
                lock.countDown();
            if (lock2 != null)
                lock2.countDown();
        }

        //make sure the metadatacache task completes
        metadataTask.get();

        //function0 and 1 should now have been cached.
        validateCached(queries.get(0));
        validateCached(queries.get(1));
        //function 0 and 1 should have been processed once, function 2 and 3 should all have been processed twice.
        verifyNumberGetProcedureCalls(numFunctions * 2);
    }

    @Test(timeout = 10000)
    public void testVoidFunctionWithLock() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException, ExecutionException {
        String functionName = "myFunction";
        final String query = "func myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);

        CountDownLatch lock = null;
        Future<Object> performQuery;
        try {
            //lock query
            lock = lockObject(getCacheKey(ConnectionName, query).toString().intern());

            // run perform query. This needs to be done in a separate thread because it will not return until the query is unlocked
            performQuery = executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
                }
            });
            Thread.sleep(2000);
            validateCached(query, false);

        } finally {
            //unlock the query
            if (lock != null)
                lock.countDown();
        }

        // the the perform query result
        Object rtn = performQuery.get();

        validateFunctionReturn(null, rtn);

        // validate the function has been cached
        validateCached(query);

        //Test cache retrieval
        try {
            //lock query
            lock = lockObject(getCacheKey(ConnectionName, query).toString().intern());
            //locking the function should not block the query from executing
            rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
            validateFunctionReturn(null, rtn);
        } finally {
            if (lock != null)
                lock.countDown();
        }

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        verifyNumberGetProcedureCalls(1);

    }

    /**
     * This will lock an object so any other threads will be forced to wait on it if they synchronize around it.
     *
     * @param obj The object to lock
     * @return A countdown latch. On the first countdown the object will become unlocked.
     */
    private CountDownLatch lockObject(final Object obj) {
        final CountDownLatch lock = new CountDownLatch(1);
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                synchronized (obj) {
                    lock.await();
                }
                return null;
            }
        });
        return lock;
    }

    /**
     * Calls the getCacheKey method in JdbcQueryingManagerImpl
     *
     * @param connectionName The connection name to pass to the method
     * @param query          The query to pass to the method.
     * @return The result of calling getCacheKey.
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object getCacheKey(String connectionName, String query) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = JdbcQueryingManagerImpl.class.getDeclaredMethod("getCacheKey", String.class, String.class, Option.class);
        m.setAccessible(true);
        return m.invoke(jdbcQueryingManager, connectionName, query, Option.<String>none());
    }

    /**
     * This will verify that getProcedures and getProcedureColumns was called the given number of times
     *
     * @param numberExpectedCalls The number of times that the database should have been asked for procedure info
     * @throws SQLException
     */
    private void verifyNumberGetProcedureCalls(int numberExpectedCalls) throws SQLException {
        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(numberExpectedCalls * 2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(numberExpectedCalls * 2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    /**
     * This will run the metadatacache task in in JdbcQueryingManagerImpl
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void runMetaDataCacheTask() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Class<?>[] innerClazzes = JdbcQueryingManagerImpl.class.getDeclaredClasses();
        Class<?> MetaDataCacheTaskTaskClazz = null;
        for (Class<?> innerClazz : innerClazzes) {
            if (innerClazz.getSimpleName().equals("MetaDataCacheTask")) {
                MetaDataCacheTaskTaskClazz = innerClazz;
                break;
            }
        }

        Assert.assertNotNull(MetaDataCacheTaskTaskClazz);
        Constructor<?> constructor = MetaDataCacheTaskTaskClazz.getDeclaredConstructor(JdbcQueryingManagerImpl.class, JdbcConnectionPoolManager.class);
        constructor.setAccessible(true);

        Object metaDataCacheTask = constructor.newInstance(jdbcQueryingManager, jdbcConnectionPoolManager);

        Method m = metaDataCacheTask.getClass().getDeclaredMethod("doRun");
        m.setAccessible(true);
        m.invoke(metaDataCacheTask);
    }

    /**
     * This will run the MetaDataCacheCleanUp task in in JdbcQueryingManagerImpl
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void runMetaDataCacheCleanUpTask() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Class<?>[] innerClazzes = JdbcQueryingManagerImpl.class.getDeclaredClasses();
        Class<?> metaDataCacheCleanUpTaskClazz = null;
        for (Class<?> innerClazz : innerClazzes) {
            if (innerClazz.getSimpleName().equals("MetaDataCacheCleanUpTask")) {
                metaDataCacheCleanUpTaskClazz = innerClazz;
                break;
            }
        }

        Assert.assertNotNull(metaDataCacheCleanUpTaskClazz);
        Constructor<?> constructor = metaDataCacheCleanUpTaskClazz.getDeclaredConstructor(JdbcQueryingManagerImpl.class);
        constructor.setAccessible(true);

        Object metaDataCacheCleanUpTask = constructor.newInstance(jdbcQueryingManager);

        Method m = metaDataCacheCleanUpTask.getClass().getDeclaredMethod("doRun");
        m.setAccessible(true);
        m.invoke(metaDataCacheCleanUpTask);
    }

    /**
     * This will validate that a function's return value is correct.
     *
     * @param expectedValue The expected return value
     * @param returnedValue The actual returned value
     */
    private void validateFunctionReturn(@Nullable Object expectedValue, Object returnedValue) {
        Assert.assertTrue(returnedValue instanceof List);
        if (expectedValue == null) {
            Assert.assertTrue(((List) returnedValue).isEmpty());
        } else {
            Assert.assertTrue(((List) returnedValue).size() == 1);
            Assert.assertTrue(((List) returnedValue).get(0) instanceof SqlRowSet);
            SqlRowSet result = (SqlRowSet) ((List) returnedValue).get(0);
            Assert.assertTrue(result.next());
            Assert.assertEquals(expectedValue, result.getObject(returnValueParameterName));
        }
    }

    /**
     * This will validate that a procedure's return values are correct.
     *
     * @param expectedValues The expected return values. This is a map because procedures can have multiple out parameters.
     * @param returnedValue  The actual returned value
     */
    private void validateProcedureReturn(@Nullable Map<String, Object> expectedValues, Object returnedValue) {
        Assert.assertTrue(returnedValue instanceof List);
        if (expectedValues == null) {
            Assert.assertTrue(((List) returnedValue).isEmpty());
        } else {
            Assert.assertTrue(((List) returnedValue).size() == 1);
            Assert.assertTrue(((List) returnedValue).get(0) instanceof SqlRowSet);
            SqlRowSet result = (SqlRowSet) ((List) returnedValue).get(0);
            Assert.assertTrue(result.next());
            for (String paramName : expectedValues.keySet()) {
                Assert.assertEquals(expectedValues.get(paramName), result.getObject(paramName));
            }
        }
    }

    /**
     * Checkes to make sure that a query has been cached.
     *
     * @param query the query to search for in the cache
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void validateCached(String query) throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        validateCached(query, true);
    }

    /**
     * Checkes to make sure that a query has been cached or not cached.
     *
     * @param query       the query to search for in the cache
     * @param checkCached if true it will check if the query has been cached. If false it will check to see if it has not been cached
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void validateCached(String query, boolean checkCached) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method m = JdbcQueryingManagerImpl.class.getDeclaredMethod("getCacheKey", String.class, String.class, Option.class);
        m.setAccessible(true);
        Object cacheKey = m.invoke(null, ConnectionName, query, new Option<String>(null));

        Field f = JdbcQueryingManagerImpl.class.getDeclaredField("simpleJdbcCallCache");
        f.setAccessible(true);
        Map metadataCache = (Map) f.get(jdbcQueryingManager);

        if (checkCached) {
            Assert.assertTrue("Query does not appear to have been cached: " + query, metadataCache.containsKey(cacheKey));
        } else {
            Assert.assertFalse("Query appear to have been cached: " + query, metadataCache.containsKey(cacheKey));
        }
    }

    /**
     * Fixes mocking metadata retrieval error. Call this after @mockMetadataError to undo it.
     *
     * @throws SQLException
     */
    private void fixMockMetadataError() throws SQLException {
        Mockito.doReturn(databaseMetaData).when(connection).getMetaData();
    }

    /**
     * Mocks a metadata retrieval error.
     *
     * @param errorMessage The error message that should be returned when attempting to retrieve the metadata
     * @throws SQLException
     */
    private void mockMetadataError(String errorMessage) throws SQLException {
        Mockito.doThrow(new SQLException(errorMessage)).when(connection).getMetaData();
    }

    /**
     * This will mock a function or procedure. It will make the getProcedures and getProcedureColumns return expected data.
     * Other methods that are needed to mock the function call are also mocked.
     *
     * @param procedureName The name of the function or procedure to mock
     * @param parameters    The list of function or procedure patameters
     * @param rtn           The map of results that the function or procedure should return
     * @throws SQLException
     */
    private void mockFunction(String procedureName, List<Parameter> parameters, @Nullable final Map<Integer, Object> rtn) throws SQLException {
        Mockito.doReturn(new MockResultSet(Collections.<Map<String, Object>>emptyList())).when(databaseMetaData).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.eq(procedureName));

        ArrayList<Map<String, Object>> parametersList = new ArrayList<>();
        for (Parameter parameter : parameters) {
            CollectionUtils.MapBuilder<String, Object> mapBuilder = CollectionUtils.mapBuilder();
            mapBuilder.put("COLUMN_NAME", parameter.name)
                    .put("COLUMN_TYPE", parameter.parameterType)
                    .put("DATA_TYPE", parameter.dataType)
                    .put("TYPE_NAME", parameter.typeName)
                    .put("NULLABLE", parameter.nullable);
            parametersList.add(mapBuilder.map());
        }

        Mockito.doReturn(new MockResultSet(parametersList)).when(databaseMetaData).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.eq(procedureName), Matchers.isNull(String.class));

        CallableStatement callableStatement = Mockito.mock(CallableStatement.class);

        Mockito.doReturn(false).when(callableStatement).execute();
        Mockito.doReturn(rtn == null ? 0 : 1).doReturn(-1).when(callableStatement).getUpdateCount();
        Mockito.doReturn(false).when(callableStatement).getMoreResults();
        if (rtn != null && !rtn.isEmpty()) {
            for (int index : rtn.keySet()) {
                Mockito.doReturn(rtn.get(index)).when(callableStatement).getObject(Matchers.eq(index));
            }
        }

        Mockito.doReturn(callableStatement).when(connection).prepareCall(Matchers.contains(procedureName));
    }

    /**
     * This represents a function of procedure parameter.
     */
    private class Parameter {
        private String name;
        private int parameterType;
        private int dataType;
        private String typeName;
        private boolean nullable;

        private Parameter(String name, int parameterType, int dataType, String typeName, boolean nullable) {
            this.name = name;
            this.parameterType = parameterType;
            this.dataType = dataType;
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
}
