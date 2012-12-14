package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;

import java.text.MessageFormat;
import java.util.logging.Level;

import static com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion.DEFAULT_COOKIE_NAME;
import static org.junit.Assert.*;

public class ServerCookieCredentialSourceAssertionTest {
    private static final String SESSION_VALUE = "asdf123";
    private ServerCookieCredentialSourceAssertion serverAssertion;
    private CookieCredentialSourceAssertion assertion;
    private PolicyEnforcementContext context;
    private MockHttpServletRequest mockRequest;

    @Before
    public void setup() {
        mockRequest = new MockHttpServletRequest();
        assertion = new CookieCredentialSourceAssertion();
    }

    @Test
    public void checkRequestSetsContextVariableDefaults() throws Exception {
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(DEFAULT_COOKIE_NAME, SESSION_VALUE);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) context.getVariable(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
        assertEquals(SESSION_VALUE, contextVariable);
    }

    @Test
    public void checkRequestSetsContextVariableNonDefaultCookieName() throws Exception {
        final String cookieName = "testCookie";
        final String cookieValue = "cookieValue";
        assertion.setCookieName(cookieName);
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) context.getVariable(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + cookieName);
        assertEquals(cookieValue, contextVariable);
    }

    @Test
    public void checkRequestSetsContextVariableNonDefaultPrefix() throws Exception {
        final String prefix = "pre";
        assertion.setVariablePrefix(prefix);
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(DEFAULT_COOKIE_NAME, SESSION_VALUE);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) context.getVariable(prefix + "." + DEFAULT_COOKIE_NAME);
        assertEquals(SESSION_VALUE, contextVariable);
    }

    @Test
    public void checkRequestSetsContextVariableNonDefaultNameAndPrefix() throws Exception {
        final String cookieName = "testCookie";
        final String cookieValue = "cookieValue";
        final String prefix = "pre";
        assertion.setCookieName(cookieName);
        assertion.setVariablePrefix(prefix);
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) context.getVariable(prefix + "." + cookieName);
        assertEquals(cookieValue, contextVariable);
    }

    @Test
    public void checkRequestCookieNameDoesNotMatch() throws Exception {
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final String cookieName = "nomatch";
        final Cookie cookie = new Cookie(cookieName, "nomatchvalue");
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.AUTH_REQUIRED, assertionStatus);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + cookieName);
    }

    @Test
    public void checkRequestNoCookies() throws Exception {
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(null), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.AUTH_REQUIRED, assertionStatus);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
    }

    /**
     * Should only set a context variable for the cookie that matches the cookie name.
     */
    @Test
    public void checkRequestSetsContextVariableMultipleCookies() throws Exception {
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie sessionCookie = new Cookie(DEFAULT_COOKIE_NAME, SESSION_VALUE);
        final Cookie testCookie = new Cookie("testCookie", "testValue");
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(sessionCookie, testCookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) context.getVariable(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
        assertEquals(SESSION_VALUE, contextVariable);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + ".testCookie");
    }

    @Test
    public void checkRequestSetsContextVariableNullPrefix() throws Exception {
        assertion.setVariablePrefix(null);
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(DEFAULT_COOKIE_NAME, SESSION_VALUE);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
    }

    @Test
    public void checkRequestSetsContextVariableEmptyPrefix() throws Exception {
        assertion.setVariablePrefix("");
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final Cookie cookie = new Cookie(DEFAULT_COOKIE_NAME, SESSION_VALUE);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        checkContextVariableDoesNotExist(CookieCredentialSourceAssertion.DEFAULT_VARIABLE_PREFIX + "." + DEFAULT_COOKIE_NAME);
    }

    /**
     * Validate that a cookie with an empty value is treated as missing
     * <ul>
     *     <li>checkRequest() returns AUTH_REQUIRED</li>
     *     <li>Required audit is generated and audit is at the FINE level</li>
     * </ul>
     */
    @Test
    @BugNumber(13610)
    public void testEmptyCookiesTreatedAsMissing() throws Exception {
        serverAssertion = new ServerCookieCredentialSourceAssertion(assertion);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.MapBuilder.<String, Object>builder()
                                .put("auditFactory", testAudit.factory())
                                .map());

        final Cookie cookie = new Cookie(DEFAULT_COOKIE_NAME, "");
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(createRequest(cookie), new Message());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_REQUIRED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTTPCOOKIE_FOUND_EMPTY));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTTPCOOKIE_FOUND_EMPTY.getMessage(), DEFAULT_COOKIE_NAME)));
        assertEquals(Level.FINE, AssertionMessages.HTTPCOOKIE_FOUND_EMPTY.getLevel());

    }

    private Message createRequest(final Cookie... cookies) {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setCookies(cookies);
        final HttpRequestKnob knob = new HttpServletRequestKnob(mockRequest);
        final Message request = new Message();
        request.attachHttpRequestKnob(knob);
        return request;
    }

    private void checkContextVariableDoesNotExist(final String variable) throws NoSuchVariableException {
        try{
            context.getVariable(variable);
            fail("Expected NoSuchVariableException");
        }catch(final NoSuchVariableException e){
            //pass
        }
    }
}
