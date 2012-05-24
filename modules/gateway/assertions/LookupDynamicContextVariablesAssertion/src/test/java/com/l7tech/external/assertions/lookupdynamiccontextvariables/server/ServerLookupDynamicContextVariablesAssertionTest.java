package com.l7tech.external.assertions.lookupdynamiccontextvariables.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
            assertion.setSourceVariable("${sugar}.${foo}");
            assertion.setTargetOutputVariable("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals("goodness", pec.getVariable(assertion.getTargetOutputVariable()));
            Assert.assertEquals(AssertionStatus.NONE, actual);
        } catch (Exception e) {
            Assert.fail("testValidSource() failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidSource(){
        try {
            assertion.setSourceVariable("${sugar}.${foo22222}");
            assertion.setTargetOutputVariable("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertNull(pec.getVariable(assertion.getTargetOutputVariable()));
            Assert.fail(assertion.getTargetOutputVariable() + " should NOT exist!");
        } catch (Exception e) {

        }
    }

    @Test
    public void testInvalidSourceEntry(){
        try {
            assertion.setSourceVariable("${sugar}.${foo22222");
            assertion.setTargetOutputVariable("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertNull(pec.getVariable(assertion.getTargetOutputVariable()));
            Assert.fail(assertion.getTargetOutputVariable() + " should NOT exist!");
        } catch (Exception e) {

        }
    }

    @Test
    public void testArraySubscriptExpression(){
        try {
            pec.setVariable("output", new String[]{"one", "two", "three"});
            pec.setVariable("two", "Looked up value");
            assertion.setSourceVariable("${output[1]}");
            assertion.setTargetOutputVariable("targetOutput");
            AssertionStatus actual = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, actual);
            Assert.assertEquals("Looked up value", pec.getVariable(assertion.getTargetOutputVariable()));
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
        assertion.setTargetOutputVariable(null);
        AssertionStatus actual = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, actual);
    }
}


