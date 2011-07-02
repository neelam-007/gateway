package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.message.Message;
import com.l7tech.util.TextUtils;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the JoinAssertion.
 * @noinspection FieldCanBeLocal
 */
public class ServerJoinAssertionTest {
    private PolicyEnforcementContext context;
    private String b1;
    private String b2;
    private String b3;
    private String joined;
    private String inputVariable;
    private String outputVariable;
    private JoinAssertion assertion;

    @Before
    public void setUp() throws Exception {
        context = getContext();
        b1 = "blah 1 blah blah";
        b2 = "bleeh 1 blee blee";
        b3 = "blih 1 blih blih";
        joined = TextUtils.join(",", new CharSequence[] {b1, b2, b3}).toString();
        inputVariable = "myinputvar";
        outputVariable = "myoutputvar";
        context.setVariable(inputVariable, new String[] { b1, b2, b3 });
        assertion = new JoinAssertion();
        assertion.setInputVariable(inputVariable);
        assertion.setOutputVariable(outputVariable);
    }

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testJoinArray() throws Exception {
        ServerJoinAssertion sja = new ServerJoinAssertion(assertion);
        AssertionStatus result = sja.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof String);
        Assert.assertEquals(outputObj, joined);
    }

    @Test
    public void testJoinCollection() throws Exception {
        assertion.setJoinSubstring(">,<");
        List<String> list = new ArrayList<String>();
        list.add(b1);
        list.add(b2);
        list.add(b3);
        context.setVariable(inputVariable, list);

        ServerJoinAssertion sja = new ServerJoinAssertion(assertion);
        AssertionStatus result = sja.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof String);
        Assert.assertEquals(outputObj, b1 + ">,<" + b2 + ">,<" + b3);
    }

    /**
     * If intput value is single valued, then the output should equal the single value.
     * @throws Exception
     */
    @Test
    public void testJoinSingleValuedVariable() throws Exception {
        final PolicyEnforcementContext context = getContext();
        context.setVariable("input", new String[]{"one"});

        JoinAssertion assertion = new JoinAssertion();
        assertion.setInputVariable("input");
        assertion.setOutputVariable("output");

        ServerJoinAssertion serverJoinAssertion = new ServerJoinAssertion(assertion);
        final AssertionStatus status = serverJoinAssertion.checkRequest(context);

        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
        Assert.assertEquals("Invalid value found", "one", context.getVariable("output"));
    }
}
