package com.l7tech.external.assertions.managecookie;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.TargetMessageType;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.NAME;
import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.VALUE;
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
        assertion.getCookieAttributes().put(NAME, new ManageCookieAssertion.CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new ManageCookieAssertion.CookieAttribute(VALUE, "bar", false));
        assertEquals("Request: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(ManageCookieAssertion.Operation.REMOVE);
        assertEquals("Request: Remove Cookie", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameUpdate() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertEquals("Request: Update Cookie", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameAddOrReplace() {
        assertion.setOperation(ManageCookieAssertion.Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new ManageCookieAssertion.CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new ManageCookieAssertion.CookieAttribute(VALUE, "bar", false));
        assertEquals("Request: Add or Replace Cookie foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameResponse() {
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.getCookieAttributes().put(NAME, new ManageCookieAssertion.CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new ManageCookieAssertion.CookieAttribute(VALUE, "bar", false));
        assertEquals("Response: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
