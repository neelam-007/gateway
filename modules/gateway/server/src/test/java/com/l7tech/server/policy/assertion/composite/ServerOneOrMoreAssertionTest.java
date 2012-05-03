package com.l7tech.server.policy.assertion.composite;

import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
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

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 30/04/12
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerOneOrMoreAssertionTest {
    private OneOrMoreAssertion assertion;
    private TrueAssertion trueAssertion1;
    private TrueAssertion trueAssertion2;
    private FalseAssertion falseAssertion1;
    private FalseAssertion falseAssertion2;
    private PolicyEnforcementContext peCtx;
    static ServerPolicyFactory serverPolicyFactory;

    private PolicyEnforcementContext makeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @BeforeClass
    public static void init() throws Exception {
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
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
        assertion = new OneOrMoreAssertion();
        trueAssertion1 = new TrueAssertion();
        trueAssertion2 = new TrueAssertion();
        falseAssertion1 = new FalseAssertion();
        falseAssertion2 = new FalseAssertion();
    }

    @Test
    public void testOneSuccess() throws Exception {
        assertion.addChild(trueAssertion1);
        assertion.addChild(falseAssertion1);
        assertion.addChild(falseAssertion2);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result  = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testTwoSuccess() throws Exception {
        assertion.addChild(trueAssertion1);
        assertion.addChild(trueAssertion2);
        assertion.addChild(falseAssertion1);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result  = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    public void testNoSuccess() throws Exception {
        assertion.addChild(falseAssertion1);
        assertion.addChild(falseAssertion2);
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result  = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testNoChildAssertion() throws Exception {
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result  = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }
}
