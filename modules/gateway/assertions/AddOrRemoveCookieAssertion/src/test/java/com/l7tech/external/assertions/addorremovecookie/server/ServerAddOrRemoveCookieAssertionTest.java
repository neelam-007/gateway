package com.l7tech.external.assertions.addorremovecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
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
        assertFalse(cookie.isNew());
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
        assertFalse(cookie.isNew());
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
    public void addCookieNameResolvesToEmpty() throws Exception {
        assertion.setName("${name}");
        assertion.setValue("bar");

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(context.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieNameAlreadyExists() throws Exception {
        context.addCookie(new HttpCookie("foo", "existingValue", 0, "/", "localhost", 60, true, "test"));
        assertion.setName("foo");
        assertion.setValue("newValue");

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("existingValue", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ALREADY_EXISTS));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void removeCookie() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.setName("foo");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(context.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromContextVar() throws Exception {
        context.setVariable("name", "foo");
        context.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.setName("${name}");
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

    @Test
    public void removeCookieFromResponse() throws Exception {
        final HttpCookie responseCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", true);
        context.addCookie(responseCookie);
        assertion.setName("foo");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(context.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromResponseDoesNotRemoveRequestCookies() throws Exception {
        final HttpCookie requestCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
        context.addCookie(requestCookie);
        assertion.setName("foo");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        assertEquals("foo", context.getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_NOT_FOUND));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieNotFound() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.setName("notFound");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        assertEquals("foo", context.getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_NOT_FOUND));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieNameResolvesToEmpty() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.setName("${name}");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        assertEquals("foo", context.getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void addCookieToResponse() throws Exception {
        assertion.setTarget(TargetMessageType.RESPONSE);
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
        assertTrue(cookie.isNew());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    /**
     * For now we do not support TargetMessageType.OTHER until we move cookies to be stored on the message instead of the PEC.
     */
    @Test(expected = PolicyAssertionException.class)
    public void otherTargetMessageType() throws Exception {
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("testMessage");
        assertion.setName("foo");
        assertion.setValue("bar");

        try {
            new ServerAddOrRemoveCookieAssertion(assertion).checkRequest(context);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Unsupported target: testMessage", e.getMessage());
            throw e;
        }
    }

    @Test
    public void updateCookie() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.setName("foo");
        assertion.setValue(modified);
        assertion.setDomain(modified);
        assertion.setCookiePath(modified);
        assertion.setComment(modified);
        assertion.setVersion(1);
        assertion.setMaxAge("99");
        assertion.setSecure(true);
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals(modified, cookie.getCookieValue());
        assertEquals(modified, cookie.getPath());
        assertEquals(modified, cookie.getDomain());
        assertEquals(modified, cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(99, cookie.getMaxAge());
        assertTrue(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieFromContextVars() throws Exception {
        final String modified = "modified";
        context.setVariable("name", "foo");
        context.setVariable("value", modified);
        context.setVariable("domain", modified);
        context.setVariable("path", modified);
        context.setVariable("comment", modified);
        context.setVariable("maxAge", 99);
        context.addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        assertion.setName("${name}");
        assertion.setValue("${value}");
        assertion.setDomain("${domain}");
        assertion.setCookiePath("${path}");
        assertion.setComment("${comment}");
        assertion.setVersion(1);
        assertion.setMaxAge("${maxAge}");
        assertion.setSecure(true);
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals(modified, cookie.getCookieValue());
        assertEquals(modified, cookie.getPath());
        assertEquals(modified, cookie.getDomain());
        assertEquals(modified, cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(99, cookie.getMaxAge());
        assertTrue(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieNameResolvesToEmpty() throws Exception {
        context.addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        assertion.setName("${name}");
        assertion.setValue("bar");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FAILED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieDoesNotExist() throws Exception {
        assertion.setName("foo");
        assertion.setValue("bar");
        assertion.setOperation(AddOrRemoveCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerAddOrRemoveCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_NOT_FOUND));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    private ServerAddOrRemoveCookieAssertion configureServerAssertion(final ServerAddOrRemoveCookieAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
        return serverAssertion;
    }
}
