package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.message.Message;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.test.BugNumber;
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
        joined = TextUtils.join(",", b1, b2, b3 ).toString();
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
        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion);
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals( (long) outs.size(), 3L );
        Assert.assertEquals(outs.get(0), b1);
        Assert.assertEquals(outs.get(1), b2);
        Assert.assertEquals(outs.get(2), b3);
    }

    @BugNumber(9286)
    @Test
    public void testSplit_NoRegex() throws Exception {
        assertion.setSplitPattern(".");
        assertion.setSplitPatternRegEx(false);
        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion);
        context.setVariable(inputVariable, "one.two.three");
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals( (long) outs.size(), 3L );
    }

    @BugNumber(9286)
    @Test
    public void testSplit_NoRegex_Identity() throws Exception {
        assertion.setSplitPattern(".");
        assertion.setSplitPatternRegEx(false);
        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion);
        context.setVariable(inputVariable, "onetwothree");
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals( (long) outs.size(), 1L );
        Assert.assertEquals(outs.get(0), "onetwothree");
    }

    @Test
    public void testSplitOnWhitespace() throws Exception {
        String w1 = "asdfjhasdfljkahd";
        String w2 = "h097y2,asdf\u382724,szcxv;:248y\\swegsfz";
        String w3 = "asdfasdflkeasdfasdf\u2827\u1827ahaiwuherf";
        String big = w1 + " \t \r   \n " + w2 + "  \t   \t\t  \n" + w3;
        context.setVariable(inputVariable, big);
        assertion.setSplitPattern("\\s+");

        ServerSplitAssertion ssa = new ServerSplitAssertion(assertion);
        AssertionStatus result = ssa.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable(outputVariable);
        Assert.assertTrue(outputObj instanceof List);
        List outs = (List) outputObj;
        for (Object outObj : outs)
            Assert.assertTrue(outObj instanceof String);
        Assert.assertEquals( (long) outs.size(), 3L );
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

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion);
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

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion);
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

        ServerSplitAssertion serverSplitAssertion = new ServerSplitAssertion(assertion);
        final AssertionStatus status = serverSplitAssertion.checkRequest(context);

        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Object variable = context.getVariable("output");
        List<String> coll = (List<String>) variable;
        //Java8 does not generate an empty element as a first character
        Assert.assertEquals("Wrong number of elements found", 13L, (long) coll.size() );

        for(int i = 0; i < value.length(); i++){
            Assert.assertEquals("Wrong value found", String.valueOf(value.charAt(i)), coll.get(i));
        }
   }

    /**
     * Test for existing behavior before enhancement for bug.
     */
    @Test
    @BugNumber(10898)
    public void testTrailingDefaultBehavior() throws Exception {
        SplitAssertion sa = new SplitAssertion();
        sa.setInputVariable("input");
        sa.setOutputVariable("output");
        sa.setSplitPattern(",");

        ServerSplitAssertion serverSa = new ServerSplitAssertion(sa);
        final PolicyEnforcementContext context1 = getContext();
        context1.setVariable("input", ",,,,,,");
        serverSa.checkRequest(context1);

        // When input consists solely of delimiter characters = empty list.
        Object output = context1.getVariable("output");
        System.out.println(output);
        List<String> outputList = (List<String>) output;
        Assert.assertTrue("List should be empty", outputList.isEmpty());

        // When first match is after a delimiter character = results in an empty string and subsequent matches.
        context1.setVariable("input", ", ,,,,,");
        serverSa.checkRequest(context1);
        output = context1.getVariable("output");
        System.out.println(output);
        outputList = (List<String>) output;

        Assert.assertEquals("Incorrect number of items found", 2, outputList.size());
        Assert.assertEquals("Incorrect value found", "", outputList.get(0));
        Assert.assertEquals("Incorrect value found", " ", outputList.get(1));

        // When a non empty match is found after a sequence of delimiter characters = output has an empty string for each delimiter.
        context1.setVariable("input", ", ,,, ,,");
        serverSa.checkRequest(context1);
        output = context1.getVariable("output");
        System.out.println(output);
        outputList = (List<String>) output;

        Assert.assertEquals("Incorrect number of items found", 5, outputList.size());
        Assert.assertEquals("Incorrect value found", "", outputList.get(0));
        Assert.assertEquals("Incorrect value found", " ", outputList.get(1));
        Assert.assertEquals("Incorrect value found", "", outputList.get(2));
        Assert.assertEquals("Incorrect value found", "", outputList.get(3));
        Assert.assertEquals("Incorrect value found", " ", outputList.get(4));
    }

    /**
     * Tests that empty strings are removed when configured.
     */
    @Test
    @BugNumber(10898)
    public void testTrailingRemoveEmpty() throws Exception {
        SplitAssertion sa = new SplitAssertion();
        sa.setInputVariable("input");
        sa.setOutputVariable("output");
        sa.setSplitPattern(",");
        sa.setIgnoreEmptyValues(true);

        ServerSplitAssertion serverSa = new ServerSplitAssertion(sa);
        final PolicyEnforcementContext context1 = getContext();
        context1.setVariable("input", ",,,,,,");
        serverSa.checkRequest(context1);

        // When input consists solely of delimiter characters = empty list.
        Object output = context1.getVariable("output");
        System.out.println(output);
        List<String> outputList = (List<String>) output;
        Assert.assertTrue("List should be empty", outputList.isEmpty());

        // When first match is after a delimiter character = empty strings ignored.
        context1.setVariable("input", ", ,,,,,");
        serverSa.checkRequest(context1);
        output = context1.getVariable("output");
        System.out.println(output);
        outputList = (List<String>) output;

        Assert.assertEquals("Incorrect number of items found", 1, outputList.size());
        Assert.assertEquals("Incorrect value found", " ", outputList.get(0));

        // When a non empty match is found after a sequence of delimiter characters = empty strings ignored.
        context1.setVariable("input", ", ,,, ,,");
        serverSa.checkRequest(context1);
        output = context1.getVariable("output");
        System.out.println(output);
        outputList = (List<String>) output;

        Assert.assertEquals("Incorrect number of items found", 2, outputList.size());
        Assert.assertEquals("Incorrect value found", " ", outputList.get(0));
        Assert.assertEquals("Incorrect value found", " ", outputList.get(1));
    }
}
