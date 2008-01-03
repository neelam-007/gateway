package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.common.message.Message;
import com.l7tech.common.util.HexUtils;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * Test the SplitAssertion.
 * @noinspection FieldCanBeLocal
 */
public class ServerSplitAssertionTest extends TestCase {
    private Message request;
    private Message response;
    private PolicyEnforcementContext context;
    private String b1;
    private String b2;
    private String b3;
    private String joined;
    private String inputVariable;
    private String outputVariable;
    private SplitAssertion assertion;

    public ServerSplitAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerSplitAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        request = new Message();
        response = new Message();
        context = new PolicyEnforcementContext(request, response);
        b1 = "blah 1 blah blah";
        b2 = "bleeh 1 blee blee";
        b3 = "blih 1 blih blih";
        joined = HexUtils.join(",", new CharSequence[] {b1, b2, b3}).toString();
        inputVariable = "myinputvar";
        outputVariable = "myoutputvar";
        context.setVariable(inputVariable, joined);
        assertion = new SplitAssertion();
        assertion.setInputVariable(inputVariable);
        assertion.setOutputVariable(outputVariable);
    }

    public void testSplit() throws Exception {
        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion, null);
        AssertionStatus result = ssa.checkRequest(context);

        assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        assertTrue(outputObj instanceof List);
        List outs = (List)outputObj;
        for (Object outObj : outs)
            assertTrue(outObj instanceof String);
        assertEquals(outs.size(), 3);
        assertEquals(outs.get(0), b1);
        assertEquals(outs.get(1), b2);
        assertEquals(outs.get(2), b3);
    }

    public void testSplitOnWhitespace() throws Exception {
        String w1 = "asdfjhasdfljkahd";
        String w2 = "h097y2,asdf\u382724,szcxv;:248y\\swegsfz";
        String w3 = "asdfasdflkeasdfasdf\u2827\u1827ahaiwuherf";
        String big = w1 + " \t \r   \n " + w2 + "  \t   \t\t  \n" + w3;
        context.setVariable(inputVariable, big);
        assertion.setSplitPattern("\\s+");

        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion, null);
        AssertionStatus result = ssa.checkRequest(context);

        assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        assertTrue(outputObj instanceof List);
        List outs = (List)outputObj;
        for (Object outObj : outs)
            assertTrue(outObj instanceof String);
        assertEquals(outs.size(), 3);
        assertEquals(outs.get(0), w1);
        assertEquals(outs.get(1), w2);
        assertEquals(outs.get(2), w3);
    }
}
