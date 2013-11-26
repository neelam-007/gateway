package com.l7tech.external.assertions.addorremovecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerAddOrRemoveCookieAssertionTest {
    private AddOrRemoveCookieAssertion assertion;
    private PolicyEnforcementContext context;
    private TestAudit testAudit;

    @Before
    public void setup() {
        assertion = new AddOrRemoveCookieAssertion();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        testAudit = new TestAudit();
    }

    @Test
    public void addBasicCookie() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(1, cookie.getVersion());
        assertNull(cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookie() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setCookiePath("/test");
        assertion.setDomain("localhost");
        assertion.setMaxAge("60");
        assertion.setComment("test comment");
        assertion.setSecure(true);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(1, cookie.getVersion());
        assertEquals("/test", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("test comment", cookie.getComment());
        assertTrue(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieValuesFromContextVars() throws Exception {
        context.setVariable("name", "foo");
        context.setVariable("value", "bar");
        context.setVariable("version", "1");
        context.setVariable("path", "/test");
        context.setVariable("domain", "localhost");
        context.setVariable("maxAge", "60");
        context.setVariable("comment", "test comment");
        assertion.setName("${name}");
        assertion.setValue("${value}");
        assertion.setCookiePath("${path}");
        assertion.setDomain("${domain}");
        assertion.setMaxAge("${maxAge}");
        assertion.setComment("${comment}");

        assertEquals(AssertionStatus.NONE, new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(1, cookie.getVersion());
        assertEquals("/test", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("test comment", cookie.getComment());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void addCookieEmptyValues() throws Exception {
        assertion.setName("foo");
        assertion.setValue("");
        assertion.setCookiePath("");
        assertion.setDomain("");
        assertion.setMaxAge("");
        assertion.setComment("");

        assertEquals(AssertionStatus.NONE, new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        // empty value is valid
        assertEquals("", cookie.getCookieValue());
        assertEquals(1, cookie.getVersion());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getPath());
        assertNull(cookie.getDomain());
        assertNull(cookie.getComment());
    }

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNullName() throws Exception {
        assertion.setValue("someValue");
        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Cookie name is null", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNullValue() throws Exception {
        assertion.setName("foo");
        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Cookie value is null", e.getMessage());
            throw e;
        }
    }

    @Test
    public void addCookieMaxAgeNotNumeric() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setMaxAge("notNumeric");

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_MAX_AGE));
    }

    @Test
    public void removeCookie() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        context.addCookie(new HttpCookie("foo", "bar2", 1, "/", "localhost2"));
        assertion.setName("foo");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(context.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void removeCookieNullName() throws Exception {
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);
        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Cookie name is null", e.getMessage());
            throw e;
        }
    }

    private ServerAddOrRemoveCookieAssertion configureServerAssertion(final ServerAddOrRemoveCookieAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
        return serverAssertion;
    }
}
