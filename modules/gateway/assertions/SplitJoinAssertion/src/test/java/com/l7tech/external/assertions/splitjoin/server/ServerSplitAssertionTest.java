package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.TextUtils;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import java.util.Collection;
import java.util.List;

/**
 * Test the SplitAssertion.
 *
 * @noinspection FieldCanBeLocal
 */
public class ServerSplitAssertionTest {
    private PolicyEnforcementContext context;
    private String b1;
    private String b2;
    private String b3;
    private String joined;
    private String inputVariable;
    private String outputVariable;
    private SplitAssertion assertion;

    @Before
    public void setUp() throws Exception {
        context = getContext();
        b1 = "blah 1 blah blah";
        b2 = "bleeh 1 blee blee";
        b3 = "blih 1 blih blih";
        joined = TextUtils.join(",", new CharSequence[]{b1, b2, b3}).toString();
        inputVariable = "myinputvar";
        outputVariable = "myoutputvar";
        context.setVariable(inputVariable, joined);
        assertion = new SplitAssertion();
        assertion.setInputVariable(inputVariable);
        assertion.setOutputVariable(outputVariable);
    }

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testSplit() throws Exception {
        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion, null);
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals(outs.size(), 3);
        Assert.assertEquals(outs.get(0), b1);
        Assert.assertEquals(outs.get(1), b2);
        Assert.assertEquals(outs.get(2), b3);
    }

    @Test
    public void testSplitOnWhitespace() throws Exception {
        String w1 = "asdfjhasdfljkahd";
        String w2 = "h097y2,asdf\u382724,szcxv;:248y\\swegsfz";
        String w3 = "asdfasdflkeasdfasdf\u2827\u1827ahaiwuherf";
        String big = w1 + " \t \r   \n " + w2 + "  \t   \t\t  \n" + w3;
        context.setVariable(inputVariable, big);
        assertion.setSplitPattern("\\s+");

        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion, null);
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals(outs.size(), 3);
        Assert.assertEquals(outs.get(0), w1);
        Assert.assertEquals(outs.get(1), w2);
        Assert.assertEquals(outs.get(2), w3);
    }

    /**
     * When pattern does not match, the output should equal the input.
     * @throws Exception
     */
    @Test
    public void testNoMatchOfPattern() throws Exception{
        final PolicyEnforcementContext context = getContext();
        final String value = "one;two;three";
        context.setVariable("input", value);

        SplitAssertion assertion = new SplitAssertion();
        assertion.setInputVariable("input");
        assertion.setOutputVariable("output");

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion, null);
        final AssertionStatus status = serverSplitAssertion.checkRequest(context);

        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Object variable = context.getVariable("output");
        Assert.assertTrue("Variable is wrong type", variable instanceof Collection);

        String s = ExpandVariables.process("${output}", context.getVariableMap(new String[]{"output"}, null), null);
        Assert.assertEquals("Invalid value found", value, s);

        s = ExpandVariables.process("${output[0]}", context.getVariableMap(new String[]{"output"}, null), null);
        Assert.assertEquals("Invalid value found", value, s);
    }

    /**
     * Tests spaces are not removed from the split pattern
     * @throws Exception
     */
    @Test
    public void testSpaces() throws Exception{
        final PolicyEnforcementContext context = getContext();
        final String value = "one two three";
        context.setVariable("input", value);

        SplitAssertion assertion = new SplitAssertion();
        assertion.setInputVariable("input");
        assertion.setOutputVariable("output");
        assertion.setSplitPattern(" ");

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion, null);
        final AssertionStatus status = serverSplitAssertion.checkRequest(context);

        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Object variable = context.getVariable("output");
        Assert.assertTrue("Variable is wrong type", variable instanceof Collection);

        String s = ExpandVariables.process("${output[0]}", context.getVariableMap(new String[]{"output"}, null), null);
        Assert.assertEquals("Invalid value found", "one", s);

        s = ExpandVariables.process("${output[1]}", context.getVariableMap(new String[]{"output"}, null), null);
        Assert.assertEquals("Invalid value found", "two", s);

        s = ExpandVariables.process("${output[2]}", context.getVariableMap(new String[]{"output"}, null), null);
        Assert.assertEquals("Invalid value found", "three", s);    
    }

    /**
     * Tests spaces are not removed from the split pattern
     * @throws Exception
     */
    @Test
    public void testEmptyCompiles() throws Exception{
        final PolicyEnforcementContext context = getContext();
        final String value = "one two three";
        context.setVariable("input", value);

        SplitAssertion assertion = new SplitAssertion();
        assertion.setInputVariable("input");
        assertion.setOutputVariable("output");
        assertion.setSplitPattern("");

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion, null);
        final AssertionStatus status = serverSplitAssertion.checkRequest(context);

        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Object variable = context.getVariable("output");
        List<String> coll = (List<String>) variable;
        Assert.assertEquals("Wrong number of elements found", 14, coll.size());
        Assert.assertEquals("First character should be the empty string", "", coll.get(0));

        for(int i = 1; i < value.length(); i++){
            Assert.assertEquals("Wrong value found", String.valueOf(value.charAt(i - 1)), coll.get(i));
        }
   }
}
