package com.l7tech.skunkworks.jdbc;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.test.BugId;
import com.l7tech.util.Config;
import org.junit.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tests the server jdbc query assertion.
 *
 * @author Victor Kazakov
 */
public class ServerJdbcQueryAssertionIntegrationTests extends JdbcCallHelperIntegrationAbstractBaseTestClass {

    private static ApplicationContext context;

    private static PolicyEnforcementContext policyEnforcementContext;
    private static Map<String, Object> contextVariables = new HashMap<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(200);

    /**
     * Sets up the mock objects
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        JdbcCallHelperIntegrationAbstractBaseTestClass.beforeClass(null);

        context = Mockito.mock(ApplicationContext.class);

        Mockito.doReturn(getJdbcQueryingManager()).when(context).getBean(Matchers.eq("jdbcQueryingManager"), Matchers.eq(JdbcQueryingManager.class));
        Mockito.doReturn(getMockConfig()).when(context).getBean(Matchers.eq("serverConfig"), Matchers.eq(Config.class));

        policyEnforcementContext = Mockito.mock(PolicyEnforcementContext.class);
        Mockito.doAnswer(new Answer<Map<String, Object>>() {
            @Override
            public Map<String, Object> answer(InvocationOnMock invocationOnMock) throws Throwable {
                String[] names = (String[]) invocationOnMock.getArguments()[0];
                Map<String, Object> vars = new HashMap<>(names.length);
                for (String name : names) {
                    vars.put(name, contextVariables.get(name));
                }
                return vars;
            }
        }).when(policyEnforcementContext).getVariableMap(Matchers.any(String[].class), Matchers.any(Audit.class));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                String name = (String) invocationOnMock.getArguments()[0];
                Object value = invocationOnMock.getArguments()[1];
                contextVariables.put(name.toLowerCase(), value);
                return null;
            }
        }).when(policyEnforcementContext).setVariable(Matchers.any(String.class), Matchers.any());
    }

    /**
     * Clears config and contexts variables before each test.
     */
    @Before
    public void before() {
        getConfigProperties().clear();
        contextVariables.clear();

        getConfigProperties().put(ServerConfigParams.PARAM_JDBC_QUERY_CACHE_METADATA_ENABLED, "false");
    }

