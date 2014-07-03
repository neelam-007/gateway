package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import com.l7tech.test.BugId;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Copyright: CA Technologies, 2013
 * @author : ymoiseyenko
 * Date: 9/6/13
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderAuthorizeAssertionTest {

    private static final String SSO_TOKEN = "abcdef0123456789==";
    private static final String SOURCE_IP = "10.7.22.22";
    ServerSiteMinderAuthorizeAssertion fixture;

    @Mock
    SiteMinderContext mockContext;
    @Mock
    ApplicationContext mockAppCtx;
    @Mock
    SiteMinderHighLevelAgent mockHla;
    @Mock
    SiteMinderLowLevelAgent mockLla;

    SiteMinderAuthorizeAssertion assertion;
    PolicyEnforcementContext pec;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception{
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode","false"); //Disable java compatibility mode to avoid linking JNI libraries to the tests
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "true");
        assertion = new SiteMinderAuthorizeAssertion();
        when(mockAppCtx.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class)).thenReturn(mockHla);
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(new SiteMinderConfigurationManagerStub());
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);
        //Setup Policy Enforcement Context
        Message requestMsg = new Message();
        Message responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldAuthorizeWhenValidSsoTokenPresent() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(false);
        assertion.setSetSMCookie(false);
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        when(mockContext.getAgent()).thenReturn(mockLla);
        when(mockHla.processAuthorizationRequest(anyString(), (String)isNull(), eq(mockContext))).thenReturn(AbstractServerSiteMinderAssertion.SM_YES);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertTrue(pec.getResponse().getHttpResponseKnob() != null);
        assertTrue(pec.getResponse().getHttpCookiesKnob().getCookies().isEmpty());
        verify(mockContext, times(1)).getSourceIpAddress();

    }

    @Test
    public void shouldAuthorizeAndSetCookieWhenValidSsoTokenPresent() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(false);
        assertion.setSetSMCookie(true);
        assertion.setCookieName("SMSESSION");
        assertion.setCookieDomain(".domain");
        assertion.setCookiePath("/some/path");
        assertion.setCookieSecure("false");
        assertion.setCookieVersion("1");
        assertion.setCookieMaxAge("1000");
        assertion.setCookieComment("this is a cookie comment");
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        when(mockContext.getAgent()).thenReturn(mockLla);
        when(mockHla.processAuthorizationRequest(anyString(), (String)isNull(), eq(mockContext))).thenReturn(AbstractServerSiteMinderAssertion.SM_YES);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertTrue(pec.getResponse().getHttpResponseKnob() != null);
        final Set<HttpCookie> cookies = pec.getResponse().getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("SMSESSION", cookie.getCookieName());
        assertEquals(".domain", cookie.getDomain());
        assertEquals("/some/path", cookie.getPath());
        assertEquals(false, cookie.isSecure());
        assertEquals(1000, cookie.getMaxAge());
        assertEquals(1, cookie.getVersion());
        assertEquals("this is a cookie comment", cookie.getComment());

    }

    @Test
    public void shouldAuthorizeAndSetCookieWhenSsoTokenInContextVariable() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(true);
        assertion.setCookieSourceVar("test");
        assertion.setSetSMCookie(true);
        assertion.setCookieName("SMSESSION");
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        pec.setVariable("test", SSO_TOKEN);
        when(mockContext.getAgent()).thenReturn(mockLla);
        when(mockHla.processAuthorizationRequest(anyString(), eq(SSO_TOKEN), eq(mockContext))).thenReturn(AbstractServerSiteMinderAssertion.SM_YES);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertTrue(pec.getResponse().getHttpResponseKnob() != null);
        final Set<HttpCookie> cookies = pec.getResponse().getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("SMSESSION", cookie.getCookieName());
        assertEquals("\"" + SSO_TOKEN + "\"", cookie.getCookieValue());

    }

    @Test
    public void testSetCookie_cookieValueEndsWithEqualSign() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(true);
        assertion.setCookieSourceVar("test");
        assertion.setSetSMCookie(true);
        assertion.setCookieName("SMSESSION");
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        pec.setVariable("test", SSO_TOKEN);
        Map<String,Object> map = pec.getVariableMap(assertion.getVariablesUsed(), new TestAudit());
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertTrue(fixture.setSessionCookie(pec, mockContext, map));
        verify(mockContext, times(1)).getSsoToken();
        String[] cookies = pec.getResponse().getHeadersKnob().getHeaderValues("Set-Cookie", HeadersKnob.HEADER_TYPE_HTTP);
        assertTrue("Only one cookie header is set",cookies.length == 1);
        assertEquals("SMSESSION=\"" + SSO_TOKEN+ "\"", cookies[0]);
    }

    @Test
    public void testSetCookie_cookieValueDoesNotEndWithEqualSign() throws Exception {
        final String ssoToken = "abCdeFgh01/8986863/aVbgG";
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(true);
        assertion.setCookieSourceVar("test");
        assertion.setSetSMCookie(true);
        assertion.setCookieName("SMSESSION");
        when(mockContext.getSsoToken()).thenReturn(ssoToken);
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        pec.setVariable("test", ssoToken);
        Map<String,Object> map = pec.getVariableMap(assertion.getVariablesUsed(), new TestAudit());
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertTrue(fixture.setSessionCookie(pec, mockContext, map));
        verify(mockContext, times(1)).getSsoToken();
        String[] cookies = pec.getResponse().getHeadersKnob().getHeaderValues("Set-Cookie", HeadersKnob.HEADER_TYPE_HTTP);
        assertTrue("Only one cookie header is set",cookies.length == 1);
        assertEquals("SMSESSION=" + ssoToken, cookies[0]);
    }

    @Test
    public void shouldFalsifyWhenResponseIsNotHttp() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(true);
        assertion.setCookieSourceVar("test");
        assertion.setSetSMCookie(true);
        assertion.setCookieName("SMSESSION");
        //Setup Policy Enforcement Context
        Message requestMsg = new Message();
        Message responseMsg = new Message();
        responseMsg.attachJmsKnob(new JmsKnobStub(Goid.DEFAULT_GOID, false, "SOAP-Action"));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        pec.setVariable("test", SSO_TOKEN);
        when(mockContext.getAgent()).thenReturn(mockLla);
        when(mockHla.processAuthorizationRequest(anyString(), eq(SSO_TOKEN), eq(mockContext))).thenReturn(AbstractServerSiteMinderAssertion.SM_YES);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(pec));
    }

    @Test
    public void shouldFalsifyWhenNoSmContextFound() throws Exception {
        assertion.setPrefix("siteminder");
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion,mockAppCtx);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(pec));
    }

    @Test
    public void shouldFalsifyWhenNoAgentPresent() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(false);
        assertion.setSetSMCookie(false);
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion,mockAppCtx);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(pec));
    }

    @BugId("SSG-7535")
    @Test
    public void shouldAuthorizeWhenSourceIpInSmContext() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setUseVarAsCookieSource(false);
        assertion.setSetSMCookie(false);
        pec.setVariable(assertion.getPrefix() + ".smcontext", mockContext);
        when(mockContext.getSourceIpAddress()).thenReturn(SOURCE_IP);
        when(mockContext.getAgent()).thenReturn(mockLla);
        when(mockHla.processAuthorizationRequest(eq(SOURCE_IP), (String) isNull(), eq(mockContext))).thenReturn(AbstractServerSiteMinderAssertion.SM_YES);
        fixture = new ServerSiteMinderAuthorizeAssertion(assertion, mockAppCtx);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        verify(mockContext, times(2)).getSourceIpAddress();
        assertTrue(pec.getResponse().getHttpResponseKnob() != null);
        assertTrue(pec.getResponse().getHttpCookiesKnob().getCookies().isEmpty());

    }
}
