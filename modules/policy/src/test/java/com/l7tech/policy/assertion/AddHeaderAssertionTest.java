package com.l7tech.policy.assertion;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AddHeaderAssertionTest {
    private AddHeaderAssertion assertion;
    private AssertionNodeNameFactory<AddHeaderAssertion> assertionNameFactory;

    @Before
    public void setup() {
        assertion = new AddHeaderAssertion();
        assertionNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
    }

    @Test
    public void getAssertionNameNoDecorate() {
        assertion.setHeaderName("foo");
        assertEquals("Manage Header", assertionNameFactory.getAssertionName(assertion, false));
    }

    @Test
    public void getAssertionNameAdd() {
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        assertEquals("Request: Add Header foo:bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameReplace() {
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        assertion.setRemoveExisting(true);
        assertEquals("Request: Add Header foo:bar (replace existing)", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertion.setHeaderName("foo");
        // remove existing should be ignored
        assertion.setRemoveExisting(true);
        assertEquals("Request: Remove Header(s) foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveMatchValue() {
        assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        // remove existing should be ignored
        assertion.setRemoveExisting(true);
        assertEquals("Request: Remove Header(s) foo:bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
