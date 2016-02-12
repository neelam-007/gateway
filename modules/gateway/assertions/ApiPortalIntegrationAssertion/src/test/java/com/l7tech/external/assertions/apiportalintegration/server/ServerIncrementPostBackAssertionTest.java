package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.l7tech.external.assertions.apiportalintegration.IncrementPostBackAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.PortalSyncPostbackJson;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.ExpectedException;

import javax.sql.DataSource;
import java.io.IOException;
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

    final long incrementStart = 1234L;
    final long incrementEnd = 1446503181299L;

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private JdbcQueryingManager jdbcQueryingManager;

    @Mock
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    @Mock
    private DataSource dataSource;

    @Before
    public void setup() throws Exception {
        when(applicationContext.getBean("jdbcQueryingManager", JdbcQueryingManager.class)).thenReturn(jdbcQueryingManager);
        when(applicationContext.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class)).thenReturn(jdbcConnectionPoolManager);
        assertion = new IncrementPostBackAssertion();
        serverAssertion = new ServerIncrementPostBackAssertion(assertion, applicationContext);
    }

    @Test
    public void testUpdateErrorEntities() throws Exception {

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
    public void validatePostbackTest01() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_OK);
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log"))
        );
        postback.validate();
    }


    @Test(expected = IOException.class)
    public void validatePostbackTest02() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_ERROR);
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log"))
        );

        postback.validate();
        fail("Should throw IOException");

    }

    @Test(expected = IOException.class)
    public void validatePostbackTest03() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_PARTIAL);

        postback.validate();
        fail("Should throw IOException");

    }

    @Ignore
    //@Test
    public void handleApplicationSyncPostbackTest01() {

        final List<String> successSqlCalls = Lists.newArrayList();
        final String APPLICATION_TENANT_GATEWAY_TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String TENANT_GATEWAY_TABLE_NAME = "TENANT_GATEWAY";
        final String UUID_COLUMN_NAME = "UUID";
        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        final String nodeId = "jkhfhjdksfhsdja-nodeId";
        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated = "errorEntityWillBeUpdated";
        final String errorEntityWillBeInserted = "errorEntityWillBeInserted";
        final String columeName = "entity_uuid";

        final PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_OK);


        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Integer>() {
                    @Override
                    public Integer answer(final InvocationOnMock invocation) throws Throwable {
                        if (((String) invocation.getArguments()[2]).startsWith(String.format("INSERT INTO %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeInserted)) {
                                successSqlCalls.add((String) invocation.getArguments()[2]);
                                return Integer.valueOf(1);
                            } else fail("entity id should be updated: " + (String) input.get(3));
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s", APPLICATION_TENANT_GATEWAY_TABLE_NAME))) {
                            List input = (List) invocation.getArguments()[6];
                            if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeUpdated)) {
                                successSqlCalls.add((String) invocation.getArguments()[2]);
                                return Integer.valueOf(1);
                            } else return Integer.valueOf(0);
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("UPDATE %s SET %s=? , %s=? WHERE %s=?", TENANT_GATEWAY_TABLE_NAME, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME, UUID_COLUMN_NAME))) {
                            List input = (List) invocation.getArguments()[2];
                            assertEquals("sync log message is incorrect", postback.getSyncLog(), input.get(1));
                            assertEquals("node id is incorrect", nodeId, input.get(2));
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            return Integer.valueOf(1);
                        } else if (((String) invocation.getArguments()[2]).startsWith(String.format("SELECT %s FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?", columeName.toUpperCase()))) {
                            successSqlCalls.add((String) invocation.getArguments()[2]);
                            return null;
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
        assertTrue("performJdbcQuery should be called twice instead of " + successSqlCalls.size(), successSqlCalls.size() == 2);

    }
}
