package com.l7tech.external.assertions.managecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.*;
import static org.junit.Assert.*;

public class ServerManageCookieAssertionTest {
    private static final String STARTS_WITH_F = "f.*";
    private static final String STARTS_WITH_L = "l.*";
    private static final String STARTS_WITH_FORWARD_SLASH = "\\/.*";
    private ManageCookieAssertion assertion;
    private PolicyEnforcementContext context;
    private TestAudit testAudit;
    private Message request;
    private Message response;

    @Before
    public void setup() {
        assertion = new com.l7tech.external.assertions.managecookie.ManageCookieAssertion();
        request = new Message();
        response = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        testAudit = new TestAudit();
    }

    @Test
    public void addBasicCookie() throws Exception {
        assertion.getCookieAttributes().put(NAME, new ManageCookieAssertion.CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new ManageCookieAssertion.CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new ManageCookieAssertion.CookieAttribute(VALUE, "bar", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
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
        assertion.getCookieAttributes().put(NAME, new ManageCookieAssertion.CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new ManageCookieAssertion.CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new ManageCookieAssertion.CookieAttribute(VALUE, "bar", false));
        assertion.getCookieAttributes().put(PATH, new ManageCookieAssertion.CookieAttribute(PATH, "/test", false));
        assertion.getCookieAttributes().put(DOMAIN, new ManageCookieAssertion.CookieAttribute(DOMAIN, "localhost", false));
        assertion.getCookieAttributes().put(MAX_AGE, new ManageCookieAssertion.CookieAttribute(MAX_AGE, "60", false));
        assertion.getCookieAttributes().put(COMMENT, new ManageCookieAssertion.CookieAttribute(COMMENT, "test comment", false));
        assertion.getCookieAttributes().put(SECURE, new ManageCookieAssertion.CookieAttribute(SECURE, "true", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
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
        context.setVariable("version", "5");
        context.setVariable("path", "/test");
        context.setVariable("domain", "localhost");
        context.setVariable("maxAge", "60");
        context.setVariable("comment", "test comment");
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "${value}", false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, "${path}", false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, "${domain}", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "${maxAge}", false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, "${comment}", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "${version}", false));

        assertEquals(AssertionStatus.NONE, new ServerManageCookieAssertion(assertion).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(5, cookie.getVersion());
        assertEquals("/test", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("test comment", cookie.getComment());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void addCookieEmptyValues() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "", false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, "", false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, "", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "", false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, "", false));

        assertEquals(AssertionStatus.NONE, new ServerManageCookieAssertion(assertion).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
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
    public void addCookieNoName() throws Exception {
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "someValue", false));
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie name", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNoValue() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie value", e.getMessage());
            throw e;
        }
    }

