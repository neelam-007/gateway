package com.l7tech.server.jdbc;

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
    protected final String ConnectionName = "OracleConnection";
    private JdbcQueryingManager jdbcQueryingManager;
    private Map<String, String> configProperties = new HashMap<>();
    private MockConfig mockConfig = new MockConfig(configProperties);
    private DatabaseMetaData databaseMetaData;
    private String returnValueParameterName = "RETURN_VALUE";
    private Connection connection;

    /**
     * Sets up mocks and jdbc objects
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        DataSource dataSource = Mockito.mock(DataSource.class);

        JdbcConnectionManagerImpl jdbcConnectionManager = Mockito.spy(new JdbcConnectionManagerImpl());

        Mockito.doReturn(Collections.emptyList()).when(jdbcConnectionManager).findAll();

        JdbcConnectionPoolManager jdbcConnectionPoolManager = Mockito.spy(new JdbcConnectionPoolManager(jdbcConnectionManager));

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

        validateFunctionCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());

    }

    @Test
    public void testStringReturnVoidFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true)), Arrays.<Object>asList(myReturnValue));
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        validateFunctionCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(myReturnValue, rtn);

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    @Test
    public void testStringReturnSingleParameterFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction ?";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue));
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        validateFunctionCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1"));
        validateFunctionReturn(myReturnValue, rtn);

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    @Test
    public void testStringReturnMultiParameterFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        String myReturnValue = "myReturnValue";
        String query = "func myFunction(?, ?)";
        mockFunction(functionName, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue));
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        validateFunctionCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue, rtn);

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    @Test
    public void testCallingMultipleFunctionsFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        String functionName1 = "myFunction1";
        String query1 = "func myFunction1(?, ?)";
        String myReturnValue1 = "myReturnValue1";
        mockFunction(functionName1, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue1));
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        String functionName2 = "myFunction2";
        String query2 = "func myFunction2(?, ?)";
        String myReturnValue2 = "myReturnValue2";
        mockFunction(functionName2, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue2));
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        validateFunctionCached(query1);
        validateFunctionCached(query2);

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

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(4)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(4)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    @Test
    public void testCallingMultipleFunctionsFunctionNoCache() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        configProperties.put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED, "false");

        String functionName1 = "myFunction1";
        String query1 = "func myFunction1(?, ?)";
        String myReturnValue1 = "myReturnValue1";
        mockFunction(functionName1, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue1));
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query1, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue1, rtn);

        String functionName2 = "myFunction2";
        String query2 = "func myFunction2(?, ?)";
        String myReturnValue2 = "myReturnValue2";
        mockFunction(functionName2, Arrays.asList(new Parameter(returnValueParameterName, DatabaseMetaData.procedureColumnReturn, 12, "VARCHAR", true), new Parameter("ParamIn1", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false), new Parameter("ParamIn2", DatabaseMetaData.procedureColumnIn, 12, "VARCHAR", false)), Arrays.<Object>asList(myReturnValue2));
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query2, null, 1, Arrays.<Object>asList("param1", "param2"));
        validateFunctionReturn(myReturnValue2, rtn);

        validateFunctionCached(query1, false);
        validateFunctionCached(query2, false);

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

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(12)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(12)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
    }

    @Test
    public void testErrorGettingFunctionMetadata() throws SQLException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        String errorString = "My Test Error";
        String query = "func myFunction";
        mockMetadataError(errorString);
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertEquals(errorString, rtn);

        validateFunctionCached(query);

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

        validateFunctionCached(query);

        String errorString = "My Test Error";
        mockMetadataError(errorString);
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        validateFunctionCached(query);

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        fixMockMetadataError();

        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        validateFunctionReturn(null, rtn);

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());

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

    private void validateFunctionCached(String query) throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        validateFunctionCached(query, true);
    }

    private void validateFunctionCached(String query, boolean checkCached) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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

    private void mockFunction(String procedureName, List<Parameter> parameters, @Nullable List<Object> rtn) throws SQLException {
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
        if (rtn != null) {
            for (int i = 0; i < rtn.size(); i++) {
                Mockito.doReturn(rtn.get(i)).when(callableStatement).getObject(i + 1);
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
