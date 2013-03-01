package com.l7tech.skunkworks.jdbc;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the server jdbc query assertion.
 *
 * @author Victor Kazakov
 */
public class ServerJdbcQueryAssertionIntegrationTests extends JdbcCallHelperIntegrationAbstractBaseTestClass {

    private static ApplicationContext context;
    private static Map<String, String> configProperties = new HashMap<>();
    private static MockConfig mockConfig = new MockConfig(configProperties);

    private static PolicyEnforcementContext policyEnforcementContext;
    private static Map<String, Object> contextVariables = new HashMap<>();

    /**
     * Sets up the mock objects
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        JdbcCallHelperIntegrationAbstractBaseTestClass.beforeClass();

        context = Mockito.mock(ApplicationContext.class);

        Mockito.doReturn(getJdbcQueryingManager()).when(context).getBean(Matchers.eq("jdbcQueryingManager"), Matchers.eq(JdbcQueryingManager.class));
        Mockito.doReturn(mockConfig).when(context).getBean(Matchers.eq("serverConfig"), Matchers.eq(Config.class));

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
        configProperties.clear();
        contextVariables.clear();
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
            assertion.setAllowMultiValuedVariables(true);
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
            assertion.setAllowMultiValuedVariables(true);
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
            assertion.setAllowMultiValuedVariables(true);
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
            assertion.setAllowMultiValuedVariables(true);
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

    private static JdbcQueryAssertion createJdbcQueryAssertion() {
        JdbcQueryAssertion jdbcQueryAssertion = new JdbcQueryAssertion();
        jdbcQueryAssertion.setConnectionName(ConnectionName);
        jdbcQueryAssertion.setVariablePrefix("jdbcQuery");
        return jdbcQueryAssertion;
    }

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
