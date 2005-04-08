package com.l7tech.policy;

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
        TestSuite suite = new TestSuite(ExpandVariablesTest.class);
        return suite;
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

        ExpandVariables expander = new ExpandVariables(variables);
        String inputMessage = "Blah message blah ${var1}";
        String expectedOutputMessage = "Blah message blah value_variable1";
        String processedMessage = expander.process(inputMessage);
        assertTrue(processedMessage.contains(value));
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testMultipleVariableExpand() throws Exception {
        Map variables = new HashMap();
        String value1 = "value_variable1";
        String value2 = "value_variable2";
        variables.put("var1", value1);
        variables.put("var2", value2);

        ExpandVariables expander = new ExpandVariables(variables);
        String inputMessage = "Blah message blah ${var1} and more blah ${var2}";
        String expectedOutputMessage = "Blah message blah value_variable1 and more blah value_variable2";
        String processedMessage = expander.process(inputMessage);
        assertTrue(processedMessage.contains(value1));
        assertEquals(processedMessage, expectedOutputMessage);
    }

    public void testSingleVariableNotFound() throws Exception {
          Map variables = new HashMap();
          String value = "value_variable1";
          variables.put("var1", value);

          ExpandVariables expander = new ExpandVariables(variables);
          String inputMessage = "Blah message blah ${var2}";
          String processedMessage = null;
          try {
              expander.process(inputMessage);
              fail("the "+ExpandVariables.VariableNotFoundException.class+" expected");
          } catch (ExpandVariables.VariableNotFoundException e) {
          }
      }

    /**
     * Test <code>ExpandVariablesTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
