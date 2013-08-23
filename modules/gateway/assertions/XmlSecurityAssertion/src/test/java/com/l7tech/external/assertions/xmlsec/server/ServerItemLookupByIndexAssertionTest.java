package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.ItemLookupByIndexAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test case for item lookup by index.
 */
public class ServerItemLookupByIndexAssertionTest {
    @Test
    @BugNumber(10843)
    public void testFloatingPointStringIndex() throws Exception {
        ItemLookupByIndexAssertion ass = new ItemLookupByIndexAssertion();
        ass.setMultivaluedVariableName("multivar");
        ass.setOutputVariableName("out");
        ass.setIndexValue("${i}");
        ServerItemLookupByIndexAssertion sass = new ServerItemLookupByIndexAssertion(ass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("multivar", new String[] { "foo", "bar", "baz", "blat" });
        context.setVariable("i", "2.1");

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        assertEquals("baz", context.getVariable("out"));
    }

    @Test
    public void testNotMultiValue() throws Exception {
        ItemLookupByIndexAssertion ass = new ItemLookupByIndexAssertion();
        ass.setMultivaluedVariableName("singleVar");
        ass.setOutputVariableName("out");
        ass.setIndexValue("${i}");
        ServerItemLookupByIndexAssertion sass = new ServerItemLookupByIndexAssertion(ass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("singleVar", "test");
        context.setVariable("i", "0");

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test(expected= AssertionStatusException.class)
    public void testWrongIndexSingleValue() throws Exception {
        ItemLookupByIndexAssertion ass = new ItemLookupByIndexAssertion();
        ass.setMultivaluedVariableName("singleVar");
        ass.setOutputVariableName("out");
        ass.setIndexValue("${i}");
        ServerItemLookupByIndexAssertion sass = new ServerItemLookupByIndexAssertion(ass);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("singleVar", "test");
        context.setVariable("i", "1");

        AssertionStatus result = sass.checkRequest(context);
    }
}
