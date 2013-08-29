package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test the SiteMinderAuthenticateAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderAuthenticateAssertionTest {
    public static final String SSO_TOKEN = "abcdefghigklmnopqrstuvwxyz0123456789==";

    @Mock
    SiteMinderContext mockContext;
    @Mock
    SiteMinderHighLevelAgent mockHla;
    @Mock
    ApplicationContext mockAppCtx;

    ServerSiteMinderAuthenticateAssertion fixture;
    SiteMinderAuthenticateAssertion smAuthenticateAssertion;
    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext pec;
    private TestHttpRequestKnob httpRequestKnob;


    @Before
    public void setUp() throws Exception {
        smAuthenticateAssertion = new SiteMinderAuthenticateAssertion();
        System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "true");

        when(mockAppCtx.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class)).thenReturn(mockHla);
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(new SiteMinderConfigurationManagerStub());
        //Setup Context
        requestMsg = new Message();
        httpRequestKnob = new TestHttpRequestKnob(new HttpCookie("SMSESSION", SSO_TOKEN, 1, "/", "domain"));
        requestMsg.attachHttpRequestKnob(httpRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new TestResponse());
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED);
    }

    @Test
    public void shouldAuthenticateUserWhenCredentialsPresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(false);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("user", "password".toCharArray()), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(new SiteMinderLowLevelAgent());
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(any(SiteMinderCredentials.class), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(any(SiteMinderCredentials.class), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldValidateCookieWhenCredentialsPresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(true);
        smAuthenticateAssertion.setCookieSourceVar("cookie.SMSESSION");

        pec.setVariable("cookie.SMSESSION", SSO_TOKEN);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        when(mockContext.getAgent()).thenReturn(new SiteMinderLowLevelAgent());
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("user", "password".toCharArray()), smAuthenticateAssertion.getClass()));

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(any(SiteMinderCredentials.class), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockHla, times(1)).processAuthenticationRequest(any(SiteMinderCredentials.class), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldValidateCookieWhenNoCredentialsPresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(true);

        smAuthenticateAssertion.setCookieSourceVar("cookie.SMSESSION");

        pec.setVariable("cookie.SMSESSION", SSO_TOKEN);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        when(mockContext.getAgent()).thenReturn(new SiteMinderLowLevelAgent());
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);


        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(any(SiteMinderCredentials.class), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockHla, times(1)).processAuthenticationRequest(any(SiteMinderCredentials.class), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    private static class TestResponse extends AbstractHttpResponseKnob {

        @Override
        public void addCookie(HttpCookie cookie) {
        }

    }

    private  static class TestHttpRequestKnob extends HttpRequestKnobStub {
        private final HttpCookie[] cookies;

        TestHttpRequestKnob(HttpCookie smsession) {
            cookies = new HttpCookie[] {smsession};
        }

        TestHttpRequestKnob(){
            cookies = new HttpCookie[0];
        }

        @Override
        public HttpCookie[] getCookies() {
            return cookies;
        }

    }

}
