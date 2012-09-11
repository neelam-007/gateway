package com.l7tech.external.assertions.processroutingstrategyresult.server;

import com.l7tech.common.io.failover.*;
import com.l7tech.external.assertions.processroutingstrategyresult.ProcessRoutingStrategyResultAssertion;
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
import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ServerProcessRoutingStrategyResultAssertionTest {

    private static final String STRATEGY = "prefix";
    private static final String ROUTE = "route";
    private static final String FEEDBACK = "feedback";
    private static final String LATENCY = FEEDBACK + ".current.latency";
    private static final String STATUS = FEEDBACK + ".current.status";
    private static final String FEEDBACK_ROUTE = FEEDBACK + ".current.route";
    private static final Service SERVER1 = new Service("server1", null);
    private static final Service SERVER2 = new Service("server2", null);
    private static final String REASON_CODE = FEEDBACK + ".current.reasonCode";


    private ProcessRoutingStrategyResultAssertion assertion;
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
        FailoverStrategy strategy = new RoundRobinFailoverStrategy<Service>(new Service[]{SERVER1, SERVER2});
        Service service = (Service) strategy.selectService();
        peCtx.setVariable(STRATEGY, strategy);
        peCtx.setVariable(ROUTE, service);
        peCtx.setVariable(FEEDBACK, new ArrayList<Feedback>());
        peCtx.setVariable(LATENCY, "1000");
        peCtx.setVariable(STATUS, 0);
        peCtx.setVariable(FEEDBACK_ROUTE, service);
        peCtx.setVariable(REASON_CODE, "100");

        assertion = new ProcessRoutingStrategyResultAssertion();
        assertion.setStrategy(STRATEGY);
        assertion.setFeedback(FEEDBACK);

    }

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test
    public void testRouteSuccess() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertNotNull(peCtx.getVariable(ROUTE));
        assertNotNull(peCtx.getVariable(FEEDBACK));
        List<Feedback> feedbacks = (List<Feedback>) peCtx.getVariable(FEEDBACK);
        Assert.assertEquals(1, feedbacks.size());
        Assert.assertEquals(1000, feedbacks.get(0).getLatency());
        Assert.assertEquals(0, feedbacks.get(0).getStatus());
        Assert.assertEquals(100, feedbacks.get(0).getReason());
        Assert.assertEquals(SERVER1.getName(), feedbacks.get(0).getRoute());
        FailoverStrategy strategy = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        Assert.assertEquals(SERVER2, strategy.selectService());
    }

    @Test
    public void testRouteFailed() throws Exception {
        peCtx.setVariable(STATUS, -1);

        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertNotNull(peCtx.getVariable(ROUTE));
        assertNotNull(peCtx.getVariable(FEEDBACK));
        List<Feedback> feedbacks = (List<Feedback>) peCtx.getVariable(FEEDBACK);
        Assert.assertEquals(1, feedbacks.size());
        Assert.assertEquals(1000, feedbacks.get(0).getLatency());
        Assert.assertEquals(-1, feedbacks.get(0).getStatus());
        Assert.assertEquals(100, feedbacks.get(0).getReason());
        Assert.assertEquals(SERVER1.getName(), feedbacks.get(0).getRoute());
        FailoverStrategy strategy = (FailoverStrategy) peCtx.getVariable(STRATEGY);
        Assert.assertEquals(SERVER2, strategy.selectService());
        Assert.assertEquals(SERVER2, strategy.selectService());
    }


    @Test
    public void testNoStrategy() throws Exception {
        peCtx.setVariable(STRATEGY, null);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testNoLatencyAndStatusAndReasonCode() throws Exception {
        //peCtx.setVariable(LATENCY, null);
        //peCtx.setVariable(STATUS, null);
        //peCtx.setVariable(REASON_CODE, null);
        PolicyEnforcementContext peCtx = makeContext();
        FailoverStrategy strategy = new RoundRobinFailoverStrategy<Service>(new Service[]{SERVER1, SERVER2});
        Service service = (Service) strategy.selectService();
        peCtx.setVariable(STRATEGY, strategy);
        peCtx.setVariable(ROUTE, service);
        peCtx.setVariable(FEEDBACK, new ArrayList<Feedback>());
        peCtx.setVariable(FEEDBACK_ROUTE, service);

        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertNotNull(peCtx.getVariable(ROUTE));
        assertNotNull(peCtx.getVariable(FEEDBACK));
        List<Feedback> feedbacks = (List<Feedback>) peCtx.getVariable(FEEDBACK);
        Assert.assertEquals(1, feedbacks.size());
        Assert.assertEquals(0, feedbacks.get(0).getLatency());
        Assert.assertEquals(0, feedbacks.get(0).getStatus());
        Assert.assertEquals(0, feedbacks.get(0).getReason());
        Assert.assertEquals(SERVER1.getName(), feedbacks.get(0).getRoute());
    }

}
