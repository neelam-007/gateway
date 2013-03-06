package com.l7tech.server.jdbc;

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
    private CallableStatement callableStatement;

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

        Connection connection = Mockito.mock(Connection.class);

        databaseMetaData = Mockito.mock(DatabaseMetaData.class);
        //needs to be one of "MySQL","Microsoft SQL Server","Oracle","PostgreSQL" These are the databases that support functions.
        Mockito.doReturn("MySQL").when(databaseMetaData).getDatabaseProductName();
        Mockito.doReturn("MyDatabaseUsername").when(databaseMetaData).getUserName();

        Mockito.doReturn(databaseMetaData).when(connection).getMetaData();

        callableStatement = Mockito.mock(CallableStatement.class);

        Mockito.doReturn(connection).when(dataSource).getConnection();
        Mockito.doReturn(callableStatement).when(connection).prepareCall(Matchers.anyString());
    }

    @Test
    public void testVoidFunction() throws SQLException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String functionName = "myFunction";
        mockFunction(functionName, Collections.<Parameter>emptyList(), null);
        String query = "func myFunction";
        Object rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertTrue(rtn instanceof List);
        Assert.assertTrue(((List)rtn).isEmpty());

        validateFunctionCached(query);

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertTrue(rtn instanceof List);
        Assert.assertTrue(((List) rtn).isEmpty());

        //Test cache retrieval
        rtn = jdbcQueryingManager.performJdbcQuery(ConnectionName, query, null, 1, Collections.emptyList());
        Assert.assertTrue(rtn instanceof List);
        Assert.assertTrue(((List)rtn).isEmpty());

        //These actually get called twice each the first time it caches
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedures(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());
        Mockito.verify(databaseMetaData, Mockito.times(2)).getProcedureColumns(Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any(), Matchers.<String>any());

    }

    private void validateFunctionCached(String query) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method m = JdbcQueryingManagerImpl.class.getDeclaredMethod("getCacheKey", String.class, String.class, Option.class);
        m.setAccessible(true);
        Object cacheKey = m.invoke(null, ConnectionName, query, new Option<String>(null));

        Field f = JdbcQueryingManagerImpl.class.getDeclaredField("simpleJdbcCallCache");
        f.setAccessible(true);
        Map metadataCache = (Map) f.get(jdbcQueryingManager);

        Assert.assertTrue("Query does not appear to have been cached: " + query, metadataCache.containsKey(cacheKey));
    }

    private void mockFunction(String procedureName, List<Parameter> parameters, @Nullable Object rtn) throws SQLException {
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

        Mockito.doReturn(false).when(callableStatement).execute();
        Mockito.doReturn(rtn == null ? 0 : 1).doReturn(-1).when(callableStatement).getUpdateCount();
        Mockito.doReturn(false).when(callableStatement).getMoreResults();
    }

    /**
     * TODO:
     * Test that things get properly added to the cache.
     * cache gets invalidated when an error occurs??? (maybe on certain types of errors)
     * cache gets correctly expired
     * cache edge cases (similar names, cache values)
     * test functions and procedures
     */


    private class Parameter {
        private String name;
        private int parameterType;
        private int dataType;
        private String typeName;
        private boolean nullable;

        private Parameter() {
        }

        private Parameter(String name, int parameterType, int dataType, String typeName, boolean nullable) {
            this.name = name;
            this.parameterType = parameterType;
            this.dataType = dataType;
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
}
