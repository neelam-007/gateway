package com.l7tech.external.assertions.createroutingstrategy.server;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.RoundRobinFailoverStrategy;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerCreateRoutingStrategyAssertionTest {

    private static final String STRATEGY = "prefix";
    private static final Service SERVER1 = new Service("server1", null);
    private static final Service SERVER2 = new Service("server2", null);
    private static final String DSERVER_SV1 = "dserver_sv1";
    private static final String DSERVER_MV1 = "dserver_mv1";
    private static final String DSERVER_MV2 = "dserver_mv2";
    private static final String DSERVER_MV3 = "dserver_mv3";
    private static final Service DSERVER_SV = new Service("${server}", null);
    private static final Service DSERVER_MV = new Service("${servers}", null);


    private CreateRoutingStrategyAssertion assertion;
    private PolicyEnforcementContext peCtx;
    static ServerPolicyFactory serverPolicyFactory;
    static GenericApplicationContext applicationContext;
    static FailoverStrategyFactory failoverStrategyFactory;


    @BeforeClass
    public static void init() throws Exception {
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        failoverStrategyFactory = new FailoverStrategyFactory();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("failoverStrategyFactory", failoverStrategyFactory);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
    }

    @Before
    public void setUp() throws Exception {

        // Get the policy enforcement context
        //create assertion
        peCtx = makeContext();
        peCtx.setVariable("server", DSERVER_SV1);
        peCtx.setVariable("servers", new String[]{DSERVER_MV1, DSERVER_MV2});
        assertion = new CreateRoutingStrategyAssertion();
        assertion.setStrategy(STRATEGY);
        Service[] servers = new Service[]{SERVER1, DSERVER_SV, SERVER2, DSERVER_MV};
        assertion.setRoutes(Arrays.asList(servers));
        assertion.setStrategyName(new RoundRobinFailoverStrategy<Service>(new Service[]{SERVER1}).getName());
        
    }

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test
    public void testCreateStrategy() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertNotNull(peCtx.getVariable(STRATEGY));

    }

    @Test
    public void testStaticServers() throws Exception {
        assertion.setRoutes(Arrays.asList(SERVER1, SERVER2));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertNotNull(peCtx.getVariable(STRATEGY));
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testCachedServersWithDynamic() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        FailoverStrategy s = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        FailoverStrategy s2 = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        assertNotNull(s);
        assertNotNull(s2);
        assertTrue(s == s2);
    }

    @Test
    public void testServerListChange() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        Assert.assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        FailoverStrategy s = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        peCtx.setVariable("servers", new String[]{DSERVER_MV1, DSERVER_MV2, DSERVER_MV3});
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        FailoverStrategy s2 = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        assertTrue(s != s2);
        assertNotNull(s);
        assertNotNull(s2);
    }

    @Test
    public void shouldReturnFalsifiedWhenNoRoutesPresent() throws Exception {
        CreateRoutingStrategyAssertion ass = new CreateRoutingStrategyAssertion();
        ass.setRoutes(new ArrayList<Service>());
        PolicyEnforcementContext peCtx = makeContext();
        peCtx.setVariable("emptyRoutes","");
        peCtx.setVariable("empty","");
        List<Service> routes = new ArrayList<Service>();
        routes.add(new Service("${emptyRoutes}", null));
        routes.add(new Service("${empty}",null));
        ass.setRoutes(routes);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);
        Assert.assertEquals(AssertionStatus.FALSIFIED,sass.checkRequest(peCtx));
    }

}
