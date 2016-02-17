package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.l7tech.external.assertions.apiportalintegration.IncrementPostBackAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
import com.l7tech.external.assertions.apiportalintegration.server.resource.PortalSyncPostbackJson;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.validation.constraints.AssertTrue;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author rchan, 2/05/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerIncrementPostBackAssertionTest {
    private ServerIncrementPostBackAssertion serverAssertion;
    private IncrementPostBackAssertion assertion;
    boolean debug=false;
    final long incrementStart = 1234L;
    final long incrementEnd = 1446503181299L;

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private JdbcQueryingManager jdbcQueryingManager;
    @Mock
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private DataSource dataSource;
    @Mock
    PolicyEnforcementContext context;
    @Mock
    TransactionStatus status;

    Map<String, Object> vars;

    final static String NODE_ID="theNodeId";

    @Before
    public void setup() throws Exception {
        String connectionName = "tenant-connection";
        when(applicationContext.getBean("jdbcQueryingManager", JdbcQueryingManager.class)).thenReturn(jdbcQueryingManager);
        when(applicationContext.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class)).thenReturn(jdbcConnectionPoolManager);
        when(jdbcConnectionPoolManager.getDataSource(connectionName)).thenReturn(dataSource);

        assertion = new IncrementPostBackAssertion();
        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext);
        vars = Maps.newHashMap();
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JDBC_CONNECTION, connectionName);
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_NODE_ID, NODE_ID);
        when(context.getVariableMap(any(String[].class), any(Audit.class))).thenReturn(vars);
        when(transactionManager.getTransaction(any(DefaultTransactionDefinition.class))).thenReturn(status);
    }

    @Test
    public void updateErrorEntitiesTest01() throws Exception {
        printStrForDebug("updateErrorEntitiesTest01- update error entities, no error ============================");
        final String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated = "errorEntityWillBeUpdated";
        final String errorEntityWillBeInserted = "errorEntityWillBeInserted";
        String errorEntityWillBeUpdatedLog = "log error for errorEntityWillBeUpdated";
        String errorEntityWillBeInsertedLog = "log error for errorEntityWillBeInserted";

        final List<String> calledEntityIds = Lists.newArrayList();


        final String nodeId = "jkhfhjdksfhsdja-nodeId";
        final List<Map<String, String>> errors = Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeInserted, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeInsertedLog),
                ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeUpdated, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeUpdatedLog));
        final String tableName = "table1";
        final String columnName = "column1";

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Integer>() {
                    @Override
                    public Integer answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", tableName))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeInserted)) {
                                calledEntityIds.add(((String) input.get(3)));
                                return Integer.valueOf(1);
                            } else fail("entity id should be updated: " + (String) input.get(3));
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", tableName))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeUpdated)) {
                                calledEntityIds.add(((String) input.get(3)));
                                return Integer.valueOf(1);
                            } else return Integer.valueOf(0);
                        }
                        return null;
                    }
                }
        );
        List<String> outputErrors = serverAssertion.updateErrorEntities(jdbcConnectionName, nodeId, errors, incrementEnd, tableName, columnName);
        assertTrue("output list should contain " + errorEntityWillBeInserted, outputErrors.contains(errorEntityWillBeInserted));
        assertTrue("output list should contain " + errorEntityWillBeUpdated, outputErrors.contains(errorEntityWillBeUpdated));
        assertTrue("performJdbcQuery should be called twice instead of " + calledEntityIds.size(), calledEntityIds.size() == 2);
        assertTrue("errorEntityWillBeInserted should be passed in as argument to performJdbcQuery.", calledEntityIds.contains(errorEntityWillBeInserted));
        assertTrue("errorEntityWillBeUpdated should be passed in as argument to performJdbcQuery.", outputErrors.contains(errorEntityWillBeUpdated));
    }
    @Test
    public void checkRequestTest01() throws IOException, PolicyAssertionException {
        //test if the assertion fails if the incrementStatus is invalid
        printStrForDebug("checkRequestTest01- invalid incrementStatus, Failed ============================");
        String jsonPayload="{\"incrementStatus\" : \"123\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"syncLog\" : \"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"\n" +
                "}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
    }



    @Test
    public void checkRequestTest02() throws SQLException, NamingException, IOException, PolicyAssertionException {
        //test if the assertion fails if the database is not found
        printStrForDebug("checkRequestTest02- jdbc expetion, connection not found, Failed ============================");
        when(jdbcConnectionPoolManager.getDataSource(anyString())).thenReturn(null);
        String jsonPayload="{\"incrementStatus\" : \"123\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"syncLog\" : \"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"\n" +
                "}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestTest03() throws IOException, PolicyAssertionException, JAXBException {
        //success
        printStrForDebug("checkRequestTest03- should be successful and commit the transaction ============================");

        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext, transactionManager);
        final List<String> successSqlCalls = Lists.newArrayList();
        final String APPLICATION_TENANT_GATEWAY_TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String TENANT_GATEWAY_TABLE_NAME = "TENANT_GATEWAY";
        final String UUID_COLUMN_NAME = "UUID";
        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated = "updateErrorEntity";
        final String errorEntityWillBeInserted = "insertErrorEntity";
        final String entity_uuid_column = "entity_uuid";
        final String uuid_column = "uuid";
        final String application_uuid_column = "application_uuid";
        final String sync_log="\"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"";
        final String jsonPayload="{\"incrementStatus\" : \"partial\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"entityErrors\": [\n" +
                "       { \"id\":\""+errorEntityWillBeUpdated+"\",\n"+
                "           \"msg\": \"error message 1\" },\n"+
                "       { \"id\":\""+errorEntityWillBeInserted+"\",\n"+
                "         \"msg\": \"error message 2\" }\n"+
                "       ],\n  \"syncLog\" : "+sync_log+"\n}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        final List<String> insertIds = Lists.newArrayList(errorEntityWillBeInserted , "insertdelete2", "insert1");
        final List<String> updateIds = Lists.newArrayList("update1",errorEntityWillBeUpdated, "updatedelete1");

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            if (insertIds.contains((String) input.get(3))) {
                                insertIds.remove((String) input.get(3));
                                return Integer.valueOf(1);
                            } else fail("entity id should be updated: " + (String) input.get(3));
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            input = input.subList(3, input.size());
                            if (updateIds.containsAll(input)) {
                                updateIds.removeAll(input);
                                return Integer.valueOf(1);
                            }else return Integer.valueOf(0);
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s SET %s=? , %s=? WHERE %s=?", TENANT_GATEWAY_TABLE_NAME, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME, UUID_COLUMN_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            assertEquals("node id is incorrect", NODE_ID, input.get(2));
                            return Integer.valueOf(1);
                        } else if (((String) invocation.getArguments()[2]).startsWith("SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?")) {
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(entity_uuid_column, Lists.<String>newArrayList("updatedelete1", "insertdelete2"));
                            return result;
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format(ServerIncrementalSyncCommon.SELECT_ENTITIES_SQL, "a."+uuid_column.toUpperCase()))) {
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(uuid_column, Lists.newArrayList("update1", "insert1"));
                            return result;
                        }else if (((String) invocation.getArguments()[2]).startsWith(String.format("SELECT  %s FROM APPLICATION_TENANT_GATEWAY WHERE TENANT_GATEWAY_UUID=? and APPLICATION_UUID IN (",
                                application_uuid_column.toUpperCase()))) {
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(application_uuid_column, Lists.newArrayList("update1","updatedelete1"));
                            return result;
                        }
                        return null;
                    }
                }
        );

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertTrue("updateId list should be empty:" + updateIds.toString(), updateIds.isEmpty());
        assertTrue("insertId list should be empty:"+insertIds.toString(), insertIds.isEmpty());
        verify(transactionManager).commit(status);
    }



    @Test
    public void checkRequestTest07() throws IOException, PolicyAssertionException, JAXBException {
        //success
        printStrForDebug("checkRequestTest07- should be successful and commit the transaction ============================");

        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext, transactionManager);
        final List<String> successSqlCalls = Lists.newArrayList();
        final String APPLICATION_TENANT_GATEWAY_TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String TENANT_GATEWAY_TABLE_NAME = "TENANT_GATEWAY";
        final String UUID_COLUMN_NAME = "UUID";
        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        final String entity_uuid_column = "entity_uuid";
        final String uuid_column = "uuid";
        final String application_uuid_column = "application_uuid";
        final String sync_log="\"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"";
        final String jsonPayload="{\"incrementStatus\" : \"ok\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"syncLog\" : "+sync_log+"\n}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            fail("should not insert anything to db");
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            fail("should not update anything in db");
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s SET %s=? , %s=? WHERE %s=?", TENANT_GATEWAY_TABLE_NAME, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME, UUID_COLUMN_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            assertEquals("node id is incorrect", NODE_ID, input.get(2));
                            return Integer.valueOf(1);
                        } else if (((String) invocation.getArguments()[2]).startsWith("SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?")) {
                            return null;
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format(ServerIncrementalSyncCommon.SELECT_ENTITIES_SQL, "a."+uuid_column.toUpperCase()))) {
                            return null;
                        }else if (((String) invocation.getArguments()[2]).startsWith(String.format("SELECT  %s FROM APPLICATION_TENANT_GATEWAY WHERE TENANT_GATEWAY_UUID=? and APPLICATION_UUID IN (",
                                application_uuid_column.toUpperCase()))) {
                            return null;
                        }
                        return null;
                    }
                }
        );


        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        verify(transactionManager).commit(status);
    }


    @Test
    public void checkRequestTest04() throws IOException, PolicyAssertionException, JAXBException {
        //test if jdbcQueryingManage returns error string
        printStrForDebug("checkRequestTest04- jdbcQueryingManage returns error string, failed ============================");
        String jsonPayload="{\"incrementStatus\" : \"ok\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"syncLog\" : \"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"\n" +
                "}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext, transactionManager);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        return "jdbc error";
                    }
                }
        );
        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestTest05() throws IOException, PolicyAssertionException, JAXBException {
        //test if jdbcQueryingManage returns 0 when updating tenant_gateway table
        printStrForDebug("checkRequestTest05- jdbcQueryingManage returns 0, failed ============================");
        String jsonPayload="{\"incrementStatus\" : \"ok\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"syncLog\" : \"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"\n" +
                "}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext, transactionManager);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        return Integer.valueOf(0);
                    }
                }
        );
        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
    }
    @Test
    public void checkRequestTest06() throws IOException, PolicyAssertionException, JAXBException {
        //test if transaction gets rolled back if there is  a partial failure
        printStrForDebug("checkRequestTest06-transaction gets rolled back if there is a partial failure, failed ============================");

        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext, transactionManager);
        final List<String> successSqlCalls = Lists.newArrayList();
        final String APPLICATION_TENANT_GATEWAY_TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String TENANT_GATEWAY_TABLE_NAME = "TENANT_GATEWAY";
        final String UUID_COLUMN_NAME = "UUID";
        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated = "updateErrorEntity";
        final String errorEntityWillBeInserted = "insertErrorEntity";
        final String entity_uuid_column = "entity_uuid";
        final String uuid_column = "uuid";
        final String application_uuid_column = "application_uuid";
        final String sync_log="\"{\\\"count\\\" : \\\"2\\\", \\\"cron\\\" : \\\"*/20 * * * * ?\\\"}\"";
        final String jsonPayload="{\"incrementStatus\" : \"partial\",\n" +
                "  \"incrementStart\" : 1234,\n" +
                "  \"incrementEnd\" : 2453502843060,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"entityErrors\": [\n" +
                "       { \"id\":\""+errorEntityWillBeUpdated+"\",\n"+
                "           \"msg\": \"error message 1\" },\n"+
                "       { \"id\":\""+errorEntityWillBeInserted+"\",\n"+
                "         \"msg\": \"error message 2\" }\n"+
                "       ],\n  \"syncLog\" : "+sync_log+"\n}";
        vars.put(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON, jsonPayload);
        final List<String> insertIds = Lists.newArrayList(errorEntityWillBeInserted , "insertdelete2", "insert1");
        final List<String> updateIds = Lists.newArrayList("update1",errorEntityWillBeUpdated, "updatedelete1");

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            return "jdbc error";
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            return "jdbc error";
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s SET %s=? , %s=? WHERE %s=?", TENANT_GATEWAY_TABLE_NAME, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME, UUID_COLUMN_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            assertEquals("node id is incorrect", NODE_ID, input.get(2));
                            return Integer.valueOf(1);
                        } else if (((String) invocation.getArguments()[2]).startsWith("SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?")) {
                            return "jdbc error";
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format(ServerIncrementalSyncCommon.SELECT_ENTITIES_SQL, "a."+uuid_column.toUpperCase()))) {
                            return "jdbc error";
                        }else if (((String) invocation.getArguments()[2]).startsWith(String.format("SELECT  %s FROM APPLICATION_TENANT_GATEWAY WHERE TENANT_GATEWAY_UUID=? and APPLICATION_UUID IN (",
                                application_uuid_column.toUpperCase()))) {
                            return "jdbc error";
                        }
                        return null;
                    }
                }
        );

        try {
            assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
        } catch (Exception e) {
            fail("should not throw Exception");
        }
        verify(transactionManager).rollback(status);
    }

    @Test
    public void handleApplicationSyncPostbackTest01() throws PolicyAssertionException, JAXBException {
        //test with status ok.
        // insert or update entity sync status
        // make sure the error messages are ignored
        printStrForDebug("handleApplicationSyncPostbackTest01-success and status is ok and error entities are ignored, success ============================");
        final List<String> successSqlCalls = Lists.newArrayList();
        final String APPLICATION_TENANT_GATEWAY_TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String TENANT_GATEWAY_TABLE_NAME = "TENANT_GATEWAY";
        final String UUID_COLUMN_NAME = "UUID";
        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        final String nodeId = "jkhfhjdksfhsdja-nodeId";
        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated = "updateErrorEntity";
        final String errorEntityWillBeInserted = "insertErrorEntity";
        final String entity_uuid_column = "entity_uuid";
        final String uuid_column = "uuid";
        final String application_uuid_column = "application_uuid";


        final PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
        postback.setIncrementEnd(235324L);
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_OK);
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_OK);

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeInserted)) {
                                fail("error message should not be inserted: " + (String) input.get(3));
                            } else if (((String) input.get(3)).equalsIgnoreCase("insertdelete2") ||
                                    ((String) input.get(3)).equalsIgnoreCase("insert1")) {
                                successSqlCalls.add((String) invocation.getArguments()[2]);
                                return Integer.valueOf(1);
                            } else fail("entity id should be updated: " + (String) input.get(3));
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeUpdated)){
                                fail("error message should not be updated: " + (String) input.get(3));
                            }else if (((String) input.get(3)).equalsIgnoreCase("update1") ||
                                    ((String) input.get(3)).equalsIgnoreCase("updatedelete1")) {
                                successSqlCalls.add((String) invocation.getArguments()[2]);
                                return Integer.valueOf(1);
                            }else return Integer.valueOf(0);
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s SET %s=? , %s=? WHERE %s=?", TENANT_GATEWAY_TABLE_NAME, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME, UUID_COLUMN_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            assertEquals("sync log message is incorrect", postback.getSyncLog(), input.get(1));
                            assertEquals("node id is incorrect", nodeId, input.get(2));
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            return Integer.valueOf(1);
                        } else if (((String) invocation.getArguments()[2]).startsWith("SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?")) {
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(entity_uuid_column, Lists.<String>newArrayList("updatedelete1", "insertdelete2"));
                            return result;
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format(ServerIncrementalSyncCommon.SELECT_ENTITIES_SQL, "a."+uuid_column.toUpperCase()))) {
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(uuid_column, Lists.newArrayList("update1", "insert1"));
                            return result;
                        }else if (((String) invocation.getArguments()[2]).startsWith(String.format("SELECT  %s FROM APPLICATION_TENANT_GATEWAY WHERE TENANT_GATEWAY_UUID=? and APPLICATION_UUID IN (",
                                application_uuid_column.toUpperCase()))) {
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            ImmutableMap<String, ArrayList<String>> result = ImmutableMap.of(application_uuid_column, Lists.newArrayList("update1","updatedelete1"));
                            return result;
                        }
                        return null;
                    }
                }
        );

        try {
            serverAssertion.handleApplicationSyncPostback(jdbcConnectionName, postback, nodeId);
        } catch (PolicyAssertionException e) {
            fail("should not throw PolicyAssertionException");
        }
        assertEquals("incrementStart should set to 0 for BulkSync", 0, postback.getIncrementStart());
    }
    private void printStrForDebug(String msg){
        if (debug) System.out.println(msg);
    }
}
