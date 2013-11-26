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
        assertion.setVersion("1");

        assertEquals(AssertionStatus.NONE, new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context));
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(1, cookie.getVersion());
        assertNull(cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void addCookie() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setVersion("1");
        assertion.setCookiePath("/test");
        assertion.setDomain("localhost");
        assertion.setMaxAge("60");
        assertion.setComment("test comment");
        assertion.setSecure(true);

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
        assertTrue(cookie.isSecure());
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
        assertion.setVersion("${version}");
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

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNullName() throws Exception {
        assertion.setValue("someValue");
        assertion.setVersion("1");
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
        assertion.setVersion("1");
        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Cookie value is null", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNullVersion() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Cookie version is null", e.getMessage());
            throw e;
        }
    }

    @Test
    public void addCookieVersionNotNumeric() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setVersion("notNumeric");

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
    }

    @Test
    public void addCookieMaxAgeNotNumeric() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setVersion("1");
        assertion.setMaxAge("notNumeric");

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_MAX_AGE));
    }

    private ServerAddOrRemoveCookieAssertion configureServerAssertion(final ServerAddOrRemoveCookieAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
        return serverAssertion;
    }
}
