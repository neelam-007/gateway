package com.l7tech.server.identity.ldap;

import static org.junit.Assert.*;


import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUrlBasedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;

import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.message.Message;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.context.ApplicationContext;
import com.l7tech.server.policy.assertion.identity.ServerAuthenticationAssertion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by chaja24 on 8/3/2017.
 * Unit test for the ServerAuthenticationAssertion
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LdapUtils.class)
public class ServerAuthenticationAssertionTest {

    private static String PROVIDER_GOID = "a5a3d5aba3ea0236f52677478cdcafad";
    private static String LDAP_INVALID_CREDS_ERROR = "[LDAP: error code 49 - Invalid Credentials]";
    private static String USER1_DN = "dc=com,dc=company,ou=group1,cn=firstname1 lastname1";
    private static String USER2_DN = "dc=com,dc=company,ou=group1,cn=firstname2 lastname2";
    private static String AUTHENTICATION_USER_CONTEXT_VAR = "idp.error.login";
    private static String AUTHENTICATION_ERROR_CONTEXT_VAR = "idp.error.message";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PolicyEnforcementContext pec;

    @Mock
    private MessageTargetableAssertion messageTargetableAssertion;

    @Mock
    private ServerAuthenticationAssertion serverAuthenticationAssertion;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private IdentityProviderFactory identityProviderFactory;

    @Mock
    private LdapIdentityProviderConfig ldapIdentityProviderConfig;

    @Mock
    private SecurityToken securityToken;

    @Mock
    private LdapGroupManagerImpl ldapGroupManagerImpl;

    @Mock
    LdapRuntimeConfig ldapRuntimeConfig;

    private AuthenticationAssertion authenticationAssertion;
    private LdapIdentityProviderImpl ldapIdentityProviderImpl;
    private LdapUserManagerImpl ldapUserManagerImpl;

    @Before
    public void setUp() throws Exception {

        PowerMockito.mockStatic(LdapUtils.class);

        authenticationAssertion = new AuthenticationAssertion();
        authenticationAssertion.setIdentityProviderOid(new Goid(PROVIDER_GOID));
        authenticationAssertion.setEnabled(true);
        authenticationAssertion.setTarget(TargetMessageType.REQUEST);

        Message requestMessage = new Message();
        when(pec.getTargetMessage(authenticationAssertion)).thenReturn(requestMessage);
        when(pec.getAuthenticationContext(any(Message.class))).thenReturn(authenticationContext);

        ldapIdentityProviderImpl = Mockito.spy(new LdapIdentityProviderImpl());
        ldapIdentityProviderImpl.setApplicationContext(applicationContext);

        LdapIdentityProviderConfig identityProviderConfig = new LdapIdentityProviderConfig();
        identityProviderConfig.setGoid(new Goid(PROVIDER_GOID));
        identityProviderConfig.setLdapUrl(new String[]{"ldap://host:5555"});

        ldapUserManagerImpl = Mockito.spy(new LdapUserManagerImpl());
        ldapUserManagerImpl.setLdapRuntimeConfig(ldapRuntimeConfig);

        ldapIdentityProviderImpl.setUserManager(ldapUserManagerImpl);
        ldapIdentityProviderImpl.setGroupManager(ldapGroupManagerImpl);
        ldapIdentityProviderImpl.setIdentityProviderConfig(identityProviderConfig);

        Mockito.when(applicationContext.getBean("identityProviderFactory", IdentityProviderFactory.class)).thenReturn(identityProviderFactory);
        when(identityProviderFactory.getProvider(Mockito.any(Goid.class))).thenReturn(ldapIdentityProviderImpl);
    }

