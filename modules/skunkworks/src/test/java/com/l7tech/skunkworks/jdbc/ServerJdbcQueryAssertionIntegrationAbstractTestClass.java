package com.l7tech.skunkworks.jdbc;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Config;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * This was created: 3/18/13 as 1:58 PM
 *
 * @author Victor Kazakov
 */
public abstract class ServerJdbcQueryAssertionIntegrationAbstractTestClass extends JdbcCallHelperIntegrationAbstractBaseTestClass {

    private static ApplicationContext context;

    private static PolicyEnforcementContext policyEnforcementContext;

    private static JdbcConnectionManager jdbcConnectionManager;

    public static Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static PolicyEnforcementContext getPolicyEnforcementContext() {
        return policyEnforcementContext;
    }

    private static Map<String, Object> contextVariables = new HashMap<>();

    /**
     * Sets up the mock objects
     *
     * @throws Exception
     */
    public static void beforeClass(@Nullable JdbcConnection jdbcConnection) throws Exception {
        JdbcCallHelperIntegrationAbstractBaseTestClass.beforeClass(jdbcConnection);

        context = Mockito.mock(ApplicationContext.class);

        jdbcConnectionManager = Mockito.mock(JdbcConnectionManager.class);

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

        Mockito.doReturn(jdbcConnectionManager).when(context).getBean(Matchers.eq("jdbcConnectionManager"), Matchers.eq(JdbcConnectionManager.class));
        Mockito.when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(ConnectionName))).thenReturn(jdbcConnection == null ? getJdbcConnection() : jdbcConnection);
    }

    /**
     * Clears config and contexts variables before each test.
     */
    @Before
    public void before() {
        getConfigProperties().clear();
        contextVariables.clear();

        getConfigProperties().put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_ENABLED, "false");
    }

    protected static JdbcQueryAssertion createJdbcQueryAssertion() {
        JdbcQueryAssertion jdbcQueryAssertion = new JdbcQueryAssertion();
        jdbcQueryAssertion.setConnectionName(ConnectionName);
        jdbcQueryAssertion.setVariablePrefix("jdbcQuery");
        return jdbcQueryAssertion;
    }
}
