package com.l7tech.server.jdbc;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.util.Option;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.Nullable;
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

    private void verifyNumberGetProcedureCalls(int numberExpectedCalls) throws SQLException {
        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(numberExpectedCalls * 2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(numberExpectedCalls * 2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

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

    private void validateCached(String query) throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        validateCached(query, true);
    }

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

    private void fixMockMetadataError() throws SQLException {
        Mockito.doReturn(databaseMetaData).when(connection).getMetaData();
    }

    private void mockMetadataError(String errorMessage) throws SQLException {
        Mockito.doThrow(new SQLException(errorMessage)).when(connection).getMetaData();
    }

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
