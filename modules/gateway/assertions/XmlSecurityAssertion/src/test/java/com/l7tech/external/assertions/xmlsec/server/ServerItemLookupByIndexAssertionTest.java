package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.ItemLookupByIndexAssertion;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

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
        context.setVariable("multivar", new String[]{"foo", "bar", "baz", "blat"});
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

    @Test(expected = AssertionStatusException.class)
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

    @Test
    public void requestHeadersContextVariable() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("request.http.allheadervalues");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final Message request = new Message();
        request.getHeadersKnob().addHeader("foo", "bar", HeadersKnob.HEADER_TYPE_HTTP);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        assertEquals(AssertionStatus.NONE, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
        assertEquals("foo:bar", context.getVariable("out"));
    }

    @Test
    public void requestHeadersContextVariableNone() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("request.http.allheadervalues");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        assertEquals(AssertionStatus.SERVER_ERROR, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
    }

    @BugId("SSG-8482")
    @Test
    public void customMessageHeadersContextVariable() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("msg.http.allheadervalues");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        final Message msg = context.getOrCreateTargetMessage(new MessageTargetableSupport("msg"), false);
        msg.getHeadersKnob().addHeader("foo", "bar", HeadersKnob.HEADER_TYPE_HTTP);

        assertEquals(AssertionStatus.NONE, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
        assertEquals("foo:bar", context.getVariable("out"));
    }

    @Test
    public void customMessageHeadersContextVariableNone() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("msg.http.allheadervalues");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.getOrCreateTargetMessage(new MessageTargetableSupport("msg"), false);

        assertEquals(AssertionStatus.SERVER_ERROR, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
    }

    @Test
    public void multiValuedVariableDoesNotExist() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("doesnotexist");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        assertEquals(AssertionStatus.SERVER_ERROR, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
    }

    @Test
    public void multiValuedVariableEmpty() throws Exception {
        final ItemLookupByIndexAssertion assertion = new ItemLookupByIndexAssertion();
        assertion.setMultivaluedVariableName("empty");
        assertion.setOutputVariableName("out");
        assertion.setIndexValue("0");
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("empty", Collections.emptyList());

        assertEquals(AssertionStatus.SERVER_ERROR, new ServerItemLookupByIndexAssertion(assertion).checkRequest(context));
    }
}
