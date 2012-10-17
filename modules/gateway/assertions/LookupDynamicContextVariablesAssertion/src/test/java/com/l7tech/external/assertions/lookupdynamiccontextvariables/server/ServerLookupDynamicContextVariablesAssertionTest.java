package com.l7tech.external.assertions.lookupdynamiccontextvariables.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Test the LookupDynamicContextVariablesAssertion.
 */
public class ServerLookupDynamicContextVariablesAssertionTest {

    private LookupDynamicContextVariablesAssertion assertion;

    private PolicyEnforcementContext pec;

    private ServerLookupDynamicContextVariablesAssertion serverAssertion;

    @Before
    public void setUp(){
        assertion = new LookupDynamicContextVariablesAssertion();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument("<request />"));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument("<response />"));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        pec.setVariable("foo", "bar");
        pec.setVariable("sugar", "klondike");
        pec.setVariable("klondike.bar", "goodness");
        serverAssertion = new ServerLookupDynamicContextVariablesAssertion(assertion);
    }

    @Test
    public void testValidSource(){
        try {
            assertion.setTargetDataType(DataType.STRING);
            assertion.setSourceVariable("${sugar}.${foo}");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.FOUND_SUFIX));
            Assert.assertEquals("goodness", pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX));
            Assert.assertEquals(Boolean.FALSE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.MULTIVALUED_SUFIX));
            Assert.assertEquals(AssertionStatus.NONE, actual);
        } catch (Exception e) {
            Assert.fail("testValidSource() failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidSource(){
        try {
            assertion.setTargetDataType(DataType.STRING);
            assertion.setSourceVariable("${sugar}.${foo22222}");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, actual);
            Assert.assertNull(pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX));
            Assert.fail(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX + " should NOT exist!");
        } catch (Exception e) {

        }
    }

    @Test
    public void testInvalidSourceEntry(){
        try {
            assertion.setTargetDataType(DataType.STRING);
            assertion.setSourceVariable("${sugar}.${foo22222");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, actual);
            Assert.assertNull(pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX));
            Assert.fail(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX + " should NOT exist!");
        } catch (Exception e) {

        }
    }

    @Test
    public void testArraySubscriptExpression(){
        try {
            assertion.setTargetDataType(DataType.STRING);
            pec.setVariable("output", new String[]{"one", "two", "three"});
            pec.setVariable("two", "Looked up value");
            assertion.setSourceVariable("${output[1]}");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.FOUND_SUFIX));
            Assert.assertEquals("Looked up value", pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX));
        } catch (Exception e) {
            Assert.fail("testExpression() failed: " + e.getMessage());
        }
    }

    @Test
    public void testMissingSourceVariable(){
        assertion.setSourceVariable(null);
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }

    @Test
    public void testMissingTargetVariable(){
        assertion.setTargetOutputVariablePrefix(null);
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }

    @Test
    public void testUnsupportedDataType(){
        assertion.setSourceVariable("unsupported");
        assertion.setTargetOutputVariablePrefix("output");
        pec.setVariable("unsupported", new StringReader("unsupported1234"));
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }

    @Test
    public void testUnsupportedDataTypeTarget(){
        assertion.setSourceVariable("foo");
        assertion.setTargetOutputVariablePrefix("output");
        assertion.setTargetDataType(DataType.CLOB);
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }

    @Test
    public void testDataTypeMismatch(){
        assertion.setSourceVariable("foo");
        assertion.setTargetOutputVariablePrefix("output");
        assertion.setTargetDataType(DataType.DATE_TIME);
        pec.setVariable("foo", true);
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);

        assertion.setTargetDataType(DataType.CERTIFICATE);
        pec.setVariable("foo", new Message());
        actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);

        assertion.setTargetDataType(DataType.STRING);
        pec.setVariable("foo", true);
        actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, actual);

        assertion.setTargetDataType(DataType.STRING);
        pec.setVariable("foo", DataType.FLOAT);
        actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }

    @Test
    public void testMultiValuedContextVariables(){
        try {
            assertion.setTargetDataType(DataType.STRING);
            // test array
            String[] val = new String[]{"one", "two", "three"};
            pec.setVariable("output", val);

            assertion.setSourceVariable("output");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.FOUND_SUFIX));
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.MULTIVALUED_SUFIX));
            Assert.assertEquals(val, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX));

            //test collection
            List<String> values = Arrays.asList(val);
            pec.setVariable("output", values);

            assertion.setSourceVariable("output");
            assertion.setTargetOutputVariablePrefix("targetOutput");
            actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.FOUND_SUFIX));
            Assert.assertEquals(Boolean.TRUE, pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.MULTIVALUED_SUFIX));

            //when ExpandVariables is called, it would have changed Collection/List to an Object[] ExpandVariables#getAndFilter(...)
            Object[] actualOutput = (Object[]) pec.getVariable(assertion.getTargetOutputVariablePrefix() + LookupDynamicContextVariablesAssertion.OUTPUT_SUFIX);

            Assert.assertNotNull(actualOutput);
            Assert.assertEquals(values.size(), actualOutput.length);
            for(int i = 0; i < values.size(); i++){
                Assert.assertEquals(values.get(i), actualOutput[i]);
            }
        } catch (Exception e) {
            Assert.fail("testExpression() failed: " + e.getMessage());
        }
    }

    @Test
    public void testDateLookup(){
        assertion.setSourceVariable("foo");
        assertion.setTargetOutputVariablePrefix("output");
        assertion.setTargetDataType(DataType.DATE_TIME);
        pec.setVariable("foo", new Date());
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, actual);


    }
}


