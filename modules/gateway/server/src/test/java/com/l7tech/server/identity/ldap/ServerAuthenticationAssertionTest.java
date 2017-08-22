package com.l7tech.server.identity.ldap;

import static org.junit.Assert.*;


import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.message.Message;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import com.l7tech.server.policy.assertion.identity.ServerAuthenticationAssertion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chaja24 on 8/3/2017.
 * Unit test for the ServerAuthenticationAssertion
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerAuthenticationAssertionTest {

    private static String LDAP_INVALID_CREDS_ERROR = "Invalid username or password";
    private static String AUTHENTICATION_USER_CONTEXT_VAR = "idp.error.login";
    private static String AUTHENTICATION_ERROR_CONTEXT_VAR = "idp.error.message";
    private static int FOUR_MINS_IN_MS = 480000;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PolicyEnforcementContext pec;

    @Mock
    private ServerAuthenticationAssertion serverAuthenticationAssertion;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private IdentityProviderFactory identityProviderFactory;

    @Mock
    private LdapIdentityProviderConfig ldapIdentityProviderConfig;

    private AuthenticationAssertion authenticationAssertion;
    private TestIdentityProvider testIdentityProvider;

    @Before
    public void setUp() throws Exception {

        testIdentityProvider = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        Mockito.when(applicationContext.getBean("identityProviderFactory", IdentityProviderFactory.class)).thenReturn(identityProviderFactory);
        Mockito.when(identityProviderFactory.getProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG.getGoid())).thenReturn(testIdentityProvider);

        //Disable AuthCache by default
        when(authenticationContext.getAuthSuccessCacheTime()).thenReturn(0);
        when(authenticationContext.getAuthFailureCacheTime()).thenReturn(0);

        authenticationAssertion = new AuthenticationAssertion();
        authenticationAssertion.setIdentityProviderOid(testIdentityProvider.getConfig().getGoid());
        authenticationAssertion.setEnabled(true);
        authenticationAssertion.setTarget(TargetMessageType.REQUEST);

        final Message requestMessage = new Message();
        when(pec.getTargetMessage(authenticationAssertion)).thenReturn(requestMessage);
        when(pec.getAuthenticationContext(any(Message.class))).thenReturn(authenticationContext);
    }

    // The user has a valid password and should authenticate successfully.
    // AuthCache disabled by default
    @Test
    public void testValidUser() throws Exception {

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String validPassword1 = "validPassword";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                validPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        final AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);
    }


    // User exists but password is invalid.  The authentication should fail.
    // AuthCache disabled by default
    @Test
    public void testUserWithInvalidPassword() throws Exception {

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String validPassword1 = "password";
        final String badPassword1 = "invalidPassword";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        verifyContextOutput(pec, AUTHENTICATION_USER_CONTEXT_VAR, 0, login1); // verify user1 was returned
        verifyContextOutput(pec, AUTHENTICATION_ERROR_CONTEXT_VAR, 0, LDAP_INVALID_CREDS_ERROR); // verify user1 returned error message

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);
    }


    // User does not exist in the LDAP server.  The authentication should fail.
    // AuthCache disabled by default
    @Test
    public void testUserNotInLdap() throws Exception {

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        String login1 = "userNotExist";
        String password1 = "invalidPassword";
        String login2 = "user2";
        String password2 = "validPassword";

        LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                password1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login2, password2.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);
    }


    // Authenticate 2 users with bad passwords.  The authentication should fail. The Context Variables should return
    // the logins that failed and the reason it failed. AuthCache disabled by default
    @Test
    public void test2UsersInvalidPasswords() throws Exception {

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String validPassword1 = "password1";
        final String badPassword1 = "invalidPassword1";

        final String login2 = "user2";
        final String validPassword2 = "password2";
        final String badPassword2 = "invalidPassword2";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);

        final LoginCredentials loginCredsUser2 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login2,
                                badPassword2.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser2);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp.

        final UserBean ub2 = new UserBean(testIdentityProvider.getConfig().getGoid(), login2);
        TestIdentityProvider.addUser(ub2, login2, validPassword2.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        final AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        verifyContextOutput(pec, AUTHENTICATION_USER_CONTEXT_VAR, 0, login1); // verify user1 was returned
        verifyContextOutput(pec, AUTHENTICATION_USER_CONTEXT_VAR, 1, login2); // verify user2 was returned

        verifyContextOutput(pec, AUTHENTICATION_ERROR_CONTEXT_VAR, 0, LDAP_INVALID_CREDS_ERROR); // verify user1 returned error message
        verifyContextOutput(pec, AUTHENTICATION_ERROR_CONTEXT_VAR, 1, LDAP_INVALID_CREDS_ERROR); // verify user2 returned error message

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);

    }


    // If one user provides an invalid password and the other provides a valid password, the ServerAuthenticationAssertion
    // will authenticate successfully. AuthCache disabled by default.
    @Test
    public void testOneInvalidOneValidPasswords() throws Exception {

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String badPassword1 = "invalidPassword";
        final String validPassword1 = "password";

        final String login2 = "user2";
        final String validPassword2 = "validPassword";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);

        final LoginCredentials loginCredsUser2 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login2,
                                validPassword2.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser2);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp.

        final UserBean ub2 = new UserBean(testIdentityProvider.getConfig().getGoid(), login2);
        TestIdentityProvider.addUser(ub2, login2, validPassword2.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        final AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);
    }


    // Authenticate the user twice.  After the first attempt, the result will be stored in the AuthCache.
    // On the second attempt, the authentication results will be retrieved from the AuthCache.
    // The authentication should succeed.
    @Test
    public void testSuccessCache() throws Exception {

        // Turn on the success cache.
        when(authenticationContext.getAuthSuccessCacheTime()).thenReturn(FOUR_MINS_IN_MS);

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String validPassword1 = "validPassword";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                validPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        final UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);

        TestIdentityProvider.clearAllUsers(); // remove user from identity provider.  User should still be in the cache.

        status = serverAuthenticationAssertion.checkRequest(pec);
        // verify status is NONE.
        assertEquals(AssertionStatus.NONE, status);
    }


    // Authenticate the user with a bad password twice. After the first attempt, the result will be stored in the AuthCache.
    // On the second attempt, the authentication results will be retrieved from the AuthCache.  The authentication should fail.
    @Test
    public void testFailureCache() throws Exception {

        // Turn on the failure cache.
        when(authenticationContext.getAuthFailureCacheTime()).thenReturn(FOUR_MINS_IN_MS);

        final List<LoginCredentials> loginCredentialsList = new ArrayList<>();
        final String login1 = "user1";
        final String validPassword1 = "password";
        final String badPassword1 = "invalidPassword";

        final LoginCredentials loginCredsUser1 =
                LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                                SecurityTokenType.HTTP_BASIC,
                                login1,
                                badPassword1.toCharArray()),
                        AuthenticationAssertion.class);

        loginCredentialsList.add(loginCredsUser1);
        when(authenticationContext.getCredentials()).thenReturn(loginCredentialsList);

        UserBean ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, validPassword1.toCharArray()); // add user with valid password to idp.

        serverAuthenticationAssertion = new ServerAuthenticationAssertion(authenticationAssertion, applicationContext);
        AssertionStatus status = serverAuthenticationAssertion.checkRequest(pec);

        verifyContextOutput(pec, AUTHENTICATION_USER_CONTEXT_VAR, 0, login1); // verify user1 was returned
        verifyContextOutput(pec, AUTHENTICATION_ERROR_CONTEXT_VAR, 0, LDAP_INVALID_CREDS_ERROR); // verify user1 returned error message

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);

        TestIdentityProvider.clearAllUsers();
        // add the user with bad password to the identity provider so that it matches the request.  This should not affect
        // the authentication to make it pass because the request will use the previous authentication results from the failurecache
        // to fail the check request.
        ub = new UserBean(testIdentityProvider.getConfig().getGoid(), login1);
        TestIdentityProvider.addUser(ub, login1, badPassword1.toCharArray()); // add user with valid password to idp.

        status = serverAuthenticationAssertion.checkRequest(pec);

        verifyContextOutput(pec, AUTHENTICATION_USER_CONTEXT_VAR, 0, login1); // verify user1 was returned
        verifyContextOutput(pec, AUTHENTICATION_ERROR_CONTEXT_VAR, 0, LDAP_INVALID_CREDS_ERROR); // verify user1 returned error message

        // verify status is not NONE.
        assertNotEquals(AssertionStatus.NONE, status);
    }


    void verifyContextOutput(PolicyEnforcementContext pec, String variableName, int arrayElement, String expectedString) {

        ArgumentCaptor<Object[]> argCaptor = ArgumentCaptor.forClass(Object[].class);

        verify(pec,Mockito.atLeast(1)).setVariable(eq(variableName), argCaptor.capture());
        String returnedMessage = (String) (argCaptor.getValue()[arrayElement]);
        assertEquals(expectedString, returnedMessage);

    }
}
