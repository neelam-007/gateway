package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.*;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import com.l7tech.util.Pair;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
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
    public static final byte[] certBytes = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};

    @Mock
    SiteMinderContext mockContext;
    @Mock
    SiteMinderHighLevelAgent mockHla;
    @Mock
    SiteMinderLowLevelAgent mockLla;
    @Mock
    ApplicationContext mockAppCtx;
    @Mock
    X509Certificate mockClientCertificate;

    ServerSiteMinderAuthenticateAssertion fixture;
    SiteMinderAuthenticateAssertion smAuthenticateAssertion;
    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext pec;
    private TestAudit auditor;
    private List<Pair<String,Object>> attrList;


    @BeforeClass
    public static void setUpBeforeClass() {
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode", "false");
    }


    @Before
    public void setUp() throws Exception {

        auditor = new TestAudit();
        smAuthenticateAssertion = new SiteMinderAuthenticateAssertion();
        System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "true");

        when(mockAppCtx.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class)).thenReturn(mockHla);
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(new SiteMinderConfigurationManagerStub());
        //Setup Context
        requestMsg = new Message();
        responseMsg = new Message();
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        attrList = new ArrayList<>();
        attrList.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERDN, "CN=user,OU=QA Test Users,DC=domain,DC=local"));
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
        attrList.add(new Pair<String, Object>(SiteMinderAgentConstants.ATTR_USERNAME, USER_LOGIN));
        when(mockContext.getAttrList()).thenReturn(attrList);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials("user", "password")), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        assertEquals(USER_LOGIN, expandVariable(pec, "${request.authenticatedUser}"));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials(USER_LOGIN, USER_PASSWORD)), isNull(String.class), isNull(String.class), eq(mockContext));

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
        when(mockContext.getAttrList()).thenReturn(attrList);

        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_LOGIN, USER_PASSWORD.toCharArray()), smAuthenticateAssertion.getClass()));

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(USER_LOGIN, USER_PASSWORD)), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        assertEquals(USER_LOGIN, expandVariable(pec, "${request.authenticatedUser}"));
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
        when(mockContext.getAttrList()).thenReturn(attrList);


        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        assertEquals(USER_LOGIN, expandVariable(pec, "${request.authenticatedUser}"));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldValidateCookieWhenLoginCredentialsHaveNoUsername() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setUseSMCookie(true);
        smAuthenticateAssertion.setCredentialsName("user");
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
        when(mockContext.getAttrList()).thenReturn(attrList);
        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials()), anyString(), eq(SSO_TOKEN), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        assertEquals(USER_LOGIN, expandVariable(pec, "${request.authenticatedUser}"));
        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials()), isNull(String.class), eq(SSO_TOKEN), eq(mockContext));
    }

    @Test
    public void shouldAuthenticateWhenX509CertificatePresent() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(true);
        smAuthenticateAssertion.setUseSMCookie(false);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        X500Principal mockSubjectDn = new X500Principal("CN=user,OU=unit,O=CA Technologies,C=CA");
        when(mockClientCertificate.getSubjectX500Principal()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getSubjectDN()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getEncoded()).thenReturn(certBytes);
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpClientCertToken(mockClientCertificate), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldAuthenticateWhenX509CertificatePresentAndCredentialsContainSubjectDN() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setCredentialsName("CN=user,OU=unit,O=CA Technologies,C=CA");
        smAuthenticateAssertion.setUseSMCookie(false);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        X500Principal mockSubjectDn = new X500Principal("CN=user,OU=unit,O=CA Technologies,C=CA");
        when(mockClientCertificate.getSubjectX500Principal()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getSubjectDN()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getEncoded()).thenReturn(certBytes);
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpClientCertToken(mockClientCertificate), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldAuthenticateWhenX509CertificatePresentAndCredentialsContainCN() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setCredentialsName("user");
        smAuthenticateAssertion.setUseSMCookie(false);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        X500Principal mockSubjectDn = new X500Principal("CN=user,OU=unit,O=CA Technologies,C=CA");
        when(mockClientCertificate.getSubjectX500Principal()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getSubjectDN()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getEncoded()).thenReturn(certBytes);
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpClientCertToken(mockClientCertificate), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));

        verify(mockHla, times(1)).processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldFailWhenX509CertificatePresentAndCredentialsDontMatch() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setCredentialsName("other user");
        smAuthenticateAssertion.setUseSMCookie(false);
        pec.setVariable(smAuthenticateAssertion.getPrefix() + ".smcontext", mockContext);

        X500Principal mockSubjectDn = new X500Principal("CN=user,OU=unit,O=CA Technologies,C=CA");
        when(mockClientCertificate.getSubjectX500Principal()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getSubjectDN()).thenReturn(mockSubjectDn);
        when(mockClientCertificate.getEncoded()).thenReturn(certBytes);
        AuthenticationContext ac = pec.getDefaultAuthenticationContext();
        ac.addCredentials(LoginCredentials.makeLoginCredentials(new HttpClientCertToken(mockClientCertificate), smAuthenticateAssertion.getClass()));

        when(mockContext.getAgent()).thenReturn(mockLla);
        List<SiteMinderContext.AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTISSUEDN);
        authSchemes.add(SiteMinderContext.AuthenticationScheme.X509CERTUSERDN);
        when(mockContext.getAuthSchemes()).thenReturn(authSchemes);
        when(mockContext.getSsoToken()).thenReturn(SSO_TOKEN);

        when(mockHla.processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), anyString(), isNull(String.class), any(SiteMinderContext.class))).thenReturn(1);

        fixture = new ServerSiteMinderAuthenticateAssertion(smAuthenticateAssertion, mockAppCtx);
        assertTrue(AssertionStatus.FALSIFIED == fixture.checkRequest(pec));

        verify(mockHla, never()).processAuthenticationRequest(eq(new SiteMinderCredentials(mockClientCertificate)), isNull(String.class), isNull(String.class), eq(mockContext));

    }

    @Test
    public void shouldFailWhenLoginCredentialsHaveNoUsername() throws Exception {
        smAuthenticateAssertion.setPrefix("siteminder");

        smAuthenticateAssertion.setLastCredential(false);
        smAuthenticateAssertion.setUseSMCookie(false);
        smAuthenticateAssertion.setCredentialsName("user");

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

    private String expandVariable(PolicyEnforcementContext context, String expression) {
        String[] usedVars = Syntax.getReferencedNames(expression);
        Map<String, Object> vars = context.getVariableMap(usedVars, auditor);
        return ExpandVariables.process(expression, vars, auditor);
    }

}
