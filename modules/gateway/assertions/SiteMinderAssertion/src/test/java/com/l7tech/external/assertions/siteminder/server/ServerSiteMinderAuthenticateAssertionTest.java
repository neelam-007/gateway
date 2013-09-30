package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Test the SiteMinderAuthenticateAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderAuthenticateAssertionTest {
    public static final String SSO_TOKEN = "abcdefghigklmnopqrstuvwxyz0123456789==";
    public static final String USER_LOGIN = "user";
    public static final String USER_PASSWORD = "password";

    @Mock
    SiteMinderContext mockContext;
    @Mock
    SiteMinderHighLevelAgent mockHla;
    @Mock
    SiteMinderLowLevelAgent mockLla;
    @Mock
    ApplicationContext mockAppCtx;

    ServerSiteMinderAuthenticateAssertion fixture;
    SiteMinderAuthenticateAssertion smAuthenticateAssertion;
    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext pec;

    @BeforeClass
    public static void setUpBeforeClass() {
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode", "false");
    }


    @Before
    public void setUp() throws Exception {

        smAuthenticateAssertion = new SiteMinderAuthenticateAssertion();
        System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "true");

        when(mockAppCtx.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class)).thenReturn(mockHla);
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(new SiteMinderConfigurationManagerStub());
        //Setup Context
        requestMsg = new Message();
        responseMsg = new Message();
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

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials("user", "password")), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials("user", "password")), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldValidateCookieWhenCredentialsPresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(true);
        smAuthenticateAssertion.setCookieSourceVar("cookie.SMSESSION");

        pec.setVariable("cookie.SMSESSION", SSO_TOKEN);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_LOGIN, USER_PASSWORD.toCharArray()), smAuthenticateAssertion.getClass()));

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(USER_LOGIN, USER_PASSWORD)), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials(USER_LOGIN, USER_PASSWORD)), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldValidateCookieWhenNoCredentialsPresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(true);

        smAuthenticateAssertion.setCookieSourceVar("cookie.SMSESSION");

        pec.setVariable("cookie.SMSESSION", SSO_TOKEN);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);


        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldValidateCookieWhenLoginCredentialsHaveNoUsername() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setUseSMCookie(true);
        smAuthenticateAssertion.setLogin("user");
        smAuthenticateAssertion.setCookieSourceVar("cookie.SMSESSION");

        pec.setVariable("cookie.SMSESSION", SSO_TOKEN);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new OpaqueSecurityToken(null, SSO_TOKEN.toCharArray()), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), anyString(), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldFailWhenLoginCredentialsHaveNoUsername() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setUseSMCookie(false);
        smAuthenticateAssertion.setLogin("user");

        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new OpaqueSecurityToken(null, SSO_TOKEN.toCharArray()), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(3);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.FALSIFIED == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), isNull(String.class), eq(mockContext));
    }

    @Test
    public void shouldFailWhenTargetHasNoCredentials() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(false);
        smAuthenticateAssertion.setTarget(TargetMessageType.RESPONSE);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_LOGIN, USER_PASSWORD.toCharArray()), smAuthenticateAssertion.getClass()));
        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.METADATA);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), isNull(String.class), any(SiteMinderContext.class))).thenReturn(-1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.FALSIFIED == fixture.checkRequest(pec));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), isNull(String.class), eq(mockContext));
    }

}
