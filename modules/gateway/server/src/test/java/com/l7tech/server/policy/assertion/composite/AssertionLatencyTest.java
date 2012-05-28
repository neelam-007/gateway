package com.l7tech.server.policy.assertion.composite;

import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.TestTimeSource;
import com.l7tech.util.TimeSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssertionLatencyTest {

    private MockCompositeAssertion assertion;
    private SetVariableAssertion assertion1;
    private SetVariableAssertion assertion2;
    private SetVariableAssertion assertion3;
    private PolicyEnforcementContext peCtx;
    static ServerPolicyFactory serverPolicyFactory;
    static GenericApplicationContext applicationContext;
    private TimeSource timeSource;


    @BeforeClass
    public static void init() throws Exception {
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
    }

    @Before
    public void setUp() throws Exception {

        // Get the policy enforcement context
        //create assertion
        peCtx = makeContext();
        timeSource = new TestTimeSource();
        assertion = new MockCompositeAssertion();
        assertion.setTimeSource(timeSource);
        assertion1 = new DelayAssertion("a", "Test1", 100, timeSource);
        assertion2 = new DelayAssertion("b", "Test2", 200, timeSource);
        assertion3 = new DelayAssertion("c", "Test3", 300, timeSource);
        assertion.addChild(assertion1);
        assertion.addChild(assertion2);
        assertion.addChild(assertion3);

    }

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test
    public void testNoLatencyCaptured() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        assertEquals(0L, peCtx.getVariable("assertion.latency.ns"));
    }

    @Test
    public void testFirstLatencyCaptured() throws Exception {
        assertion1.setExpression("${assertion.latency.ns}");
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        assertEquals(0L, peCtx.getVariable("assertion.latency.ns"));
        assertEquals(0L, Long.parseLong((String) peCtx.getVariable("a")));
    }

    @Test
    public void testSecondLatencyCaptured() throws Exception {
        assertion2.setExpression("${assertion.latency.ns}");
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        check(TimeUnit.NANOSECONDS, (Long) peCtx.getVariable("assertion.latency.ns"), 100L);
        assertEquals(peCtx.getVariable("assertion.latency.ns"), Long.parseLong((String) peCtx.getVariable("b")));
    }

    @Test
    public void testAllAssertionLatencyCapture() throws Exception {
        MockCompositeAssertion root = new MockCompositeAssertion();
        root.setTimeSource(timeSource);
        assertion.addChild(new DelayAssertion("c.1", "${assertion.latency.ns}", 0, timeSource));
        root.addChild(assertion);
        root.addChild(new DelayAssertion("d", "${assertion.latency.ns}", 0, timeSource));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(root, false);
        sass.checkRequest(peCtx);
        check(TimeUnit.NANOSECONDS, (Long) peCtx.getVariable("assertion.latency.ns"), 600L);
        check(TimeUnit.NANOSECONDS, Long.parseLong((String) peCtx.getVariable("c.1")), 300L);
        assertEquals(peCtx.getVariable("assertion.latency.ns"), Long.parseLong((String) peCtx.getVariable("d")));
    }

    @Test
    public void testAllAssertionLatencyCaptureMs() throws Exception {
        MockCompositeAssertion root = new MockCompositeAssertion();
        root.setTimeSource(timeSource);
        assertion.addChild(new DelayAssertion("c.1", "${assertion.latency.ms}", 0, timeSource));
        root.addChild(assertion);
        root.addChild(new DelayAssertion("d", "${assertion.latency.ms}", 0, timeSource));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(root, false);
        sass.checkRequest(peCtx);
        check(TimeUnit.MILLISECONDS, (Long) peCtx.getVariable("assertion.latency.ms"), 600L);
        check(TimeUnit.MILLISECONDS, Long.parseLong((String) peCtx.getVariable("c.1")), 300L);
        assertEquals(peCtx.getVariable("assertion.latency.ms"), Long.parseLong((String) peCtx.getVariable("d")));
    }

    /**
     * Make sure the checkTime is greater than the minimum time
     * but less than the minimum + 100.
     *
     * @param checkTime the time to verify
     * @param time      the minimum time in millisecond
     * @parma unit the checkTime unit
     */
    private void check(TimeUnit unit, long checkTime, long time) {
        checkTime = unit.toMillis(checkTime);
        assertEquals(checkTime,time);
    }

}
