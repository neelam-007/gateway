package com.l7tech.external.assertions.managecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.NameValuePair;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertNull(cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookie() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, "/test"));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, "localhost"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "60"));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, "test comment"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));

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
        assertTrue(cookie.isHttpOnly());
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "${value}"));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, "${path}"));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, "${domain}"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "${maxAge}"));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, "${comment}"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "${version}"));

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
        assertFalse(cookie.isHttpOnly());
    }

    @Test
    public void addCookieEmptyValues() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, ""));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, ""));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, ""));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, ""));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, ""));

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
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "someValue"));
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "notNumeric"));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_MAX_AGE));
    }

    @Test
    public void addCookieNameResolvesToEmpty() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieAlreadyExists() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "existingValue", 0, "/", "localhost", 60, true, "test", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, "localhost"));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, "/"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "newValue"));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("existingValue", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ALREADY_EXISTS));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieNoVersion() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
    }

    @Test
    public void addCookieVersionResolvesToEmpty() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "${version}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
    }

    @Test
    public void addCookieVersionNotNumeric() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "notNumeric"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("noDomain", "noDomain", 1, "/", null));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final Map<String, HttpCookie> cookieMap = mapCookies(request.getHttpCookiesKnob());
        assertNull(cookieMap.get("noDomain").getDomain());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost"));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("noPath", "noPath", 1, null, "localhost"));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final Map<String, HttpCookie> cookieMap = mapCookies(request.getHttpCookiesKnob());
        assertNull(cookieMap.get("noPath").getPath());
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
        final HttpCookie responseCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
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
        final HttpCookie requestCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

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
        assertFalse(cookie.isHttpOnly());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieToOtherTargetMessageType() throws Exception {
        context.getOrCreateTargetMessage(new MessageTargetableSupport("testMessage"), false);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("testMessage");
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

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
        assertFalse(cookie.isHttpOnly());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByName() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
        assertTrue(cookie.isHttpOnly());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByDomain() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("noDomain", "noDomain", 0, "/", null, -1, false, null, false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(2, request.getHttpCookiesKnob().getCookies().size());
        final Map<String, HttpCookie> cookieMap = mapCookies(request.getHttpCookiesKnob());
        final HttpCookie modifiedCookie = cookieMap.get(modified);
        assertEquals(modified, modifiedCookie.getCookieName());
        assertEquals(modified, modifiedCookie.getCookieValue());
        assertEquals(modified, modifiedCookie.getPath());
        assertEquals(modified, modifiedCookie.getDomain());
        assertEquals(modified, modifiedCookie.getComment());
        assertEquals(1, modifiedCookie.getVersion());
        assertEquals(99, modifiedCookie.getMaxAge());
        assertTrue(modifiedCookie.isSecure());
        assertTrue(modifiedCookie.isHttpOnly());
        assertNull(cookieMap.get("noDomain").getDomain());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("noPath", "noPath", 0, null, "localhost", -1, false, null, false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(2, request.getHttpCookiesKnob().getCookies().size());
        final Map<String, HttpCookie> cookieMap = mapCookies(request.getHttpCookiesKnob());
        final HttpCookie modifiedCookie = cookieMap.get(modified);
        assertEquals(modified, modifiedCookie.getCookieName());
        assertEquals(modified, modifiedCookie.getCookieValue());
        assertEquals(modified, modifiedCookie.getPath());
        assertEquals(modified, modifiedCookie.getDomain());
        assertEquals(modified, modifiedCookie.getComment());
        assertEquals(1, modifiedCookie.getVersion());
        assertEquals(99, modifiedCookie.getMaxAge());
        assertTrue(modifiedCookie.isSecure());
        assertTrue(modifiedCookie.isHttpOnly());
        assertNull(cookieMap.get("noPath").getPath());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByNameDomainAndPath() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, "localhost", false));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "/", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
        assertTrue(cookie.isHttpOnly());
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "${value}"));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, "${domain}"));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, "${path}"));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, "${comment}"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "${maxAge}"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
        assertTrue(cookie.isHttpOnly());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByNameRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, STARTS_WITH_F, true));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
            assertTrue(cookie.isHttpOnly());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByDomainRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(DOMAIN, new CookieCriteria(DOMAIN, STARTS_WITH_L, true));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
            assertTrue(cookie.isHttpOnly());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieByPathRegex() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo2", "bar2", 0, "/2", "localhost2", 60, false, "test2", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, STARTS_WITH_FORWARD_SLASH, true));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, modified));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, modified));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "99"));
        assertion.getCookieAttributes().put(SECURE, new NameValuePair(SECURE, "true"));
        assertion.getCookieAttributes().put(HTTP_ONLY, new NameValuePair(HTTP_ONLY, "true"));
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
            assertTrue(cookie.isHttpOnly());
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieNameResolvesToEmpty() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "${name}", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar2"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${path}", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar2"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        assertion.getCookieCriteria().put(PATH, new CookieCriteria(PATH, "${domain}", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar2"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "invalidVersion"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 0, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, modified));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VALUE, "1"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "invalidMaxAge"));
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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false));
        final String modified = "modified";
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", true));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, modified));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals(modified, cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("/", cookie.getPath());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("test", cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(60, cookie.getMaxAge());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void updateCookieDoesNotExist() throws Exception {
        assertion.getCookieCriteria().put(NAME, new CookieCriteria(NAME, "foo", false));
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.setOperation(com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.UPDATE);

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIES_NOT_MATCHED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test(expected = PolicyAssertionException.class)
    public void updateCookieNoCriteria() throws Exception {
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "0"));

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
        request.getHttpCookiesKnob().addCookie(new HttpCookie("foo", "originalValue", 1, null, null, -1, false, null, false));
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "newValue"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "0"));

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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "${value}"));
        assertion.getCookieAttributes().put(PATH, new NameValuePair(PATH, "${path}"));
        assertion.getCookieAttributes().put(DOMAIN, new NameValuePair(DOMAIN, "${domain}"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "${maxAge}"));
        assertion.getCookieAttributes().put(COMMENT, new NameValuePair(COMMENT, "${comment}"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "${version}"));

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
        assertFalse(cookie.isHttpOnly());
    }

    @Test(expected = PolicyAssertionException.class)
    public void addOrReplaceCookieNoName() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "test"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "0"));

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
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "test"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "0"));

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

    @Test
    public void addOrReplaceCookieNoVersion() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
    }

    @Test
    public void addOrReplaceCookieMaxAgeNotNumeric() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertion.getCookieAttributes().put(MAX_AGE, new NameValuePair(MAX_AGE, "notNumeric"));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_MAX_AGE));
    }

    @Test
    public void addOrReplaceCookieNameResolvesToEmpty() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "${name}"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "1"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(request.getHttpCookiesKnob().getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_COOKIE_NAME));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addOrReplaceCookieVersionResolvesToEmpty() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "${version}"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));

        assertEquals(AssertionStatus.NONE, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertEquals(1, request.getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = request.getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
    }

    @Test
    public void addOrReplaceCookieVersionNotNumeric() throws Exception {
        assertion.setOperation(Operation.ADD_OR_REPLACE);
        assertion.getCookieAttributes().put(NAME, new NameValuePair(NAME, "foo"));
        assertion.getCookieAttributes().put(VERSION, new NameValuePair(VERSION, "notNumeric"));
        assertion.getCookieAttributes().put(VALUE, new NameValuePair(VALUE, "bar"));
        assertEquals(AssertionStatus.FALSIFIED, configureServerAssertion(new ServerManageCookieAssertion(assertion)).checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_COOKIE_VERSION));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    private Map<String, HttpCookie> mapCookies(final HttpCookiesKnob cookiesKnob) {
        final Map<String, HttpCookie> cookieMap = new HashMap<>();
        for (final HttpCookie cookie : cookiesKnob.getCookies()) {
            cookieMap.put(cookie.getCookieName(), cookie);
        }
        return cookieMap;
    }

    private ServerManageCookieAssertion configureServerAssertion(final ServerManageCookieAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
        return serverAssertion;
    }
}
