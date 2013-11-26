package com.l7tech.external.assertions.addorremovecookie;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
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
        assertEquals("Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);
        assertion.setName("foo");
        assertEquals("Remove Cookie foo", assertionNameFactory.getAssertionName(assertion, true));
    }
}