    /**
     * Tests executing a simple query.
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void test() throws PolicyAssertionException, IOException {
        JdbcQueryAssertion assertion = createJdbcQueryAssertion();
        assertion.setSqlQuery("select 123 from dual");
        ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

        AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test(timeout = 60 * 1000)
    public void testWithLoop() throws PolicyAssertionException, IOException, InterruptedException, ExecutionException {
        final int threads = 200;
        ArrayList<Future<Void>> futures = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(new Callable<Void>() {
                public Void call() throws PolicyAssertionException, IOException {
                    JdbcQueryAssertion assertion = createJdbcQueryAssertion();
                    assertion.setSqlQuery("select 123" + index + " as returnvalue from dual");
                    String variablePrefix = "jdbcquery" + index;
                    assertion.setVariablePrefix(variablePrefix);
                    ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

                    AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

                    Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

                    Assert.assertEquals("123" + index, String.valueOf(((Object[]) contextVariables.get(variablePrefix + ".returnvalue"))[0]));
                    return null;
                }
            }));
        }

        for (int i = 0; i < threads; i++) {
            futures.get(i).get();
        }
    }

    /**
     * Tests retrieving a timestamp for a select query.
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void testTimeStamp() throws PolicyAssertionException, IOException {
        JdbcQueryAssertion assertion = createJdbcQueryAssertion();
        assertion.setSqlQuery("SELECT TO_TIMESTAMP('24-March-13 12:10:10.987000', 'DD-Mon-RR HH24:MI:SS.FF') as myTimestamp FROM DUAL");
        ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

        AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

        String expandedTimestamp = ExpandVariables.process("${jdbcQuery.myTimestamp}", contextVariables, new TestAudit());

        Assert.assertEquals("2013-03-24T19:10:10.987Z", expandedTimestamp);
    }

    /**
     * Tests passing a timestamp to a procedure
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void testTimeStampPassToProcedure() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateGetYEarFromTimestampProcedure);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("time", new Date());
            assertion.setSqlQuery("CALL " + CreateGetYEarFromTimestampProcedureName + " ${time}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals("2013", ((Object[]) contextVariables.get("jdbcquery.yearout"))[0]);
        } finally {
            createDropItem(DropGetYEarFromTimestampProcedure);
        }
    }

    /**
     * Tests retrieving a timestamp from a procedure
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void testRetrieveTimeStampFromProcedure() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateGetCurrentTimestampProcedure);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("time", new Date());
            assertion.setSqlQuery("CALL " + CreateGetCurrentTimestampProcedureName);
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.currenttime"))[0].getClass());

            Timestamp currentServerTime = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.currenttime"))[0];

            //assert that the times are within a minute of eachother.
            Assert.assertTrue(Math.abs(new Date().getTime() - currentServerTime.getTime()) < 1 * 60 * 1000);

        } finally {
            createDropItem(DropGetCurrentTimestampProcedure);
        }
    }

    /**
     * Tests retrieving a timestamp from a function
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void testRetrieveTimeStampFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateGetCurrentTimestampFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("time", new Date());
            assertion.setSqlQuery("FUNC " + CreateGetCurrentTimestampFunctionName + "()");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            Timestamp currentServerTime = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //assert that the times are within a minute of eachother.
            Assert.assertTrue(Math.abs(new Date().getTime() - currentServerTime.getTime()) < 1 * 60 * 1000);

        } finally {
            createDropItem(DropGetCurrentTimestampFunction);
        }
    }

    /**
     * Tests retrieving a string from a function
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    public void testRetrieveStringFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateGetStringFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("time", new Date());
            assertion.setSqlQuery("func GetStringFunc(3)");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("123abc", returnString);

        } finally {
            createDropItem(DropGetStringFunction);
        }
    }

    /**
     * Oracle does not support passing boolean parameters to stored procedures/functions: http://docs.oracle.com/cd/B19306_01/java.102/b14355/apxtblsh.htm#i1005380
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveBooleanFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBooleanFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("boolean_in", true);
            assertion.setSqlQuery("func " + SendRetrieveBooleanFunctionName + " ${boolean_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("false", returnString);

        } finally {
            createDropItem(DropSendRetrieveBooleanFunction);
        }
    }

    /**
     * Oracle does not support passing boolean parameters to stored procedures/functions: http://docs.oracle.com/cd/B19306_01/java.102/b14355/apxtblsh.htm#i1005380
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Ignore
    @Test
    public void testRetrieveBooleanFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateRetrieveBooleanFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("int_in", 1);
            assertion.setSqlQuery("func " + RetrieveBooleanFunctionName + " ${int_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("true", returnString);

        } finally {
            createDropItem(DropRetrieveBooleanFunction);
        }
    }

    @Test
    public void testSendRetrieveVarchar2FromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveVarchar2Function);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveVarchar2FunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveVarchar2Function);
        }
    }

    @Test
    public void testSendRetrieveVarcharFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveVarcharFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveVarcharFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveVarcharFunction);
        }
    }

    @Test
    public void testSendRetrieveNVarchar2FromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNVarchar2Function);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveNVarchar2FunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveNVarchar2Function);
        }
    }

    @Test
    public void testSendRetrieveCharFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveCharFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveCharFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveCharFunction);
        }
    }

    @Test
    public void testSendRetrieveNCharFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNCharFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveNCharFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveNCharFunction);
        }
    }

    @Test
    public void testSendRetrieveNumberFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNumberFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            double varIn = 126312.15;
            contextVariables.put("var_in", varIn);
            assertion.setSqlQuery("func " + SendRetrieveNumberFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(BigDecimal.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            BigDecimal returnValue = (BigDecimal) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(new BigDecimal(varIn * 2).setScale(1, RoundingMode.HALF_UP), returnValue);

        } finally {
            createDropItem(DropSendRetrieveNumberFunction);
        }
    }

    @Test
    public void testSendRetrieveNumberFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNumberFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            double varIn = 126312.15;
            contextVariables.put("var_in", String.valueOf(varIn));
            assertion.setSqlQuery("func " + SendRetrieveNumberFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(BigDecimal.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            BigDecimal returnValue = (BigDecimal) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(new BigDecimal(varIn * 2).setScale(1, RoundingMode.HALF_UP), returnValue);

        } finally {
            createDropItem(DropSendRetrieveNumberFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryFloatFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryFloatFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            float varIn = 126387.123123f;
            contextVariables.put("var_in", varIn);
            assertion.setSqlQuery("func " + SendRetrieveBinaryFloatFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("2.5277425E+005", returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryFloatFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryFloatFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryFloatFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            float varIn = 126387.123123f;
            contextVariables.put("var_in", String.valueOf(varIn));
            assertion.setSqlQuery("func " + SendRetrieveBinaryFloatFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("2.5277425E+005", returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryFloatFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryDoubleFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryDoubleFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            double varIn = 126312.15;
            contextVariables.put("var_in", varIn);
            assertion.setSqlQuery("func " + SendRetrieveBinaryDoubleFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("2.5262429999999999E+005", returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryDoubleFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryDoubleFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryDoubleFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            double varIn = 126312.15;
            contextVariables.put("var_in", String.valueOf(varIn));
            assertion.setSqlQuery("func " + SendRetrieveBinaryDoubleFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("2.5262429999999999E+005", returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryDoubleFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryIntegerFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryIntegerFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            int varIn = -98782346;
            contextVariables.put("var_in", varIn);
            assertion.setSqlQuery("func " + SendRetrieveBinaryIntegerFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(String.valueOf(varIn * 2), returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryIntegerFunction);
        }
    }

    @Test
    public void testSendRetrieveBinaryIntegerFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBinaryIntegerFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            int varIn = -98782346;
            contextVariables.put("var_in", String.valueOf(varIn));
            assertion.setSqlQuery("func " + SendRetrieveBinaryIntegerFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(String.valueOf(varIn * 2), returnValue);

        } finally {
            createDropItem(DropSendRetrieveBinaryIntegerFunction);
        }
    }

    @Test
    public void testSendRetrievePlsIntegerFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrievePlsIntegerFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            int varIn = -98782346;
            contextVariables.put("var_in", varIn);
            assertion.setSqlQuery("func " + SendRetrievePlsIntegerFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(String.valueOf(varIn * 2), returnValue);

        } finally {
            createDropItem(DropSendRetrievePlsIntegerFunction);
        }
    }

    @Test
    public void testSendRetrievePlsIntegerFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrievePlsIntegerFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            int varIn = -98782346;
            contextVariables.put("var_in", String.valueOf(varIn));
            assertion.setSqlQuery("func " + SendRetrievePlsIntegerFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnValue = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(String.valueOf(varIn * 2), returnValue);

        } finally {
            createDropItem(DropSendRetrievePlsIntegerFunction);
        }
    }

    @Test
    public void testSendRetrieveLongFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveLongFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveLongFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveLongFunction);
        }
    }

    @Test
    public void testSendRetrieveDateFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveDateFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            @SuppressWarnings("deprecation") Date date = new Date(2013 - 1900, 2, 23);
            contextVariables.put("var_in", date);
            assertion.setSqlQuery("func " + SendRetrieveDateFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            Timestamp returnString = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(new Date(2013 - 1900, 2, 24), returnString);

        } finally {
            createDropItem(DropSendRetrieveDateFunction);
        }
    }

    @Test
    public void testSendRetrieveDateFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveDateFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "2013-02-23");
            assertion.setSqlQuery("func " + SendRetrieveDateFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            Timestamp returnString = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(new Timestamp(2013 - 1900, 1, 24, 0, 0, 0, 0), returnString);

        } finally {
            createDropItem(DropSendRetrieveDateFunction);
        }
    }

    @Test
    public void testSendRetrieveTimeStampFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveTimeStampFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            @SuppressWarnings("deprecation") Date date = new Date(2013 - 1900, 2, 23);
            contextVariables.put("var_in", date);
            assertion.setSqlQuery("func " + SendRetrieveTimeStampFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            Timestamp returnString = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(new Date(2013 - 1900, 2, 24), returnString);

        } finally {
            createDropItem(DropSendRetrieveTimeStampFunction);
        }
    }

    @Test
    public void testSendRetrieveTimeStampFromFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveTimeStampFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "2013-02-23 12:10:10.987000");
            assertion.setSqlQuery("func " + SendRetrieveTimeStampFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(Timestamp.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            Timestamp returnString = (Timestamp) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(new Timestamp(2013 - 1900, 1, 24, 12, 10, 10, 0), returnString);
        } finally {
            createDropItem(DropSendRetrieveTimeStampFunction);
        }
    }

    @Test
    public void testSendRetrieveRawFromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveRawFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc".getBytes());
            assertion.setSqlQuery("func " + SendRetrieveRawFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals("abc".getBytes(), returnString);

        } finally {
            createDropItem(DropSendRetrieveRawFunction);
        }
    }

    @Test
    public void testSendRetrieveCLOBFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveCLOBFunction);
        }
    }

    @Test
    public void testSendRetrieveCLOBFunctionLargeClob() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            String stringIn = randomString(128 * 1024);
            contextVariables.put("var_in", stringIn);
            assertion.setSqlQuery("func " + SendRetrieveCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(stringIn + stringIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveCLOBFunction);
        }
    }

    @Test
    public void testSendRetrieveCLOBFunctionLargeClob5MB() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            String stringIn = randomString(5 * 1024 * 1024);
            contextVariables.put("var_in", stringIn);
            assertion.setSqlQuery("func " + SendRetrieveCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertEquals(stringIn + stringIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveCLOBFunction);
        }
    }

    private static final char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private String randomString(int size) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    public void testSendRetrieveNCLOBFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveNCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveNCLOBFunction);
        }
    }

    @Test
    public void testSendRetrieveNCLOBFunctionSmallNClob() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            String stringIn = randomString(1024);
            contextVariables.put("var_in", stringIn);
            assertion.setSqlQuery("func " + SendRetrieveNCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(stringIn + stringIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveNCLOBFunction);
        }
    }

    /**
     * Large nclobs don't appear to work
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveNCLOBFunctionLargeNClob() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            String stringIn = randomString(128 * 1024);
            contextVariables.put("var_in", stringIn);
            assertion.setSqlQuery("func " + SendRetrieveNCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(stringIn + stringIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveNCLOBFunction);
        }
    }

    /**
     * Large nclobs don't appear to work
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveNCLOBFunctionLargeNClob5Mb() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveNCLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            String stringIn = randomString(5 * 1024 * 1024);
            contextVariables.put("var_in", stringIn);
            assertion.setSqlQuery("func " + SendRetrieveNCLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            String returnString = (String) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            //noinspection deprecation
            Assert.assertEquals(stringIn + stringIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveNCLOBFunction);
        }
    }

    @Test
    public void testSendRetrieveBLOBFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc".getBytes());
            assertion.setSqlQuery("func " + SendRetrieveBLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals("abc".getBytes(), returnString);

        } finally {
            createDropItem(DropSendRetrieveBLOBFunction);
        }
    }

    /**
     * Cannot pass a string to a function that takes in a blob
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveBLOBFunctionPassString() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveBLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals("abc".getBytes(), returnString);

        } finally {
            createDropItem(DropSendRetrieveBLOBFunction);
        }
    }

    @Test
    public void testSendRetrieveBLOBFunctionSmallBlob() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            byte[] bytesIn = new byte[24 * 1024];
            new Random().nextBytes(bytesIn);
            contextVariables.put("var_in", bytesIn);
            assertion.setSqlQuery("func " + SendRetrieveBLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals(bytesIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveBLOBFunction);
        }
    }

    /**
     * Oracle is not able to return blobs greater then 32kB
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveBLOBFunctionLargeBlob() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            byte[] bytesIn = new byte[128 * 1024];
            new Random().nextBytes(bytesIn);
            contextVariables.put("var_in", bytesIn);
            assertion.setSqlQuery("func " + SendRetrieveBLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals(bytesIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveBLOBFunction);
        }
    }

    /**
     * Oracle is not able to return blobs greater then 32kB
     *
     * @throws PolicyAssertionException
     * @throws IOException
     */
    @Test
    @Ignore
    public void testSendRetrieveBLOBFunctionLargeBlob5MB() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveBLOBFunction);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            byte[] bytesIn = new byte[5 * 1024 * 1024];
            new Random().nextBytes(bytesIn);
            contextVariables.put("var_in", bytesIn);
            assertion.setSqlQuery("func " + SendRetrieveBLOBFunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(byte[].class, ((Object[]) contextVariables.get("jdbcquery.return_value"))[0].getClass());

            byte[] returnString = (byte[]) ((Object[]) contextVariables.get("jdbcquery.return_value"))[0];

            Assert.assertArrayEquals(bytesIn, returnString);

        } finally {
            createDropItem(DropSendRetrieveBLOBFunction);
        }
    }

    @Test
    @BugId("SSG-6687")
    public void testIncorrectSchemaFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(CreateSendRetrieveVarchar2Function);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            contextVariables.put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveVarchar2FunctionName + " ${var_in}");
            assertion.setSchema("qatest2");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, context);

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(policyEnforcementContext);

            Assert.assertEquals(AssertionStatus.FAILED, assertionStatus);

        } finally {
            createDropItem(DropSendRetrieveVarchar2Function);
        }
    }

    private static JdbcQueryAssertion createJdbcQueryAssertion() {
        JdbcQueryAssertion jdbcQueryAssertion = new JdbcQueryAssertion();
        jdbcQueryAssertion.setConnectionName(ConnectionName);
        jdbcQueryAssertion.setVariablePrefix("jdbcQuery");
        return jdbcQueryAssertion;
    }

    private static final String SendRetrieveBLOBFunctionName = "SendRetrieveBLOB";
    private static final String CreateSendRetrieveBLOBFunction =
            "create or replace\n" +
                    "function " + SendRetrieveBLOBFunctionName + "(var_in in BLOB) \n" +
                    "return BLOB\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    return var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveBLOBFunction = "DROP FUNCTION \"" + SendRetrieveBLOBFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveNCLOBFunctionName = "SendRetrieveNCLOB";
    private static final String CreateSendRetrieveNCLOBFunction =
            "create or replace\n" +
                    "function " + SendRetrieveNCLOBFunctionName + "(var_in in NCLOB) \n" +
                    "return NCLOB\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveNCLOBFunction = "DROP FUNCTION \"" + SendRetrieveNCLOBFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveCLOBFunctionName = "SendRetrieveCLOB";
    private static final String CreateSendRetrieveCLOBFunction =
            "create or replace\n" +
                    "function " + SendRetrieveCLOBFunctionName + "(var_in in CLOB) \n" +
                    "return CLOB\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveCLOBFunction = "DROP FUNCTION \"" + SendRetrieveCLOBFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveRawFunctionName = "SendRetrieveRaw";
    private static final String CreateSendRetrieveRawFunction =
            "create or replace\n" +
                    "function " + SendRetrieveRawFunctionName + "(var_in in Raw) \n" +
                    "return Raw\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveRawFunction = "DROP FUNCTION \"" + SendRetrieveRawFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveTimeStampFunctionName = "SendRetrieveTimeStamp";
    private static final String CreateSendRetrieveTimeStampFunction =
            "create or replace\n" +
                    "function " + SendRetrieveTimeStampFunctionName + "(var_in in TimeStamp) \n" +
                    "return TimeStamp\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + 1;\n" +
                    "END;";
    public static final String DropSendRetrieveTimeStampFunction = "DROP FUNCTION \"" + SendRetrieveTimeStampFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveDateFunctionName = "SendRetrieveDate";
    private static final String CreateSendRetrieveDateFunction =
            "create or replace\n" +
                    "function " + SendRetrieveDateFunctionName + "(var_in in Date) \n" +
                    "return Date\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + 1;\n" +
                    "END;";
    public static final String DropSendRetrieveDateFunction = "DROP FUNCTION \"" + SendRetrieveDateFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveLongFunctionName = "SendRetrieveLong";
    private static final String CreateSendRetrieveLongFunction =
            "create or replace\n" +
                    "function " + SendRetrieveLongFunctionName + "(var_in in Long) \n" +
                    "return Long\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveLongFunction = "DROP FUNCTION \"" + SendRetrieveLongFunctionName.toUpperCase() + "\"";

    private static final String SendRetrievePlsIntegerFunctionName = "SendRetrievePlsInteger";
    private static final String CreateSendRetrievePlsIntegerFunction =
            "create or replace\n" +
                    "function " + SendRetrievePlsIntegerFunctionName + "(var_in in PLS_Integer) \n" +
                    "return PLS_Integer\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + var_in;\n" +
                    "END;";
    public static final String DropSendRetrievePlsIntegerFunction = "DROP FUNCTION \"" + SendRetrievePlsIntegerFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveBinaryIntegerFunctionName = "SendRetrieveBinaryInteger";
    private static final String CreateSendRetrieveBinaryIntegerFunction =
            "create or replace\n" +
                    "function " + SendRetrieveBinaryIntegerFunctionName + "(var_in in BINARY_Integer) \n" +
                    "return BINARY_Integer\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveBinaryIntegerFunction = "DROP FUNCTION \"" + SendRetrieveBinaryIntegerFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveBinaryDoubleFunctionName = "SendRetrieveBinaryDouble";
    private static final String CreateSendRetrieveBinaryDoubleFunction =
            "create or replace\n" +
                    "function " + SendRetrieveBinaryDoubleFunctionName + "(var_in in BINARY_DOUBLE) \n" +
                    "return BINARY_DOUBLE\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveBinaryDoubleFunction = "DROP FUNCTION \"" + SendRetrieveBinaryDoubleFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveBinaryFloatFunctionName = "SendRetrieveBinaryFloat";
    private static final String CreateSendRetrieveBinaryFloatFunction =
            "create or replace\n" +
                    "function " + SendRetrieveBinaryFloatFunctionName + "(var_in in BINARY_FLOAT) \n" +
                    "return BINARY_FLOAT\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveBinaryFloatFunction = "DROP FUNCTION \"" + SendRetrieveBinaryFloatFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveNumberFunctionName = "SendRetrieveNumber";
    private static final String CreateSendRetrieveNumberFunction =
            "create or replace\n" +
                    "function " + SendRetrieveNumberFunctionName + "(var_in in Number) \n" +
                    "return Number\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in + var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveNumberFunction = "DROP FUNCTION \"" + SendRetrieveNumberFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveNCharFunctionName = "SendRetrieveNChar";
    private static final String CreateSendRetrieveNCharFunction =
            "create or replace\n" +
                    "function " + SendRetrieveNCharFunctionName + "(var_in in NChar) \n" +
                    "return NChar\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveNCharFunction = "DROP FUNCTION \"" + SendRetrieveNCharFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveCharFunctionName = "SendRetrieveChar";
    private static final String CreateSendRetrieveCharFunction =
            "create or replace\n" +
                    "function " + SendRetrieveCharFunctionName + "(var_in in Char) \n" +
                    "return Char\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveCharFunction = "DROP FUNCTION \"" + SendRetrieveCharFunctionName.toUpperCase() + "\"";

    private static final String RetrieveBooleanFunctionName = "RetrieveBoolean";
    private static final String CreateRetrieveBooleanFunction =
            "create or replace\n" +
                    "function " + RetrieveBooleanFunctionName + "(int_in in INT) \n" +
                    "return BOOLEAN\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    IF (int_in=1) THEN\n" +
                    "  return TRUE;\n" +
                    "ELSE\n" +
                    "  return FALSE;\n" +
                    "END IF;\n" +
                    "END;";
    public static final String DropRetrieveBooleanFunction = "DROP FUNCTION \"" + RetrieveBooleanFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveVarcharFunctionName = "SendRetrieveVarchar";
    private static final String CreateSendRetrieveVarcharFunction =
            "create or replace\n" +
                    "function " + SendRetrieveVarcharFunctionName + "(var_in in Varchar) \n" +
                    "return Varchar\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveVarcharFunction = "DROP FUNCTION \"" + SendRetrieveVarcharFunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveNVarchar2FunctionName = "SendRetrieveNVarchar2";
    private static final String CreateSendRetrieveNVarchar2Function =
            "create or replace\n" +
                    "function " + SendRetrieveNVarchar2FunctionName + "(var_in in NVarchar2) \n" +
                    "return NVarchar2\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveNVarchar2Function = "DROP FUNCTION \"" + SendRetrieveNVarchar2FunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveVarchar2FunctionName = "SendRetrieveVarchar2";
    private static final String CreateSendRetrieveVarchar2Function =
            "create or replace\n" +
                    "function " + SendRetrieveVarchar2FunctionName + "(var_in in Varchar2) \n" +
                    "return Varchar2\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN var_in || var_in;\n" +
                    "END;";
    public static final String DropSendRetrieveVarchar2Function = "DROP FUNCTION \"" + SendRetrieveVarchar2FunctionName.toUpperCase() + "\"";

    private static final String SendRetrieveBooleanFunctionName = "SendRetrieveBoolean";
    private static final String CreateSendRetrieveBooleanFunction =
            "create or replace\n" +
                    "function " + SendRetrieveBooleanFunctionName + "(boolean_in in BOOLEAN) \n" +
                    "return BOOLEAN\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN not(boolean_in);\n" +
                    "END;";
    public static final String DropSendRetrieveBooleanFunction = "DROP FUNCTION \"" + SendRetrieveBooleanFunctionName.toUpperCase() + "\"";

    private static final String CreateGetYEarFromTimestampProcedureName = "GetYearFromTimestamp";
    private static final String CreateGetYEarFromTimestampProcedure =
            "create or replace\n" +
                    "procedure " + CreateGetYEarFromTimestampProcedureName + "\n" +
                    "                    (timein IN timestamp,\n" +
                    "                    yearout out NVARCHAR2)\n" +
                    "                    is\n" +
                    "                    begin\n" +
                    "                      yearout := to_char(timein, 'YYYY');  \n" +
                    "                    end ;\n" +
                    "                    ";
    public static final String DropGetYEarFromTimestampProcedure = "DROP PROCEDURE " + CreateGetYEarFromTimestampProcedureName;

    private static final String CreateGetCurrentTimestampProcedureName = "GetCurrentTimestamp";
    private static final String CreateGetCurrentTimestampProcedure =
            "create or replace\n" +
                    "procedure " + CreateGetCurrentTimestampProcedureName + "\n" +
                    "                    (currentTime out timestamp)\n" +
                    "                    is\n" +
                    "                    begin\n" +
                    "                      currentTime := sysdate;  \n" +
                    "                    end ;";
    public static final String DropGetCurrentTimestampProcedure = "DROP PROCEDURE " + CreateGetCurrentTimestampProcedureName;

    private static final String CreateGetCurrentTimestampFunctionName = "GetCurrentTimestampFunc";
    private static final String CreateGetCurrentTimestampFunction =
            "create or replace\n" +
                    "function " + CreateGetCurrentTimestampFunctionName + "\n" +
                    "return timestamp\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN sysdate;\n" +
                    "END;";
    public static final String DropGetCurrentTimestampFunction = "DROP FUNCTION \"" + CreateGetCurrentTimestampFunctionName.toUpperCase() + "\"";

    private static final String CreateGetStringFunctionName = "GetStringFunc";
    private static final String CreateGetStringFunction =
            "create or replace\n" +
                    "function " + CreateGetStringFunctionName + "(size_of_blob in NUMBER) \n" +
                    "return varchar2\n" +
                    "is\n" +
                    "BEGIN\n" +
                    "    RETURN '123abc';\n" +
                    "END;";
    public static final String DropGetStringFunction = "DROP FUNCTION \"" + CreateGetStringFunctionName.toUpperCase() + "\"";
}
