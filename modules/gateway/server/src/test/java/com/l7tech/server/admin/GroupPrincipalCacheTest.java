package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.objectmodel.IdentityHeader;

import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 1:56:03 PM
 *
 * 
 */
public class GroupPrincipalCacheTest extends TestCase {

    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "password";
    private static final int MAX_AGE = 1000;

    private GroupPrincipalCache cache = null;

    @Before
    protected void setUp() throws Exception{
        cache = getGroupPrincipalCache();
        assertNotNull(cache);
    }

    @After
    protected void tearDown() throws Exception{
        //stop the cache so other tests get a fresh cache
        cache.dispose();
        TestIdentityProvider.reset();
    }
    /*
    * Add a user with no group membership to the test identity provider
    * Retrieve this users Set<Principal> from the group cache
    * Outcome- no exceptions thrown. The returned Set is null.
    * */
    @Test
    public void testValidUserNoGroupMembership() throws Exception{
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        Set<GroupPrincipal> sP = cache.getCachedValidatedPrincipals(ub, tIP, MAX_AGE);
        assertNull(sP);
    }

    /*
    * Add a user with some group membership to the test identity provider
    * Retrieve this users Set<Principal> from the group cache
    * Outcome- no exceptions thrown. The returned Set contins a GroupPrincipal
    * for each group the user is a member of
    * */
    @Test
    public void testValidUserWithGroupMembership() throws Exception{
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());
        GroupManager gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        Set<GroupPrincipal> sP = cache.getCachedValidatedPrincipals(ub, tIP, MAX_AGE);
        assertNotNull(sP);
        assertTrue(sP.size() == 1);

        //test that the GroupPrincipal represents our InternalGroup iG
        GroupPrincipal gP = sP.iterator().next();
        assertNotNull(gP);
        IdentityHeader iH = gP.getGroupHeader();
        assertNotNull(iH);
        assertTrue(iH.getName().equals(iG.getName()));
    }

    /*
    * Retrieve a user from the cache which does not exist
    * Outcome - ValidationException should be thrown
    * */
    @Test
    public void testNoValidUser() throws Exception{
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);

        boolean exceptionThrown = false;
        try{
            cache.getCachedValidatedPrincipals(ub, tIP, MAX_AGE);
        }catch(ValidationException ve){
            exceptionThrown = true;            
        }
        assertTrue(exceptionThrown);
    }


    /*
    * Add a user with group membership to the TestIdentityProvider
    * Call private getCacheEntry and ensure a hit 
    * */
    @Test
    public void testValidCacheHit() throws Exception{
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        //CacheKey in GroupPrincipalCache uses User.getId, so want it to be non null
        ub.setUniqueIdentifier(ub.getLogin());
        TestIdentityProvider.addUser(ub, ub.getLogin(), PASSWORD.toCharArray());

        GroupManager gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        //Populate group membership in cache for user
        Set<GroupPrincipal> sP = cache.getCachedValidatedPrincipals(ub, tIP, MAX_AGE);
        assertNotNull(sP);
        
        Class c = cache.getClass();
        Method m = c.getDeclaredMethod("getCacheEntry", GroupPrincipalCache.CacheKey.class, String.class, IdentityProvider.class, Integer.TYPE);
        assertNotNull(m);
        m.setAccessible(true);

        GroupPrincipalCache.CacheKey cKey = new GroupPrincipalCache.CacheKey(tIP.getConfig().getOid(), ub.getLogin());
        Object o = m.invoke(cache, cKey, ub.getLogin(), tIP, MAX_AGE);
        assertNotNull(o);
        assertTrue(o instanceof Set);
        Set<GroupPrincipal> gPs = (Set<GroupPrincipal>)o;
    }
    /*
    * Add a user with group membership to the TestIdentityProvider
    * use reflection to call getCacheEntry supplying a max age which will cause
    * the cached values to be null
    * Outcome - return set is null
    * */
    @Test
    public void testValidUserCacheExpired() throws Exception{
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getOid(), USER_NAME);
        //CacheKey in GroupPrincipalCache uses User.getId, so want it to be non null
        ub.setUniqueIdentifier(ub.getLogin());
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        GroupManager gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        //Populate group membership in cache for user
        Set<GroupPrincipal> sP = cache.getCachedValidatedPrincipals(ub, tIP, MAX_AGE);
        assertNotNull(sP);

        //Call the private getCacheEntry method of GroupPrincipalCache to ensure that the expire
        //logic is working correctly. If we call the public getCachedValidatedPrincipals it will just
        //create a new entry if it finds that it's current value has expired
        //getCacheEntry(ckey, u.getLogin(), ip, maxAge);

        Class c = cache.getClass();
        Method m = c.getDeclaredMethod("getCacheEntry", GroupPrincipalCache.CacheKey.class, String.class, IdentityProvider.class, Integer.TYPE);
        assertNotNull(m);
        m.setAccessible(true);
        //Sleep just to ensure that the expiry time of 1 second below will work
        Thread.sleep(1000);
        GroupPrincipalCache.CacheKey cKey = new GroupPrincipalCache.CacheKey(tIP.getConfig().getOid(), ub.getLogin());
        Object o = m.invoke(cache, cKey, ub.getLogin(), tIP, 1);
        assertNull(o);
    }

    /*
    * Get the cache via reflection as it's a singleton, and want a separate cache per test method
    * */
    private GroupPrincipalCache getGroupPrincipalCache() throws Exception{
        Class cacheClass = Class.forName("com.l7tech.server.admin.GroupPrincipalCache");
        Constructor<GroupPrincipalCache> c = cacheClass.getDeclaredConstructor();
        c.setAccessible(true);
        GroupPrincipalCache cache = c.newInstance();
        return cache;
    }
}
