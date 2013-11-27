package com.l7tech.external.assertions.addorremovecookie;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.TargetMessageType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AddOrRemoveCookieAssertionTest {
    private AddOrRemoveCookieAssertion assertion;
    private AssertionNodeNameFactory<AddOrRemoveCookieAssertion> assertionNameFactory;

    @Before
    public void setup() {
        assertion = new AddOrRemoveCookieAssertion();
        assertionNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
    }

    @Test
    public void getAssertionNameDoNotDecorate() {
        assertEquals("Add or Remove Cookie", assertionNameFactory.getAssertionName(assertion, false));
    }

    @Test
    public void getAssertionNameAdd() {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertEquals("Request: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);
        assertion.setName("foo");
        assertEquals("Request: Remove Cookie foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameResponse() {
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setName("foo");
        assertion.setValue("bar");
        assertEquals("Response: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
