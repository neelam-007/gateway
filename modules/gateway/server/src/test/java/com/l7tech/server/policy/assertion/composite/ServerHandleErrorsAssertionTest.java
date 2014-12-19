package com.l7tech.server.policy.assertion.composite;


import com.google.common.collect.Lists;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.HashMap;

public class ServerHandleErrorsAssertionTest {

    private PolicyEnforcementContext context;
    private ApplicationContext applicationContext;
    private ServerPolicyFactory policyFactory;

    @Before
    public void init() throws Exception {
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        policyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", policyFactory);
        }}));
        policyFactory.setApplicationContext(applicationContext);
    }

    private PolicyEnforcementContext context() {
        if (context != null) return context;
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        return context;
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNoErrors01() throws Exception {
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new FalseAssertion()));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        AssertionStatus status = shea.checkRequest(context());
        //false because of FalseAssertion
        Assert.assertEquals(AssertionStatus.FALSIFIED, status);
        context.getVariable("foobar.message");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNoErrors02() throws Exception {
        //Raise error did not execute as FalseAssertion would have returned first
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new FalseAssertion(), new RaiseErrorAssertion()));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        //false because of FalseAssertion
        AssertionStatus status = shea.checkRequest(context());
        Assert.assertEquals(AssertionStatus.FALSIFIED, status);
        context.getVariable("foobar.message");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNoErrors03() throws Exception {
        //Raise error did not execute as FalseAssertion would have returned first
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new TrueAssertion()));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        AssertionStatus status = shea.checkRequest(context());
        Assert.assertEquals(AssertionStatus.NONE, status);
        context.getVariable("foobar.message");
    }

    @Test
    public void testErrors01() throws Exception {
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new RaiseErrorAssertion()));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        AssertionStatus status = shea.checkRequest(context());
        Assert.assertEquals(AssertionStatus.NONE, status);
        Object m = context.getVariable("foobar.message");
        Assert.assertNotNull(m);
    }

    @Test(expected = NoSuchVariableException.class)
    public void testErrors02() throws Exception {
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new RaiseErrorAssertion(), new SetVariableAssertion("foo", "bar")));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        AssertionStatus status = shea.checkRequest(context());
        Assert.assertEquals(AssertionStatus.NONE, status);
        Object m = context.getVariable("foobar.message");
        Assert.assertNotNull(m);
        //foo doesn't exist as execution would have stopped
        context.getVariable("foo");
    }

    @Test
    public void testErrors03() throws Exception {
        HandleErrorsAssertion hea = new HandleErrorsAssertion(Lists.newArrayList(new SetVariableAssertion("foo", "bar"), new RaiseErrorAssertion()));
        hea.setVariablePrefix("foobar");
        ServerHandleErrorsAssertion shea = (ServerHandleErrorsAssertion) policyFactory.compilePolicy(hea, false);
        AssertionStatus status = shea.checkRequest(context());
        Assert.assertEquals(AssertionStatus.NONE, status);
        Object m = context.getVariable("foobar.message");
        Assert.assertNotNull(m);
        Object n = context.getVariable("foo");
        Assert.assertEquals("bar", n);
    }
}