    @Test
    public void testValidUser() throws Exception {

        List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "user1";
        String validPassword1 = "validPassword";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                validPassword1.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        LdapUser user = new LdapUser();
        user.setLogin(login1);
        user.setDn(USER1_DN);
        user.setProviderId(new Goid(PROVIDER_GOID));

        Mockito.doReturn(user).when(ldapUserManagerImpl).findByLogin(login1);

        when(LdapUtils.authenticateBasic(any(LdapUrlProvider.class),
                any(LdapUrlBasedIdentityProviderConfig.class),
                any(LdapRuntimeConfig.class),
                any(Logger.class),
                eq(USER1_DN),
                eq(validPassword1))).thenReturn(true);

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);
    }


    // User exists but password is invalid
    @Test
    public void testUserWithInvalidPassword() throws Exception {

        List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "user1";
        String badPassword1 = "invalidPassword";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        LdapUser user = new LdapUser();
        user.setLogin(login1);
        user.setDn(USER1_DN);
        user.setProviderId(new Goid(PROVIDER_GOID));

        Mockito.doReturn(user).when(ldapUserManagerImpl).findByLogin(login1);

        when(LdapUtils.authenticateBasic(any(LdapUrlProvider.class),
                any(LdapUrlBasedIdentityProviderConfig.class),
                any(LdapRuntimeConfig.class),
                any(Logger.class),
                eq(USER1_DN),
                eq(badPassword1))).thenThrow(new BadCredentialsException(LDAP_INVALID_CREDS_ERROR));

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        ArgumentCaptor<Object[]> argCaptor = ArgumentCaptor.forClass(Object[].class);

        // verify returned user1
        verify(pec).setVariable(eq(AUTHENTICATION_USER_CONTEXT_VAR), argCaptor.capture());
        String returnedLogin1 = (String) (argCaptor.getValue()[0]);
        assertEquals(login1, returnedLogin1);

        // verify user1 returned error message
        verify(pec).setVariable(eq(AUTHENTICATION_ERROR_CONTEXT_VAR), argCaptor.capture());
        String returnedMessageUser1 = (String) (argCaptor.getValue()[0]);
        assertEquals(LDAP_INVALID_CREDS_ERROR, returnedMessageUser1);

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);
    }


    // User does not exist
    @Test
    public void testUserNotInLdap() throws Exception {

        List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "userNotExist";
        String password1 = "invalidPassword";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                password1.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        LdapUser user = new LdapUser();
        user.setLogin(login1);
        user.setDn(USER1_DN);
        user.setProviderId(new Goid(PROVIDER_GOID));

        Mockito.doReturn(null).when(ldapUserManagerImpl).findByLogin(login1);

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        ArgumentCaptor<Object[]> argCaptor = ArgumentCaptor.forClass(Object[].class);

        // verify returned user1
        verify(pec).setVariable(eq(AUTHENTICATION_USER_CONTEXT_VAR), argCaptor.capture());
        String returnedLogin1 = (String) (argCaptor.getValue()[0]);
        assertEquals(login1, returnedLogin1);

        // verify user1 returned error message
        verify(pec).setVariable(eq(AUTHENTICATION_ERROR_CONTEXT_VAR), argCaptor.capture());
        String returnedMessageUser1 = (String) (argCaptor.getValue()[0]);
        assertEquals("The user does not exist.", returnedMessageUser1);

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);
    }


    @Test
    public void test2UsersInvalidPasswords() throws Exception {

        List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "user1";
        String login2 = "user2";
        String badPassword1 = "invalidPassword1";
        String badPassword2 = "invalidPassword2";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);

        LoginCredentials loginCredsUser2 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login2,
                                badPassword2.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser2);

        LdapUser user1 = new LdapUser();
        user1.setLogin(login1);
        user1.setDn(USER1_DN);
        user1.setProviderId(new Goid(PROVIDER_GOID));

        LdapUser user2 = new LdapUser();
        user2.setLogin(login2);
        user2.setDn(USER2_DN);
        user2.setProviderId(new Goid(PROVIDER_GOID));
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        Mockito.doReturn(user1).when(ldapUserManagerImpl).findByLogin(login1);
        Mockito.doReturn(user2).when(ldapUserManagerImpl).findByLogin(login2);

        when(LdapUtils.authenticateBasic(any(LdapUrlProvider.class),
                any(LdapUrlBasedIdentityProviderConfig.class),
                any(LdapRuntimeConfig.class),
                any(Logger.class),
                anyString(),
                anyString())).thenThrow(new BadCredentialsException(LDAP_INVALID_CREDS_ERROR));

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        ArgumentCaptor<Object[]> argCaptor = ArgumentCaptor.forClass(Object[].class);

        // verify returned user1
        verify(pec).setVariable(eq(AUTHENTICATION_USER_CONTEXT_VAR), argCaptor.capture());
        String returnedLogin1 = (String) (argCaptor.getValue()[0]);
        assertEquals(login1, returnedLogin1);

        // verify returned user2
        String returnedLogin2 = (String) (argCaptor.getValue()[1]);
        assertEquals(login2, returnedLogin2);

        // verify user1 returned error message
        verify(pec).setVariable(eq(AUTHENTICATION_ERROR_CONTEXT_VAR), argCaptor.capture());
        String returnedMessageUser1 = (String) (argCaptor.getValue()[0]);
        assertEquals(LDAP_INVALID_CREDS_ERROR, returnedMessageUser1);

        // verify user1 returned error message
        String returnedMessageUser2 = (String) (argCaptor.getValue()[1]);
        assertEquals(LDAP_INVALID_CREDS_ERROR, returnedMessageUser2);

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);

    }


    // If one user provides an invalid password and the other provides a valid password, the ServerAuthenticationAssertion
    // will authenticate successfully.
    @Test
    public void testOneInvalidOneValidPasswords() throws Exception {

        List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "user1";
        String login2 = "user2";
        String badPassword1 = "invalidPassword";
        String password2 = "validPassword";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);

        LoginCredentials loginCredsUser2 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login2,
                                password2.toCharArray()),
                                AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser2);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        LdapUser user1 = new LdapUser();
        user1.setLogin(login1);
        user1.setDn(USER1_DN);
        user1.setProviderId(new Goid(PROVIDER_GOID));

        LdapUser user2 = new LdapUser();
        user2.setLogin(login2);
        user2.setDn(USER2_DN);
        user2.setProviderId(new Goid(PROVIDER_GOID));

        Mockito.doReturn(user1).when(ldapUserManagerImpl).findByLogin(login1);
        Mockito.doReturn(user2).when(ldapUserManagerImpl).findByLogin(login2);

        when(LdapUtils.authenticateBasic(any(LdapUrlProvider.class),
                any(LdapUrlBasedIdentityProviderConfig.class),
                any(LdapRuntimeConfig.class),
                any(Logger.class),
                eq(USER1_DN),
                anyString())).thenThrow(new BadCredentialsException(LDAP_INVALID_CREDS_ERROR));

        when(LdapUtils.authenticateBasic(any(LdapUrlProvider.class),
                any(LdapUrlBasedIdentityProviderConfig.class),
                any(LdapRuntimeConfig.class),
                any(Logger.class),
                eq(USER2_DN),
                eq(password2))).thenReturn(true);

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);

    }

}
