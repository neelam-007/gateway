package com.l7tech.external.assertions.managecookie;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.util.NameValuePair;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.*;
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertEquals("Request: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveNonRegexSingleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.REMOVE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "foo", false));
        assertEquals("Request: Remove Cookie(s) if name equals foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveNonRegexMultipleCriteria() {
        assertion.setOperation(Operation.REMOVE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "foo", false));
        assertion.getCookieCriteria().put(DOMAIN, new ManageCookieAssertion.CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieCriteria().put(PATH, new ManageCookieAssertion.CookieCriteria(PATH, "/", false));
        String assertionName = assertionNameFactory.getAssertionName(assertion, true);
        //rather than check the entire string we should check if the assertion name contains certain criteria
        assertTrue(assertionName.startsWith("Request: Remove Cookie(s) if"));
        assertTrue(assertionName.contains("name equals foo"));
        assertTrue(assertionName.contains("path equals /"));
        assertTrue(assertionName.contains("domain equals localhost"));

        //assertEquals("Request: Remove Cookie(s) if name equals foo and path equals / and domain equals localhost", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveRegexSingleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.REMOVE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "f.*", true));
        assertEquals("Request: Remove Cookie(s) if name matches f.*", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveRegexMultipleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.REMOVE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "f.*", true));
        assertion.getCookieCriteria().put(DOMAIN, new ManageCookieAssertion.CookieCriteria(DOMAIN, "l.*", true));
        assertion.getCookieCriteria().put(PATH, new ManageCookieAssertion.CookieCriteria(PATH, "p.*", true));

        String assertionName = assertionNameFactory.getAssertionName(assertion, true);
        //rather than check the entire string we should check if the assertion name contains certain criteria
        assertTrue(assertionName.startsWith("Request: Remove Cookie(s) if"));
        assertTrue(assertionName.contains("name matches f.*"));
        assertTrue(assertionName.contains("path matches p.*"));
        assertTrue(assertionName.contains("domain matches l.*"));
//        assertEquals("Request: Remove Cookie(s) if name matches f.* and path matches p.* and domain matches l.*", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameUpdateNonRegexSingleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "foo", false));
        assertEquals("Request: Update Cookie(s) if name equals foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameUpdateNonRegexMultipleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "foo", false));
        assertion.getCookieCriteria().put(DOMAIN, new ManageCookieAssertion.CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieCriteria().put(PATH, new ManageCookieAssertion.CookieCriteria(PATH, "/", false));

        String assertionName = assertionNameFactory.getAssertionName(assertion, true);
        //rather than check the entire string we should check if the assertion name contains certain criteria
        assertTrue(assertionName.startsWith("Request: Update Cookie(s) if"));
        assertTrue(assertionName.contains("name equals foo"));
        assertTrue(assertionName.contains("path equals /"));
        assertTrue(assertionName.contains("domain equals localhost"));
    }

    @Test
    public void getAssertionNameUpdateRegexSingleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "f.*", true));
        assertEquals("Request: Update Cookie(s) if name matches f.*", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameUpdateRegexMultipleCriteria() {
        assertion.setOperation(ManageCookieAssertion.Operation.UPDATE);
        assertion.getCookieCriteria().put(NAME, new ManageCookieAssertion.CookieCriteria(NAME, "f.*", true));
        assertion.getCookieCriteria().put(DOMAIN, new ManageCookieAssertion.CookieCriteria(DOMAIN, "l.*", true));
        assertion.getCookieCriteria().put(PATH, new ManageCookieAssertion.CookieCriteria(PATH, "p.*", true));

        String assertionName = assertionNameFactory.getAssertionName(assertion, true);
        //rather than check the entire string we should check if the assertion name contains certain criteria
        assertTrue(assertionName.startsWith("Request: Update Cookie(s) if"));
        assertTrue(assertionName.contains("name matches f.*"));
        assertTrue(assertionName.contains("path matches p.*"));
        assertTrue(assertionName.contains("domain matches l.*"));
    }

    @Test
    public void getAssertionNameAddOrReplace() {
        assertion.setOperation(ManageCookieAssertion.Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertEquals("Request: Add or Replace Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameResponse() {
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertEquals("Response: Add Cookie foo=bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
