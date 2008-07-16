package com.l7tech.server.identity;

import junit.framework.TestCase;
import com.l7tech.identity.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.ServerConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 15, 2008
 * Time: 8:53:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class AuthCacheTest extends TestCase {

    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "password";
    private static final int IDENTITY_LOOKUP_DELAY = 1000;//milliseconds
    private static final int MAX_AGE = 10000;
    /*AuthCache is static variable so need to know in teardown to reset any changes*/
    private boolean needToReset = false;

    /*Override authenticate in TestIdentityProvider so we can mock the hit in contacting the identity provider*/
    private class AuthCacheTestIdentityProvider extends TestIdentityProvider{

        public AuthCacheTestIdentityProvider(IdentityProviderConfig ipc){
            super(ipc);
        }

        public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
            try {
                System.out.println("Identity Provider Lookup overhead...:" + System.currentTimeMillis());
                Thread.sleep(IDENTITY_LOOKUP_DELAY);
                System.out.println("Return from Identity Provider Lookup...:" + System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.authenticate(pc);
        }
        
    }

    /*TestIdentityProvider uses static variables so reset it's users between tests*/
    public void setUp() throws Exception{
        TestIdentityProvider.clearAllUsers();
    }

    /*
    * Test the success cache works
    * Tests that the Cache hits when it should, missed when it's entries are too old yet still exist, and that the
    * Cache is cleaned up internally removing old entries
    * */
    public void testSuccessCache() throws Exception{
        LoginCredentials lc = LoginCredentials.makePasswordCredentials(USER_NAME, PASSWORD.toCharArray(), this.getClass());
        TestIdentityProvider tIP = new AuthCacheTestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        assertNotNull(tIP);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        ub.setCleartextPassword(PASSWORD);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        AuthCache aC = AuthCache.getInstance();

        //First authenticate causes the idp to be contacted
        assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));

        //Second should hit and no delay should be experienced
        long startTime = System.currentTimeMillis();
        assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        long endTime = System.currentTimeMillis();

        //The entire authenticate time should be less 5 secs as authenticate above should not have been called
        assertTrue("Cache should have hit",(endTime - startTime) < IDENTITY_LOOKUP_DELAY);

        //Sleep off the IDENTITY_LOOKUP_DELAY seconds, so the max age below finds expired hits
        Thread.sleep(IDENTITY_LOOKUP_DELAY);
        //Third authenticate should be a miss due to max ages
        startTime = System.currentTimeMillis();
        System.out.println("Start time: " + startTime);
        assertNotNull(aC.getCachedAuthResult(lc, tIP, 5, 5));
        endTime = System.currentTimeMillis();
        System.out.println("End time: " + endTime);

        //The entire authenticate time should be > 5 secs as authenticate above should have been called
        assertTrue("Cache should have missed",(endTime - startTime) >= IDENTITY_LOOKUP_DELAY);

        //Sleep off the 59 seconds used for cleaning up the success cache. After this all previous hits should
        //be gone
        Thread.sleep(60000);

        //Fourth authenticate should be a cache miss as the cache has completely expired
        startTime = System.currentTimeMillis();
        System.out.println("Start time: " + startTime);
        assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        endTime = System.currentTimeMillis();
        System.out.println("End time: " + endTime);

        //The entire authenticate time should be > IDENTITY_LOOKUP_DELAY secs as authenticate above should have been called
        assertTrue("Cache should have missed",(endTime - startTime) >= IDENTITY_LOOKUP_DELAY);

    }

    /*
    * Test the failure cache works
    * Tests that the Cache hits when it should, missed when it's entries are too old yet still exist, and that the
    * Cache is cleaned up internally removing old entries
    * */
    public void testFailureCache() throws Exception{
        LoginCredentials lc = LoginCredentials.makePasswordCredentials(USER_NAME+"miss", PASSWORD.toCharArray(), this.getClass());
        TestIdentityProvider tIP = new AuthCacheTestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        assertNotNull(tIP);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        ub.setCleartextPassword(PASSWORD);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        AuthCache aC = AuthCache.getInstance();

        //First authenticate causes the idp to be contacted
        assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));

        //Second should hit and no delay should be experienced
        long startTime = System.currentTimeMillis();
        assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        long endTime = System.currentTimeMillis();

        //The entire authenticate time should be less 5 secs as authenticate above should not have been called
        assertTrue("Cache should have hit",(endTime - startTime) < IDENTITY_LOOKUP_DELAY);

        //Sleep off IDENTITY_LOOKUP_DELAY seconds, so the max age below finds expired hits
        Thread.sleep(IDENTITY_LOOKUP_DELAY);
        //Third authenticate should be a miss due to max ages
        startTime = System.currentTimeMillis();
        System.out.println("Start time: " + startTime);
        assertNull(aC.getCachedAuthResult(lc, tIP, 5, 5));
        endTime = System.currentTimeMillis();
        System.out.println("End time: " + endTime);

        //The entire authenticate time should be > 5 secs as authenticate above should have been called
        assertTrue("Cache should have missed",(endTime - startTime) >= IDENTITY_LOOKUP_DELAY);

        //Sleep off the 61 seconds used for cleaning up the failure cache. After this all previous hits should
        //be gone
        Thread.sleep(62000);

        //Fourth authenticate should be a cache miss as the cache has completely expired
        startTime = System.currentTimeMillis();
        System.out.println("Start time: " + startTime);
        assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        endTime = System.currentTimeMillis();
        System.out.println("End time: " + endTime);

        //The entire authenticate time should be > IDENTITY_LOOKUP_DELAY secs as authenticate above should have been called
        System.out.println("Time: " + (endTime - startTime));
        assertTrue("Cache should have missed",(endTime - startTime) >= IDENTITY_LOOKUP_DELAY);
    }

    /*
    * Test that the success cache works correctly when the failure cache is disabled
    * */
    public void testSuccessCacheIndependent() throws Exception{
        //Set failure cache == 0, which should disable it
        Class c = Class.forName("com.l7tech.server.identity.AuthCache");
        Field failureSizeField = c.getDeclaredField("FAILURE_CACHE_SIZE");
        failureSizeField.setAccessible(true);
        Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        int modifier = modifierField.getInt(failureSizeField);
        // blank out the final bit in the modifiers int
        modifier &= ~Modifier.FINAL;
        modifierField.setInt(failureSizeField, modifier);
        failureSizeField.setInt(c,0);
        needToReset = true;

        recreateAuthCache();
        AuthCache aC = AuthCache.getInstance();
        //Confirm internal state
        Field failureDisabledField = c.getDeclaredField("failureCacheDisabled");
        failureDisabledField.setAccessible(true);
        assertTrue(failureDisabledField.getBoolean(aC)); 

        TestIdentityProvider tIP = new AuthCacheTestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        assertNotNull(tIP);

        String userName = USER_NAME+"1";
        UserBean ub1 = new UserBean(tIP.getConfig().getOid(), userName);
        ub1.setCleartextPassword(PASSWORD);
        TestIdentityProvider.addUser(ub1, userName, PASSWORD.toCharArray());
        LoginCredentials lc1 = LoginCredentials.makePasswordCredentials(userName, PASSWORD.toCharArray(), this.getClass());
        assertNotNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));

        addSomeUsers(5, tIP, aC);
        //Should be cache hit
        long startTime = System.currentTimeMillis();
        assertNotNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        long endTime = System.currentTimeMillis();
        assertTrue("Cache should have hit",(endTime - startTime) < IDENTITY_LOOKUP_DELAY);
    }

    /*
    * Test that the failure cache works correctly when the success cache is disabled
    * */
    public void testFailureCacheIndependent() throws Exception{
        //Set failure cache == 0, which should disable it
        Class c = Class.forName("com.l7tech.server.identity.AuthCache");
        Field successSizeField = c.getDeclaredField("SUCCESS_CACHE_SIZE");
        successSizeField.setAccessible(true);
        Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        int modifier = modifierField.getInt(successSizeField);
        // blank out the final bit in the modifiers int
        modifier &= ~Modifier.FINAL;
        modifierField.setInt(successSizeField, modifier);
        successSizeField.setInt(c,0);
        needToReset = true;
        
        recreateAuthCache();
        AuthCache aC = AuthCache.getInstance();
        //Confirm internal state
        Field successDisabledField = c.getDeclaredField("successCacheDisabled");
        successDisabledField.setAccessible(true);
        assertTrue(successDisabledField.getBoolean(aC));

        //now that we know the failure cache has been disabled, call the success test case above
        TestIdentityProvider tIP = new AuthCacheTestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        assertNotNull(tIP);

        LoginCredentials lc1 = LoginCredentials.makePasswordCredentials(USER_NAME+"1", PASSWORD.toCharArray(), this.getClass());
        assertNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        for(int i = 0; i < 5; i++){
            LoginCredentials lc = LoginCredentials.makePasswordCredentials(USER_NAME+"a"+i, PASSWORD.toCharArray(), this.getClass());
            assertNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        }

        //Should be cache hit
        long startTime = System.currentTimeMillis();
        assertNull(aC.getCachedAuthResult(lc1, tIP, MAX_AGE, MAX_AGE));
        long endTime = System.currentTimeMillis();
        assertTrue("Cache should have hit",(endTime - startTime) < IDENTITY_LOOKUP_DELAY);
    }

    public void tearDown() throws Exception{
        if(!needToReset) return;
        Class c = Class.forName("com.l7tech.server.identity.AuthCache");
        Field successSizeField = c.getDeclaredField("SUCCESS_CACHE_SIZE");
        successSizeField.setAccessible(true);
        Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        int modifier = modifierField.getInt(successSizeField);
        modifier &= ~Modifier.FINAL;
        modifierField.setInt(successSizeField, modifier);
        successSizeField.setInt(c, ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_AUTH_CACHE_SUCCESS_CACHE_SIZE, 200));

        Field failureSizeField = c.getDeclaredField("FAILURE_CACHE_SIZE");
        failureSizeField.setAccessible(true);
        modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        modifier = modifierField.getInt(failureSizeField);
        modifier &= ~Modifier.FINAL;
        modifierField.setInt(failureSizeField, modifier);
        failureSizeField.setInt(c,ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_AUTH_CACHE_FAILURE_CACHE_SIZE, 100));
    }

    private void addSomeUsers(int numUsers, TestIdentityProvider tIP, AuthCache aC) throws Exception{
        for(int i = 0; i < numUsers; i++){
            String userName = USER_NAME+"users"+i;
            UserBean ub = new UserBean(tIP.getConfig().getOid(), userName);
            ub.setCleartextPassword(PASSWORD);
            TestIdentityProvider.addUser(ub, userName, PASSWORD.toCharArray());
            LoginCredentials lc = LoginCredentials.makePasswordCredentials(userName, PASSWORD.toCharArray(), this.getClass());
            assertNotNull(aC.getCachedAuthResult(lc, tIP, MAX_AGE, MAX_AGE));
        }
    }

    /*
    * Helper method used when a test needs a fresh instance of AuthCache
    * This will create two instances of AuthCache as a side affect
    * */
    private void recreateAuthCache() throws Exception{
        Class c = Class.forName("com.l7tech.server.identity.AuthCache$InstanceHolder");
        Field instanceField = c.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);

        Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        int modifier = modifierField.getInt(instanceField);
        // blank out the final bit in the modifiers int
        modifier &= ~Modifier.FINAL;
        modifierField.setInt(instanceField, modifier);

        Constructor con = Class.forName("com.l7tech.server.identity.AuthCache").getDeclaredConstructor(null);
        con.setAccessible(true);
        AuthCache aC = (AuthCache) con.newInstance(new Object[]{});

        instanceField.set(c,aC);
    }    
}