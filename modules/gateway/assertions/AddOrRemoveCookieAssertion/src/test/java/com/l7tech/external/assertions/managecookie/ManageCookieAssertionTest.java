package com.l7tech.external.assertions.managecookie;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.TargetMessageType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ManageCookieAssertionTest {
    private ManageCookieAssertion assertion;
    private AssertionNodeNameFactory<ManageCookieAssertion> assertionNameFactory;

    @Before
    public void setup() {
        assertion = new ManageCookieAssertion();
        assertionNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
    }

    @Test
    public void getAssertionNameDoNotDecorate() {
        assertEquals("Manage Cookie", assertionNameFactory.getAssertionName(assertion, false));
    }

    @Test
    public void getAssertionNameAdd() {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertEquals("Request: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(ManageCookieAssertion.Operation.REMOVE);
        assertion.setName("foo");
        assertEquals("Request: Remove Cookie foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameUpdate() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertion.setName("foo");
        assertEquals("Request: Update Cookie foo", assertionNameFactory.getAssertionName(assertion, true));
    }


    @Test
    public void getAssertionNameResponse() {
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setName("foo");
        assertion.setValue("bar");
        assertEquals("Response: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
