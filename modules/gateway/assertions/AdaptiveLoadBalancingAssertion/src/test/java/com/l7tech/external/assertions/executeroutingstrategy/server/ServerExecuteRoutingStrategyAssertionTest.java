package com.l7tech.external.assertions.executeroutingstrategy.server;

import com.l7tech.common.io.failover.*;
import com.l7tech.external.assertions.executeroutingstrategy.ExecuteRoutingStrategyAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ServerExecuteRoutingStrategyAssertionTest {

    private static final String STRATEGY = "prefix";
    private static final String ROUTE = "route";
    private static final String FEEDBACK = "feedback";
    private static final Service SERVER1 = new Service("server1", null);
    private static final Service SERVER2 = new Service("server2", null);


    private ExecuteRoutingStrategyAssertion assertion;
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
        peCtx.setVariable(STRATEGY, new RoundRobinFailoverStrategy<Service>(new Service[]{SERVER1, SERVER2}));
        assertion = new ExecuteRoutingStrategyAssertion();
        assertion.setStrategy(STRATEGY);
        assertion.setRoute(ROUTE);
        assertion.setFeedback(FEEDBACK);

    }

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test
    public void testGetRoute() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertNotNull(peCtx.getVariable(ROUTE));
        assertNotNull(peCtx.getVariable(FEEDBACK));
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testNoRoute() throws Exception {
        peCtx.setVariable(STRATEGY, new FailoverStrategy<Service>() {
            @Override
            public Service selectService() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
            @Override
            public void reportFailure(Service service) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
            @Override
            public void reportSuccess(Service service) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
            @Override
            public String getName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
            @Override
            public String getDescription() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
            @Override
            public void reportContent(Object content, Feedback feedback) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        FailoverStrategy s = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testNoStrategy() throws Exception {
        peCtx.setVariable(STRATEGY, null);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testFeedbackCreated() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        List<Feedback> f1 = (List<Feedback>) peCtx.getVariable(FEEDBACK);
        sass.checkRequest(peCtx);
        List<Feedback> f2 = (List<Feedback>) peCtx.getVariable(FEEDBACK);
        assertTrue(f1 == f2);
        
    }

}
