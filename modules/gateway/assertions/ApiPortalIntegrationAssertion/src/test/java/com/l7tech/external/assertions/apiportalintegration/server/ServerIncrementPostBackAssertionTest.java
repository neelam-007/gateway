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

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated="errorEntityWillBeUpdated";
        final String errorEntityWillBeInserted = "errorEntityWillBeInserted";
        String errorEntityWillBeUpdatedLog="log error for errorEntityWillBeUpdated";
        String errorEntityWillBeInsertedLog = "log error for errorEntityWillBeInserted";

        final List<String> calledEntityIds=Lists.newArrayList();


        String nodeId = "jkhfhjdksfhsdja-nodeId";
        List<Map<String, String>> errors = Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeInserted, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeInsertedLog),
                ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeUpdated, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeUpdatedLog));
        String tableName = "table1";
        String columnName = "column1";

        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Integer>() {
                    @Override
                    public Integer answer(final InvocationOnMock invocation) throws Throwable {
                        List input = (List) invocation.getArguments()[6];
                        if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeInserted)) {
                            if (((String)invocation.getArguments()[2]).startsWith("INSERT INTO")){
                                calledEntityIds.add(((String) input.get(3)));
                                return Integer.valueOf(1);
                            }else{
                                return Integer.valueOf(0);
                            }
                        }else if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeUpdated)) {
                            calledEntityIds.add(((String) input.get(3)));
                            assertTrue(((String)invocation.getArguments()[2]).startsWith("UPDATE") );
                            return Integer.valueOf(1);
                        }else fail("entity id does not match. Id:" + (String) input.get(3));
                        return null;
                    }
                }
        );
        List<String> outputErrors = serverAssertion.updateErrorEntities(jdbcConnectionName, nodeId, errors, incrementEnd, tableName, columnName);
        assertTrue("output list should contain "+errorEntityWillBeInserted, outputErrors.contains(errorEntityWillBeInserted));
        assertTrue("output list should contain "+errorEntityWillBeUpdated, outputErrors.contains(errorEntityWillBeUpdated));
        assertTrue("performJdbcQuery should be called twice instead of "+calledEntityIds.size(), calledEntityIds.size()==2);
        assertTrue("errorEntityWillBeInserted should be passed in as argument to performJdbcQuery.", calledEntityIds.contains(errorEntityWillBeInserted));
        assertTrue("errorEntityWillBeUpdated should be passed in as argument to performJdbcQuery.", outputErrors.contains(errorEntityWillBeUpdated));
    }

    @Test
    public void validatePostbackTest01(){
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType("APPLICATION");
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus("ok");
        postback.setEntityErrors(
        Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log")));
        try {
            serverAssertion.validatePostback(postback);
        } catch (IOException e) {
            fail("Should not throw IOException");
        } catch (PolicyAssertionException e) {
            fail("Should not throw PolicyAssertionException");
        }
    }

    @Test
    public void validatePostbackTest02(){
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType("APPLICATION");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus("error");
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log")));
        try {
            serverAssertion.validatePostback(postback);
            fail("Should throw PolicyAssertionException");
        } catch (IOException e) {
            fail("Should not throw IOException");
        } catch (PolicyAssertionException e) {

        }
    }

    @Test
    public void validatePostbackTest03(){
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType("APPLICATION");
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus("partial");
        try {
            serverAssertion.validatePostback(postback);
            fail("Should throw PolicyAssertionException exception");
        } catch (IOException e) {
            fail("Should throw PolicyAssertionException");
        } catch (PolicyAssertionException e) {

        }
    }

    @Ignore
    //@Test
    public void handleApplicationSyncPostbackTest01(){
        String nodeId = "jkhfhjdksfhsdja-nodeId";
        String jdbcConnectionName = "tenant_connection1";
        final String errorEntityWillBeUpdated="errorEntityWillBeUpdated";
        final String errorEntityWillBeInserted = "errorEntityWillBeInserted";
        String errorEntityWillBeUpdatedLog="log error for errorEntityWillBeUpdated";
        String errorEntityWillBeInsertedLog = "log error for errorEntityWillBeInserted";

        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType("APPLICATION");
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus("ok");
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeUpdated, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeUpdatedLog),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, errorEntityWillBeInserted, ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, errorEntityWillBeInsertedLog)));
        when(jdbcQueryingManager.performJdbcQuery(anyString(), any(DataSource.class), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenAnswer(
                new Answer<Integer>() {
                    @Override
                    public Integer answer(final InvocationOnMock invocation) throws Throwable {
                        List input = (List) invocation.getArguments()[6];
                        if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeInserted)) {
                            if (((String)invocation.getArguments()[2]).startsWith("INSERT INTO APPLICATION_TENANT_GATEWAY")){
                                return Integer.valueOf(1);
                            }else{
                                return Integer.valueOf(0);
                            }
                        }else if (((String) input.get(3)).equalsIgnoreCase(errorEntityWillBeUpdated)) {
                            assertTrue(((String)invocation.getArguments()[2]).startsWith("UPDATE APPLICATION_TENANT_GATEWAY") );
                            return Integer.valueOf(1);
                        }else fail("entity id does not match.  Id:" + (String) input.get(3));
                        return null;
                    }
                }
        );

        try {
            serverAssertion.handleApplicationSyncPostback(jdbcConnectionName, postback, nodeId);
        } catch (PolicyAssertionException e) {
            fail("should not throw PolicyAssertionException");
        }
    }
}
