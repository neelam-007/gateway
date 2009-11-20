package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.message.Message;
import com.l7tech.util.TextUtils;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the JoinAssertion.
 * @noinspection FieldCanBeLocal
 */
public class ServerJoinAssertionTest extends TestCase {
    private Message request;
    private Message response;
    private PolicyEnforcementContext context;
    private String b1;
    private String b2;
    private String b3;
    private String joined;
    private String inputVariable;
    private String outputVariable;
    private JoinAssertion assertion;

    public ServerJoinAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerJoinAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        request = new Message();
        response = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
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

    public void testJoinArray() throws Exception {
        ServerJoinAssertion sja = new ServerJoinAssertion(assertion, null);
        AssertionStatus result = sja.checkRequest(context);

        assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        assertTrue(outputObj instanceof String);
        assertEquals(outputObj, joined);
    }
    
    public void testJoinCollection() throws Exception {
        assertion.setJoinSubstring(">,<");
        List<String> list = new ArrayList<String>();
        list.add(b1);
        list.add(b2);
        list.add(b3);
        context.setVariable(inputVariable, list);

        ServerJoinAssertion sja = new ServerJoinAssertion(assertion, null);
        AssertionStatus result = sja.checkRequest(context);

        assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        assertTrue(outputObj instanceof String);
        assertEquals(outputObj, b1 + ">,<" + b2 + ">,<" + b3);
    }
}