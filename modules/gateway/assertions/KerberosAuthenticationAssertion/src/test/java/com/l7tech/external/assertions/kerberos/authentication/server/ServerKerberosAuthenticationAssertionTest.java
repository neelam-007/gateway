package com.l7tech.external.assertions.kerberos.authentication.server;

import static org.mockito.Matchers.any;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.l7tech.external.assertions.kerberos.authentication.KerberosAuthenticationAssertion;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.KerberosTestSetup;
import com.l7tech.kerberos.delegate.KerberosDelegateClient;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.token.http.HttpNegotiateToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.util.MockInjector;
import com.l7tech.util.FileUtils;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.Ticket;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Test the KerberosAuthenticationAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerKerberosAuthenticationAssertionTest {

    private static final Logger log = Logger.getLogger(ServerKerberosAuthenticationAssertionTest.class.getName());
    private static final String SSG_PRINCIPAL = "ssg1test";
    static byte[] TEST_BYTES = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
    @Mock
    KerberosDelegateClient mockDelegateClient;

    @Mock
    AuthenticationContext mockAuthenticationContext;

    TestServerKerberosAuthenticationAssertion fixture;
    KerberosAuthenticationAssertion assertion;
    private GenericApplicationContext applicationContext;


    @Before
    public void setUp() throws Exception {
        applicationContext = new GenericApplicationContext();
        ConstructorArgumentValues cavs = new ConstructorArgumentValues();
        cavs.addGenericArgumentValue(new Properties());
        RootBeanDefinition config = new RootBeanDefinition(MockConfig.class, cavs, null);
        applicationContext.registerBeanDefinition("config", config);

        ConstructorArgumentValues assertionConstructors = new ConstructorArgumentValues();
        assertion = new KerberosAuthenticationAssertion();
        assertionConstructors.addGenericArgumentValue(assertion);
        RootBeanDefinition serverKerberosAuthenticationAssertion = new RootBeanDefinition(TestServerKerberosAuthenticationAssertion.class, assertionConstructors, null);
        serverKerberosAuthenticationAssertion.addQualifier(new AutowireCandidateQualifier(Inject.class));

        AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
        applicationContext.registerBeanDefinition("auditFactory", new RootBeanDefinition(LoggingAudit.LoggingAuditFactory.class));
        applicationContext.registerBeanDefinition("injector", new RootBeanDefinition(MockInjector.class));
        applicationContext.registerBeanDefinition("serverKerberosAuthenticationAssertion", serverKerberosAuthenticationAssertion);
        applicationContext.refresh();

        File tmpDir = FileUtils.createTempDirectory("kerberos", null, null, true);
        KerberosTestSetup.init(tmpDir);
        KerberosTestSetup.setUp(tmpDir);
       fixture = (TestServerKerberosAuthenticationAssertion) applicationContext.getBean("serverKerberosAuthenticationAssertion");

    }

    @Test
    public void testProtocolTransitionWithLastAuthenticatedUser_success() throws Exception {
        SecurityToken token = new HttpBasicToken("user","password".toCharArray());

        setUpProtocolTransition(token);

        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, new HashMap<String, Object>()));
        verify(mockAuthenticationContext, times(1)).addCredentials(any(LoginCredentials.class));
    }

    @Test
    public void testProtocolTransitionWithLastAuthenticatedUser_kerberosToken() throws Exception {
        final KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES));
        SecurityToken token = new KerberosSecurityToken() {
            @Override
            public KerberosGSSAPReqTicket getTicket() {
                return new KerberosGSSAPReqTicket(TEST_BYTES);
            }

            @Override
            public KerberosServiceTicket getServiceTicket() {
                return testTicket;
            }

            @Override
            public SecurityTokenType getType() {
                return SecurityTokenType.HTTP_KERBEROS;
            }
        };

        setUpProtocolTransition(token);

        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, new HashMap<String, Object>()));
        verify(mockAuthenticationContext, times(1)).addCredentials(any(LoginCredentials.class));
    }

    public void testProtocolTransitionNoAuthenticatedUser_kerberosToken() throws Exception {
        final KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES));
        SecurityToken token = new KerberosSecurityToken() {
            @Override
            public KerberosGSSAPReqTicket getTicket() {
                return new KerberosGSSAPReqTicket(TEST_BYTES);
            }

            @Override
            public KerberosServiceTicket getServiceTicket() {
                return testTicket;
            }

            @Override
            public SecurityTokenType getType() {
                return SecurityTokenType.HTTP_KERBEROS;
            }
        };

        setUpProtocolTransition(token);

        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, new HashMap<String, Object>()));
        verify(mockAuthenticationContext, times(1)).addCredentials(any(LoginCredentials.class));
    }

    @Test
    public void testProtocolTransitionWithLastAuthenticatedUser_noCredentialsFound() throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(true);
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(true);
        assertion.setKrbUseGatewayKeytab(true);
        LdapUser user = new LdapUser();
        user.setLogin("user");

        List<LoginCredentials> credentialsList = new ArrayList<LoginCredentials>();
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        assertEquals(AssertionStatus.FALSIFIED, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, new HashMap<String, Object>()));
        verify(mockAuthenticationContext, never()).addCredentials(any(LoginCredentials.class));
    }

    @Test
    public void testProtocolTransitionWithLastAuthenticatedUser_notAuthenticated() throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(true);
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(true);
        assertion.setKrbUseGatewayKeytab(true);
        LdapUser user = new LdapUser();
        user.setLogin("user");

        SecurityToken token = new HttpBasicToken("user","password".toCharArray());
        LoginCredentials pc = LoginCredentials.makeLoginCredentials(token, Assertion.class);
        List<LoginCredentials> credentialsList = Arrays.asList(new LoginCredentials[]{pc});
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        assertEquals(AssertionStatus.FALSIFIED, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, new HashMap<String, Object>()));
        verify(mockAuthenticationContext, never()).addCredentials(any(LoginCredentials.class));
    }

    @Test
    public void testProtocolTransitionWithSpecificUser_success() throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(false);
        assertion.setAuthenticatedUser("User Name");
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(true);
        assertion.setKrbUseGatewayKeytab(true);
        LdapUser user = new LdapUser();
        user.setLogin("user");
        user.setName("User Name");

        SecurityToken token = new HttpBasicToken("user","password".toCharArray());
        AuthenticationResult authResult = new AuthenticationResult(user, token);
        when(mockAuthenticationContext.getAllAuthenticationResults()).thenReturn(Arrays.asList(new AuthenticationResult[]{authResult}));

        LoginCredentials pc = LoginCredentials.makeLoginCredentials(token, Assertion.class);
        List<LoginCredentials> credentialsList = Arrays.asList(new LoginCredentials[]{pc});
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES));
        when(mockDelegateClient.getKerberosProxyServiceTicketWithKeytab(assertion.getServicePrincipalName(), fixture.getServicePrincipal(assertion.getRealm()), user.getLogin(),  assertion.getRealm())).thenReturn(testTicket);

        Map<String,Object> variableMap = new HashMap<String, Object>();

        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, variableMap));
        verify(mockAuthenticationContext, times(1)).addCredentials(any(LoginCredentials.class));
    }

    @Test
    public void testProtocolTransitionWithSpecificUser_wrongUserNotAuthenticated() throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(false);
        assertion.setAuthenticatedUser("Wrong User Name");
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(true);
        assertion.setKrbUseGatewayKeytab(true);
        LdapUser user = new LdapUser();
        user.setLogin("user");
        user.setName("User Name");

        SecurityToken token = new HttpBasicToken("user","password".toCharArray());
        SecurityToken token2 = new HttpBasicToken("otherUser", "password".toCharArray());
        AuthenticationResult authResult = new AuthenticationResult(user, token);
        when(mockAuthenticationContext.getAllAuthenticationResults()).thenReturn(Arrays.asList(new AuthenticationResult[]{authResult}));

        LoginCredentials pc = LoginCredentials.makeLoginCredentials(token, Assertion.class);
        LoginCredentials pc2 = LoginCredentials.makeLoginCredentials(token2, Assertion.class);
        List<LoginCredentials> credentialsList = Arrays.asList(new LoginCredentials[]{pc,pc2});
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        Map<String,Object> variableMap = new HashMap<String, Object>();

        KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES));
        when(mockDelegateClient.getKerberosProxyServiceTicketWithKeytab(assertion.getServicePrincipalName(), fixture.getServicePrincipal(assertion.getRealm()), user.getLogin(), assertion.getRealm())).thenReturn(testTicket);


        assertEquals(AssertionStatus.FALSIFIED, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, variableMap));
        verify(mockAuthenticationContext, never()).addCredentials(any(LoginCredentials.class));

    }

    @Test
    public void testConstrainedDelegaton_success() throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(true);
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(false);
        assertion.setS4U2Proxy(true);
        assertion.setKrbUseGatewayKeytab(true);

        final Ticket mockTicket = mock(Ticket.class);
        KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES), null, null, mockTicket);
        SecurityToken token = new HttpNegotiateToken(testTicket);

        LoginCredentials pc = LoginCredentials.makeLoginCredentials(token, Assertion.class);
        List<LoginCredentials> credentialsList = Arrays.asList(new LoginCredentials[]{pc});
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        when(mockDelegateClient.getKerberosProxyServiceTicket(assertion.getServicePrincipalName(), new PrincipalName("http/client@DOMAIN.COM"), fixture.getServicePrincipal(assertion.getRealm()), mockTicket)).thenReturn(testTicket);

        Map<String,Object> variableMap = new HashMap<String, Object>();

        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(mockAuthenticationContext, mockDelegateClient, variableMap));
        verify(mockAuthenticationContext, times(1)).addCredentials(any(LoginCredentials.class));
    }


    private void setUpProtocolTransition(SecurityToken token) throws Exception {
        assertion.setRealm("DOMAIN.COM");
        assertion.setLastAuthenticatedUser(true);
        assertion.setServicePrincipalName("http/service@DOMAIN.COM");
        assertion.setS4U2Self(true);
        assertion.setKrbUseGatewayKeytab(true);
        LdapUser user = new LdapUser();
        user.setLogin("user");

        AuthenticationResult authResult = new AuthenticationResult(user, token);
        when(mockAuthenticationContext.getLastAuthenticationResult()).thenReturn(authResult);

        LoginCredentials pc = LoginCredentials.makeLoginCredentials(token, Assertion.class);
        List<LoginCredentials> credentialsList = Arrays.asList(new LoginCredentials[]{pc});
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentialsList);

        KerberosServiceTicket testTicket = new KerberosServiceTicket("http/client@DOMAIN.COM","http/service@DOMAIN.COM", null, 0, new KerberosGSSAPReqTicket(TEST_BYTES));
        when(mockDelegateClient.getKerberosProxyServiceTicketWithKeytab(assertion.getServicePrincipalName(), fixture.getServicePrincipal(assertion.getRealm()), user.getLogin(),  assertion.getRealm())).thenReturn(testTicket);
    }

    private static class TestServerKerberosAuthenticationAssertion extends ServerKerberosAuthenticationAssertion {



        public TestServerKerberosAuthenticationAssertion(final KerberosAuthenticationAssertion assertion) throws PolicyAssertionException {
            super(assertion);
        }


        @Override
        protected String getServicePrincipal(String realm) throws KerberosException {
            return "http/" + SSG_PRINCIPAL + "@" + realm;    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

}