    @Test
    public void addCookieMaxAgeNotNumeric() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "notNumeric", false));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_MAX_AGE));
    }

    @Test
    public void addCookieNameResolvesToEmpty() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieAlreadyExists() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "existingValue", 0, "/", "localhost", 60, true, "test"));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, "localhost", false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, "/", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "newValue", false));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("existingValue", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ALREADY_EXISTS));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void addCookieNoVersion() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie version", e.getMessage());
            throw e;
        }
    }

    @Test
    public void addCookieVersionResolvesToEmpty() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "${version}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieVersionNotNumeric() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "notNumeric", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void removeCookieByName() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByDomain() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByNameDomainAndPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("shouldNotBeRemoved", "shouldNotBeRemoved", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("shouldNotBeRemoved", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromNameContextVar() throws Exception {
        context.setVariable("name", "foo");
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromDomainContextVar() throws Exception {
        context.setVariable("domain", "localhost");
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "${domain}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromPathContextVar() throws Exception {
        context.setVariable("path", "/");
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${path}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByNameRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 1, "/", "localhost2"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, STARTS_WITH_F, true));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByDomainRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 1, "/", "localhost2"));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, STARTS_WITH_L, true));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByPathRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 1, "/2", "localhost2"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, STARTS_WITH_FORWARD_SLASH, true));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void removeCookieNoCriteria() throws Exception {
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("No cookie criteria specified for remove cookie", e.getMessage());
            throw e;
        }
    }

    @Test
    public void removeCookieFromResponse() throws Exception {
        final HttpCookie responseCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        response.getHttpCookiesKnob().addCookie(responseCookie);
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(response.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromResponseDoesNotRemoveRequestCookies() throws Exception {
        final HttpCookie requestCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        request.getHttpCookiesKnob().addCookie(requestCookie);
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        assertEquals("foo", request.getHttpCookiesKnob().getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIES_NOT_MATCHED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieNotFound() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "notFound", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        assertEquals("foo", request.getHttpCookiesKnob().getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIES_NOT_MATCHED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieNameResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        assertEquals("foo", request.getHttpCookiesKnob().getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookiePathResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${path}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        assertEquals("foo", request.getHttpCookiesKnob().getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieDomainResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "${domain}", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        assertEquals("foo", request.getHttpCookiesKnob().getCookies().iterator().next().getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void addCookieToResponse() throws Exception {
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, response.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = response.getHttpCookiesKnob().getCookies().iterator().next();
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
    public void addCookieToOtherTargetMessageType() throws Exception {
        context.getOrCreateTargetMessage(new MessageTargetableSupport("testMessage"), false);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("testMessage");
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final Message testMessage = (Message) context.getVariable("testMessage");
        assertEquals(1, testMessage.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = testMessage.getHttpCookiesKnob().getCookies().iterator().next();
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
    public void updateCookieByName() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals(modified, cookie.getCookieName());
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
    public void updateCookieByDomain() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals(modified, cookie.getCookieName());
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
    public void updateCookieByPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals(modified, cookie.getCookieName());
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
    public void updateCookieByNameDomainAndPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals(modified, cookie.getCookieName());
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "${value}", false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, "${domain}", false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, "${path}", false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, "${comment}", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "${maxAge}", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
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
    public void updateCookieByNameRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, STARTS_WITH_F, true));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "", true));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(2, request.getHttpCookiesKnob().getCookies().size());
        for (final HttpCookie cookie : request.getHttpCookiesKnob().getCookies()) {
            assertTrue(cookie.getCookieName().startsWith("foo"));
            assertEquals(modified, cookie.getCookieValue());
            assertEquals(modified, cookie.getPath());
            assertEquals(modified, cookie.getDomain());
            assertEquals(modified, cookie.getComment());
            assertEquals(1, cookie.getVersion());
            assertEquals(99, cookie.getMaxAge());
            assertTrue(cookie.isSecure());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByDomainRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, STARTS_WITH_L, true));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "", true));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(2, request.getHttpCookiesKnob().getCookies().size());
        for (final HttpCookie cookie : request.getHttpCookiesKnob().getCookies()) {
            assertTrue(cookie.getCookieName().startsWith("foo"));
            assertEquals(modified, cookie.getCookieValue());
            assertEquals(modified, cookie.getPath());
            assertEquals(modified, cookie.getDomain());
            assertEquals(modified, cookie.getComment());
            assertEquals(1, cookie.getVersion());
            assertEquals(99, cookie.getMaxAge());
            assertTrue(cookie.isSecure());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByPathRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, STARTS_WITH_FORWARD_SLASH, true));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "", true));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "99", false));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, "true", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(2, request.getHttpCookiesKnob().getCookies().size());
        for (final HttpCookie cookie : request.getHttpCookiesKnob().getCookies()) {
            assertTrue(cookie.getCookieName().startsWith("foo"));
            assertEquals(modified, cookie.getCookieValue());
            assertEquals(modified, cookie.getPath());
            assertEquals(modified, cookie.getDomain());
            assertEquals(modified, cookie.getComment());
            assertEquals(1, cookie.getVersion());
            assertEquals(99, cookie.getMaxAge());
            assertTrue(cookie.isSecure());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieNameResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar2", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookiePathResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${path}", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar2", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieDomainResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${domain}", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar2", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieInvalidVersion() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "invalidVersion", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieInvalidMaxAge() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VALUE, "1", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "invalidMaxAge", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_MAX_AGE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieUseExistingValues() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test"));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", true));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, modified, true));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, modified, true));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, modified, true));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, modified, true));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, modified, true));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, modified, true));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, modified, true));
        assertion.getCookieAttributes().put(SECURE, new CookieAttribute(SECURE, modified, true));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("/", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("test", cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(60, cookie.getMaxAge());
        assertFalse(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieDoesNotExist() throws Exception {
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIES_NOT_MATCHED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void updateCookieNoCriteria() throws Exception {
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.setOperation(Operation.UPDATE);
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("No cookie criteria specified for update cookie", e.getMessage());
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void updateCookieNoAttributes() throws Exception {
        assertion.setOperation(Operation.UPDATE);
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("No cookie attributes specified for update cookie", e.getMessage());
            throw e;
        }
    }

    @Test
    public void addOrReplaceCookieDoesNotExist() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "0", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final Set<HttpCookie> cookies = request.getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void addOrReplaceCookieAlreadyExists() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "originalValue", 1, null, null, -1, false, null));
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "newValue", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "0", false));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final Set<HttpCookie> cookies = request.getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("newValue", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void addOrReplaceCookieValuesFromContextVars() throws Exception {
        context.setVariable("name", "foo");
        context.setVariable("value", "bar");
        context.setVariable("version", "5");
        context.setVariable("path", "/test");
        context.setVariable("domain", "localhost");
        context.setVariable("maxAge", "60");
        context.setVariable("comment", "test comment");
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "${value}", false));
        assertion.getCookieAttributes().put(PATH, new CookieAttribute(PATH, "${path}", false));
        assertion.getCookieAttributes().put(DOMAIN, new CookieAttribute(DOMAIN, "${domain}", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "${maxAge}", false));
        assertion.getCookieAttributes().put(COMMENT, new CookieAttribute(COMMENT, "${comment}", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "${version}", false));

        assertEquals(AssertionStatus.NONE, new ServerManageCookieAssertion(assertion).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(5, cookie.getVersion());
        assertEquals("/test", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("test comment", cookie.getComment());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void addOrReplaceCookieAlreadyExistsIgnoresOriginalAttributeValues() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "originalValue", 1, null, null, -1, false, "originalComment"));
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", true));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "newValue", true));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "0", true));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final Set<HttpCookie> cookies = request.getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("newValue", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertEquals(-1, cookie.getMaxAge());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void addOrReplaceCookieNoName() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "test", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "0", false));

        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie name", e.getMessage());
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void addOrReplaceCookieNoValue() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "test", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "0", false));

        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie value", e.getMessage());
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
            throw e;
        }
    }

    @Test(expected = PolicyAssertionException.class)
    public void addOrReplaceCookieNoVersion() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));

        try {
            new ServerManageCookieAssertion(assertion);
            fail("Expected PolicyAssertionException");
        } catch (final PolicyAssertionException e) {
            assertEquals("Missing cookie version", e.getMessage());
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
            assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
            throw e;
        }
    }

    @Test
    public void addOrReplaceCookieMaxAgeNotNumeric() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertion.getCookieAttributes().put(MAX_AGE, new CookieAttribute(MAX_AGE, "notNumeric", false));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_MAX_AGE));
    }

    @Test
    public void addOrReplaceCookieNameResolvesToEmpty() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "${name}", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "1", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addOrReplaceCookieVersionResolvesToEmpty() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "${version}", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addOrReplaceCookieVersionNotNumeric() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new CookieAttribute(NAME, "foo", false));
        assertion.getCookieAttributes().put(VERSION, new CookieAttribute(VERSION, "notNumeric", false));
        assertion.getCookieAttributes().put(VALUE, new CookieAttribute(VALUE, "bar", false));
        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    private ServerManageCookieAssertion configureServerAssertion(final ServerManageCookieAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
        return serverAssertion;
    }
}
