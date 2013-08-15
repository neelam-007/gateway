package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
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
        Assert.assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        Assert.assertTrue("Cache should have hit", invocationsBefore == authInvocations[0]);

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
        Assert.assertNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        Assert.assertTrue("Cache should have hit", invocationsBefore == authInvocations[0]);

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
}