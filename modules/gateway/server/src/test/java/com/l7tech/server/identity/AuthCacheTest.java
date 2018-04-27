package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.message.SshKnob;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SshSecurityToken;
import com.l7tech.test.BugId;
import com.l7tech.util.TimeSource;
import com.l7tech.security.token.http.HttpBasicToken;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 */
public class AuthCacheTest {

    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "password";
    private static final int MAX_AGE = 1000;

    /*TestIdentityProvider uses static variables so reset it's users between tests*/
    @Before
    public void setUp() throws Exception{
        TestIdentityProvider.clearAllUsers();
    }

    /**
     * Test the success cache basic functionality
     * Tests that the Cache hits when it should, missed when it's entries are too old yet still exist, and that the
     * Cache is cleaned up internally removing old entries
     * The max_age used for all cache retrievals is set so that no items should have expired
     * When we want to test the expiry code on an object still in the cache this is set to 5 milliseconds below
     * After the cache has been pruned the number of elements in the cache should be equal to the size of the cache
     */
    @Test
    public void testSuccessCache() throws Exception{
        LoginCredentials lc = LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_NAME, PASSWORD.toCharArray()), HttpBasic.class);
        final int[] authInvocations = new int[1];
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG){
            @Override
            public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
                authInvocations[0]++;
                return super.authenticate(pc);
            }
        };

        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        final long[] time = new long[]{ System.currentTimeMillis() };
        AuthCache aC = new AuthCache("TestAuthCache", new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return time[0];
            }
        }, 5, 3, 5, 3);

        //First authenticate causes the idp to be contacted
        Assert.assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));

        //Second should hit and no delay should be experienced
        time[0] = time[0] + 500;
        int invocationsBefore = authInvocations[0];
        Assert.assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        Assert.assertTrue("Cache should have hit", invocationsBefore == authInvocations[0]);

        //test cache miss
        time[0] = time[0] + 1000;
        invocationsBefore = authInvocations[0];
        Assert.assertNotNull(aC.getCachedAuthResult(lc, tIP, 5, 5));
        Assert.assertTrue("Cache should have missed", invocationsBefore != authInvocations[0]);

        aC.dispose();
    }

    /**
     * Test the failure cache basic functionality
     * Tests that the Cache hits when it should, missed when it's entries are too old yet still exist, and that the
     * Cache is cleaned up internally removing old entries
     * The max_age used for all cache retrievals is set so that no items should have expired
     * When we want to test the expiry code on an object still in the cache this is set to 5 milliseconds below
     * After the cache has been pruned the number of elements in the cache should be equal to the size of the cache
     */
    @Test
    public void testFailureCache() throws Exception{
        LoginCredentials lc = LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_NAME+"miss", PASSWORD.toCharArray()), HttpBasic.class);
        final int[] authInvocations = new int[1];
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG){
            @Override
            public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
                authInvocations[0]++;
                return super.authenticate(pc);
            }
        };

        final long[] time = new long[]{ System.currentTimeMillis() };
        AuthCache aC = new AuthCache("TestAuthCache", new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return time[0];
            }
        }, 5, 3, 5, 3);

        //First authenticate causes the idp to be contacted
        Assert.assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));

        //Second should hit
        time[0] = time[0] + 500;
        int invocationsBefore = authInvocations[0];

        try {
            aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE);
            Assert.fail("The user should be in the cache and return BadCredentialsException. This line should not have been executed.");
        } catch (BadCredentialsException e){
            Assert.assertTrue("Cache should have hit and return BadCredentialsException", invocationsBefore == authInvocations[0]);
        }


        //Third authenticate should be a miss due to max ages
        time[0] = time[0] + 1000;
        invocationsBefore = authInvocations[0];
        Assert.assertNull(aC.getCachedAuthResult(lc, tIP, 5, 5));
        Assert.assertTrue("Cache should have missed", invocationsBefore != authInvocations[0]);

        aC.dispose();
    }

    /**
     * Test that the success cache works correctly when the failure cache is disabled
     */
    @Test
    public void testSuccessCacheIndependent() throws Exception{
        //Set failure cache == 0, which should disable it
        AuthCache aC = new AuthCache("TestAuthCache", new TimeSource(), 5, 3, 0, 3);
        Class c = aC.getClass();

        //Confirm internal state
        Field failureDisabledField = c.getDeclaredField("failureCacheDisabled");
        failureDisabledField.setAccessible(true);
        Assert.assertTrue("Failure cache disabled", failureDisabledField.getBoolean(aC));

        final int[] authInvocations = new int[1];
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG){
            @Override
            public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
                authInvocations[0]++;
                return super.authenticate(pc);
            }
        };

        String userName = USER_NAME+"1";
        UserBean ub1 = new UserBean(tIP.getConfig().getGoid(), userName);

        TestIdentityProvider.addUser(ub1, userName, PASSWORD.toCharArray());
        LoginCredentials lc1 = LoginCredentials.makeLoginCredentials(new HttpBasicToken(userName, PASSWORD.toCharArray()), HttpBasic.class);
        Assert.assertNotNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));

        addSomeUsers(5, tIP, aC);
        //Should be cache hit
        int invocationsBefore = authInvocations[0];
        Assert.assertNotNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        Assert.assertTrue("Cache should have hit", invocationsBefore == authInvocations[0]);

        aC.dispose();
    }

    /**
     * Test that the failure cache works correctly when the success cache is disabled
     */
    @Test
    public void testFailureCacheIndependent() throws Exception{
        //Set success cache == 0, which should disable it
        AuthCache aC = new AuthCache("TestAuthCache", new TimeSource(), 0, 3, 5, 3);
        Class c = aC.getClass();

        //Confirm internal state
        Field successDisabledField = c.getDeclaredField("successCacheDisabled");
        successDisabledField.setAccessible(true);
        Assert.assertTrue("Success cache disabled", successDisabledField.getBoolean(aC));

        //now that we know the failure cache has been disabled, call the success test case above
        final int[] authInvocations = new int[1];
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG){
            @Override
            public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
                authInvocations[0]++;
                return super.authenticate(pc);
            }
        };

        LoginCredentials lc1 = LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_NAME+"1", PASSWORD.toCharArray()), HttpBasic.class);
        Assert.assertNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        for(int i = 0; i < 5; i++){
            LoginCredentials lc = LoginCredentials.makeLoginCredentials(new HttpBasicToken(USER_NAME+"a"+i, PASSWORD.toCharArray()), HttpBasic.class);
            Assert.assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        }

        //Should be cache hit
        int invocationsBefore = authInvocations[0];
        try {
            aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE);
            Assert.fail("The user should be in the cache and return BadCredentialsException. This line should not have been executed.");
        } catch (BadCredentialsException e){
            Assert.assertTrue("Cache should have hit and return BadCredentialsException", invocationsBefore == authInvocations[0]);
        }

        aC.dispose();
    }

    private void addSomeUsers(int numUsers, TestIdentityProvider tIP, AuthCache aC) throws Exception{
        for(int i = 0; i < numUsers; i++){
            String userName = USER_NAME+"users"+i;
            UserBean ub = new UserBean(tIP.getConfig().getGoid(), userName);
            TestIdentityProvider.addUser(ub, userName, PASSWORD.toCharArray());
            LoginCredentials lc = LoginCredentials.makeLoginCredentials(new HttpBasicToken(userName, PASSWORD.toCharArray()), HttpBasic.class);
            Assert.assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        }
    }

    @BugId("DE342376")
    @Test(expected = AuthenticationException.class)
    public void testSSHAuthentication_SameUsername_MustDenyUser2Authentication() throws Exception {
        String user1SshKey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9avqjf+J3MzDLH9NkcT41Sya52m5afHn9U9BjRIZ6FXMtQqQP1wWAmuL8Q0aPUWYEzZ8ABKIRr7EvPQEg3iRrB8YipyHA0fR119O1hYrmBRenDmLfH3w5WYzj+gjaKxkCeCfIowQFVOB9H3Yw1I3L3Hw9pUvxT2OSmaZunvIMQUP3+SC3KfIZ3b+BusoeD8rTEFZe3yxupCvJvpOzObE+SC+j1ALwmEsnYol8o8IEpBSAu3zDjYiun2zEWjKi3lhsYS1S9JTr6cXDcAj+mZodJtCu5enYbU5y934gvf2+YEl9fX1q9HjBiGJjWBkZUxNgRcISy6I/PMvbpgt+pAQuQIDAQAB-----END PUBLIC KEY-----\n";

        String user2SshKey = "-----BEGIN PUBLIC KEY-----MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxqWVxdHcOIW5JGQO0eoS9tY9F4X2cpDK7akiFT/laKFesDegrbqOIwW3tVOBRClj8cQ9Rh3hGZS77WPxuLv37hVBDroQQaggYPod1KdStPwEjRWfA+FXHWm93YaHklOZrDqqWqeHDryzPVGUBuPNm1ZZwt7TGx9irAqqLeyImgf34mlL4Q7GvkANxO1l8YTSRghJHYbEpxNyzJ12wRVZr1mFOWEMLax6MC4SJkzyoMj5WAvR3nMLrlO+rQTsh0uzVzmqM4+4oggM7kHeAtFeJInc3f/crWbUAcUH+uVM+u7np0H9mYANJf8ZhsyyX/rXrRwcBfffeNMHsEoJLm0ULbVibf6fQo8TWbdxP0cr9KSUOKi/rw62uVq37KwVmyHSnk0yegPyFIcl8NdTEnjURxiwwMKyNi4M9iPm8mEgO4/SJnDNtwwBFJW7FPvy8Pnrp85OWD2PA1Hdc6ZIigZ7E2X+xEh2x2bGP9EQYMbE5/sqw4C/Zbxhs0e7cq6aQpPqwigEUK6SySQTlNYPNwPWZD1swb97G2FAEGWJKVe9wzQyd8U3o2zWkUEwpW01H2Iy4rEso5Cwea+TeCdRQIY6TMf4pgs8nWaNlTItZOdA+2PeEHEU91FqypKOMhMPkuQqs35xzvX1SNRaq+GJgpz469V943dmB6IjJYDkdW+HjPkCAwEAAQ==-----END PUBLIC KEY-----\n";

        TestIdentityProvider tIP = setupSSHSessionTest("user1", user1SshKey, "user2", user2SshKey);
        LoginCredentials user1Creds = createSSHLoginCredentials("user1", user1SshKey);
        LoginCredentials user2Creds = createSSHLoginCredentials("user1", user2SshKey);

        AuthCache authCache = new AuthCache();
        AuthenticationResult user1Result = null;
        try {
            user1Result = authCache.getCachedAuthResult(user1Creds, tIP);
        } catch (AuthenticationException e) {
            Assert.fail("User1 must have been authenticated.");
        }
        Assert.assertNotNull(user1Result);
        Assert.assertEquals(user1Result.getUser().getLogin(), "user1");

        authCache.getCachedAuthResult(user2Creds, tIP); // This must throw AuthenticationException
        Assert.fail("User2 should not have been authenticated.");
    }

    @BugId("DE342376")
    @Test
    public void testSSHAuthentication_DifferentUsername_MustPassUser2Authentication() throws Exception {
        String user1SshKey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9avqjf+J3MzDLH9NkcT41Sya52m5afHn9U9BjRIZ6FXMtQqQP1wWAmuL8Q0aPUWYEzZ8ABKIRr7EvPQEg3iRrB8YipyHA0fR119O1hYrmBRenDmLfH3w5WYzj+gjaKxkCeCfIowQFVOB9H3Yw1I3L3Hw9pUvxT2OSmaZunvIMQUP3+SC3KfIZ3b+BusoeD8rTEFZe3yxupCvJvpOzObE+SC+j1ALwmEsnYol8o8IEpBSAu3zDjYiun2zEWjKi3lhsYS1S9JTr6cXDcAj+mZodJtCu5enYbU5y934gvf2+YEl9fX1q9HjBiGJjWBkZUxNgRcISy6I/PMvbpgt+pAQuQIDAQAB-----END PUBLIC KEY-----\n";

        String user2SshKey = "-----BEGIN PUBLIC KEY-----MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxqWVxdHcOIW5JGQO0eoS9tY9F4X2cpDK7akiFT/laKFesDegrbqOIwW3tVOBRClj8cQ9Rh3hGZS77WPxuLv37hVBDroQQaggYPod1KdStPwEjRWfA+FXHWm93YaHklOZrDqqWqeHDryzPVGUBuPNm1ZZwt7TGx9irAqqLeyImgf34mlL4Q7GvkANxO1l8YTSRghJHYbEpxNyzJ12wRVZr1mFOWEMLax6MC4SJkzyoMj5WAvR3nMLrlO+rQTsh0uzVzmqM4+4oggM7kHeAtFeJInc3f/crWbUAcUH+uVM+u7np0H9mYANJf8ZhsyyX/rXrRwcBfffeNMHsEoJLm0ULbVibf6fQo8TWbdxP0cr9KSUOKi/rw62uVq37KwVmyHSnk0yegPyFIcl8NdTEnjURxiwwMKyNi4M9iPm8mEgO4/SJnDNtwwBFJW7FPvy8Pnrp85OWD2PA1Hdc6ZIigZ7E2X+xEh2x2bGP9EQYMbE5/sqw4C/Zbxhs0e7cq6aQpPqwigEUK6SySQTlNYPNwPWZD1swb97G2FAEGWJKVe9wzQyd8U3o2zWkUEwpW01H2Iy4rEso5Cwea+TeCdRQIY6TMf4pgs8nWaNlTItZOdA+2PeEHEU91FqypKOMhMPkuQqs35xzvX1SNRaq+GJgpz469V943dmB6IjJYDkdW+HjPkCAwEAAQ==-----END PUBLIC KEY-----\n";

        TestIdentityProvider tIP = setupSSHSessionTest("user1", user1SshKey, "user2", user2SshKey);
        LoginCredentials user1Creds = createSSHLoginCredentials("user1", user1SshKey);
        LoginCredentials user2Creds = createSSHLoginCredentials("user2", user2SshKey);

        AuthCache authCache = new AuthCache();
        AuthenticationResult user1Result = authCache.getCachedAuthResult(user1Creds, tIP);
        Assert.assertNotNull(user1Result);
        Assert.assertEquals(user1Result.getUser().getLogin(), "user1");

        AuthenticationResult user2Result = authCache.getCachedAuthResult(user2Creds, tIP);
        Assert.assertNotNull(user2Result);
        Assert.assertEquals(user2Result.getUser().getLogin(), "user2");
    }

    private TestIdentityProvider setupSSHSessionTest(String user1, String user1Key, String user2, String user2Key) {
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG){
            @Override
            public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
                return super.authenticate(pc);
            }
        };
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), user1);
        TestIdentityProvider.addUser(ub, user1, user1Key);

        UserBean ub2 = new UserBean(tIP.getConfig().getGoid(), user2);
        TestIdentityProvider.addUser(ub2, user2, user2Key);
        return tIP;
    }

    private LoginCredentials createSSHLoginCredentials(String user, String userKey) {
        return LoginCredentials.makeLoginCredentials(
                new SshSecurityToken(SecurityTokenType.SSH_CREDENTIAL, new SshKnob.PublicKeyAuthentication(user, userKey)),
                AllAssertion.class);
    }
}
