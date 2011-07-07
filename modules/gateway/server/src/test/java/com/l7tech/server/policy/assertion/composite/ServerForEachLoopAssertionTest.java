package com.l7tech.server.policy.assertion.composite;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for ForEachLoopAssertion.
 */
public class ServerForEachLoopAssertionTest {
    Document doc1 = XmlUtil.stringAsDocument("<doc1/>");
    Document doc2 = XmlUtil.stringAsDocument("<doc2/>");
    Document doc3 = XmlUtil.stringAsDocument("<doc3/>");

    Object[] stringsArray = { "foo", "bar", "baz" };
    Object[] messagesArray = { new Message(doc1), new Message(doc2), new Message(doc3) };
    Object[] emptyArray = {};

    SetVariableAssertion setvar = new SetVariableAssertion("accum", "${accum},${i.iterations}-${i.current}");
    SetVariableAssertion setvarMainpart = new SetVariableAssertion("accum", "${accum},${i.current.mainpart}");
    ForEachLoopAssertion ass = new ForEachLoopAssertion(Arrays.asList(setvar));
    ApplicationContext applicationContext;
    ServerPolicyFactory policyFactory;
    ServerForEachLoopAssertion sass;
    PolicyEnforcementContext context;

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

    @Test
    public void testSimpleIteration() throws Exception {
        ass.setLoopVariableName("stringsArray");
        ass.setVariablePrefix("i");
        checkRequest();
        assertEquals(",0-foo,1-bar,2-baz", context.getVariable("accum"));
        assertEquals("baz", context.getVariable("i.current"));
        assertEquals(3, context.getVariable("i.iterations"));
        assertEquals(false, context.getVariable("i.exceededlimit"));
    }

    @Test
    public void testIterationLimit() throws Exception {
        ass.setLoopVariableName("stringsArray");
        ass.setVariablePrefix("i");
        ass.setIterationLimit(2);
        checkRequest();
        assertEquals(",0-foo,1-bar", context.getVariable("accum"));
        assertEquals("bar", context.getVariable("i.current"));
        assertEquals(2, context.getVariable("i.iterations"));
        assertEquals(true, context.getVariable("i.exceededlimit"));
    }

    @Test
    public void testMessageIteration() throws Exception {
        ass.setChildren(Collections.singletonList(setvarMainpart));
        ass.setLoopVariableName("messagesList");
        ass.setVariablePrefix("i");
        checkRequest();
        assertEquals(",<doc1/>,<doc2/>,<doc3/>", context.getVariable("accum"));
    }

    @Test
    public void testEmptyArray() throws Exception {
        ass.setLoopVariableName("emptyArray");
        ass.setVariablePrefix("i");
        checkRequest();
        assertNoSuchVariable("accum");
        assertNoSuchVariable("i.current");
        assertEquals(0, context.getVariable("i.iterations"));
        assertEquals(false, context.getVariable("i.exceededlimit"));
    }

    @Test
    public void testSingletonList() throws Exception {
        ass.setLoopVariableName("singletonList");
        ass.setVariablePrefix("i");
        checkRequest();
        assertEquals(",0-blah", context.getVariable("accum"));
        assertEquals("blah", context.getVariable("i.current"));
        assertEquals(1, context.getVariable("i.iterations"));
        assertEquals(false, context.getVariable("i.exceededlimit"));
    }

    @Test
    public void testSingleValuedVariable() throws Exception {
        ass.setLoopVariableName("someString");
        ass.setVariablePrefix("i");
        checkRequest();
        assertEquals(",0-contentsOfSomeString", context.getVariable("accum"));
        assertEquals("contentsOfSomeString", context.getVariable("i.current"));
        assertEquals(1, context.getVariable("i.iterations"));
        assertEquals(false, context.getVariable("i.exceededlimit"));
    }

    @Test
    public void testChildPolicyFailure() throws Exception {
        ass = new ForEachLoopAssertion(Collections.singletonList(new FalseAssertion()));
        ass.setLoopVariableName("stringsList");
        ass.setVariablePrefix("x");
        checkRequest(AssertionStatus.FAILED);
        assertNoSuchVariable("accum");
        assertEquals("foo", context.getVariable("x.current"));
        assertEquals(0, context.getVariable("x.iterations"));
        assertEquals(false, context.getVariable("x.exceededlimit"));
    }

    @Test
    public void testNoSuchTargetVar() throws Exception {
        ass.setLoopVariableName("nonexist");
        ass.setVariablePrefix("i");
        checkRequest(AssertionStatus.FAILED);
        assertNoSuchVariable("accum");
        assertNoSuchVariable("i.current");
        assertNoSuchVariable("i.iterations");
        assertNoSuchVariable("i.exceededlimit");
    }

    private void assertNoSuchVariable(String var) {
        try {
            context.getVariable(var);
            fail("Expected NoSuchVariableException for " + var);
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

    private void checkRequest() throws Exception {
        checkRequest(AssertionStatus.NONE);
    }

    private void checkRequest(AssertionStatus expected) throws Exception {
        AssertionStatus result;
        try {
            result = sass().checkRequest(context());
        } catch (AssertionStatusException e) {
            result = e.getAssertionStatus();
        }
        assertEquals(expected, result);
    }

    private PolicyEnforcementContext context() {
        if (context != null)
            return context;

        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        context.setVariable("stringsArray", stringsArray);
        context.setVariable("messagesArray", messagesArray);
        context.setVariable("emptyArray", emptyArray);

        context.setVariable("stringsList", Arrays.asList(stringsArray));
        context.setVariable("messagesList", Arrays.asList(messagesArray));
        context.setVariable("emptyList", Collections.<Object>emptyList());
        context.setVariable("singletonList", Collections.singletonList("blah"));

        context.setVariable("someString", "contentsOfSomeString");

        return context;
    }

    private ServerForEachLoopAssertion sass() throws Exception {
        return sass != null ? sass : (sass = (ServerForEachLoopAssertion) policyFactory.compilePolicy(ass, false));
    }
}
