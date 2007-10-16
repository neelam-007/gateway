package com.l7tech.policy;

import com.l7tech.policy.variable.ExpandVariables;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map;

/**
 * Class ExpandVariablesTest.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class ExpandVariablesTest extends TestCase {
    /**
     * test <code>ExpandVariablesTest</code> constructor
     */
    public ExpandVariablesTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ExpandVariablesTest <code>TestCase</code>
     */
    public static Test suite() {
        return new TestSuite(ExpandVariablesTest.class);
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testSingleVariableExpand() throws Exception {
        Map variables = new HashMap();
        String value = "value_variable1";
        variables.put("var1", value);

        String inputMessage = "Blah message blah ${var1}";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = ExpandVariables.process(inputMessage, variables);
        assertTrue(processedMessage.indexOf(value) >= 0);
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testMultipleVariableExpand() throws Exception {
        Map variables = new HashMap();
        String value1 = "value_variable1";
        String value2 = "value_variable2";
        variables.put("var1", value1);
        variables.put("var2", value2);

        String inputMessage = "Blah message blah ${var1} and more blah ${var2}";
        String expectedOutputMessage = "Blah message blah value_variable1 and more blah value_variable2";
        String processedMessage = ExpandVariables.process(inputMessage, variables);
        assertTrue(processedMessage.indexOf(value1) >= 0);
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testSingleVariableNotFound() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        String value = "value_variable1";
        variables.put("var1", value);

        final String prefix = "Blah message blah ";
        String inputMessage = prefix + "${var2}";
        String out = ExpandVariables.process(inputMessage, variables);
        assertEquals(out, prefix);
    }

    public void testUnterminatedRef() throws Exception {
        String[] vars = ExpandVariables.getReferencedNames("${foo");
        assertEquals(vars.length, 0);
    }

    public void testMultivalueDelimiter() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new String[] { "bar", "baz"});

        String out1 = ExpandVariables.process("${foo}", vars);
        assertEquals(out1, "bar, baz"); // Default delimiter is ", "

        String out2 = ExpandVariables.process("<foo><val>" + "${foo|</val><val>}" + "</val></foo>", vars);
        assertEquals(out2, "<foo><val>bar</val><val>baz</val></foo>");

        String out3 = ExpandVariables.process("${foo|}", vars);
        assertEquals(out3, "barbaz");

        String out4 = ExpandVariables.process("${foo||}", vars);
        assertEquals(out4, "bar|baz");
    }

    public void testMultivalueSubscript() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("foo", new Object[] { "bar", "baz" });
        }};

        String out1 = ExpandVariables.process("${foo[0]}", vars);
        assertEquals(out1, "bar");

        String out2 = ExpandVariables.process("${foo[1]}", vars);
        assertEquals(out2, "baz");

        try {
            ExpandVariables.process("${foo[asdf]}", vars);
            fail("Should have thrown--non-numeric subscript");
        } catch (IllegalArgumentException e) {
        }

        try {
            ExpandVariables.process("${foo[-1]}", vars);
            fail("Should have thrown--negative subscript");
        } catch (IllegalArgumentException e) {
        }

    }

    /**
     * Test <code>ExpandVariablesTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
