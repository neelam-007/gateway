package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.objectmodel.IdentityHeader;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 1:56:03 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupCacheTest {

    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "password";
    private static final Goid PROVIDER_ID = new Goid(0,1L);

    private GroupCache cache = null;
    @Mock IdentityProvider identityProvider;
    @Mock
    private GroupManager groupManager;

    @Before
    public void setUp() throws Exception{
        cache = getGroupCache();
        assertNotNull(cache);

        final IdentityProviderConfig config = new IdentityProviderConfig();
        config.setGoid(PROVIDER_ID);
        when(identityProvider.getConfig()).thenReturn(config);
        when(identityProvider.getGroupManager()).thenReturn(groupManager);
    }

    @After
    public void tearDown() throws Exception{
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
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        Set<IdentityHeader> sP = cache.getCachedValidatedGroups(ub, tIP, false);
        assertNull(sP);
    }

    /*
    * Add an expired user with no group membership to the test identity provider
    * Retrieve this users Set<Principal> from the group cache
    * Outcome- no exceptions thrown. The returned Set is null.
    * */
    @Test
    public void testExpiredUserNoGroupMembership() throws Exception{
        doExpiredUserTest(true);
    }
    
    @Test
    public void testFailingExpiredUserNoGroupMembership() throws Exception{
        try {
            doExpiredUserTest(false);
            fail("Expected ValidationException");
        } catch (ValidationException e) {
            // expected
        }
    }


    private void doExpiredUserTest(boolean skipAccountValidation) throws ValidationException {
        TestIdentityProvider tIP = new TestIdentityProvider(TestIdentityProvider.TEST_IDENTITY_PROVIDER_CONFIG);
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        ub.setPasswordExpiry(System.currentTimeMillis()-1);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        Set<IdentityHeader> sP = cache.getCachedValidatedGroups(ub, tIP, skipAccountValidation);
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
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());
        GroupManager<User,Group> gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        Set<IdentityHeader> sP = cache.getCachedValidatedGroups(ub, tIP, false);
        assertNotNull(sP);
        assertTrue(sP.size() == 1);

        //test that the GroupPrincipal represents our InternalGroup iG
        IdentityHeader iH = sP.iterator().next();
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
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);

        boolean exceptionThrown = false;
        try{
            cache.getCachedValidatedGroups(ub, tIP, false);
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
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        //CacheKey in GroupPrincipalCache uses User.getId, so want it to be non null
        ub.setUniqueIdentifier(ub.getLogin());
        TestIdentityProvider.addUser(ub, ub.getLogin(), PASSWORD.toCharArray());

        GroupManager<User,Group> gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        //Populate group membership in cache for user
        Set<IdentityHeader> sP = cache.getCachedValidatedGroups(ub, tIP, false);
        assertNotNull(sP);
        
        Class c = cache.getClass();
        Method m = c.getDeclaredMethod("getCacheEntry", GroupCache.CacheKey.class, String.class, IdentityProvider.class, Integer.TYPE);
        assertNotNull(m);
        m.setAccessible(true);

        GroupCache.CacheKey cKey = new GroupCache.CacheKey(tIP.getConfig().getGoid(), EntityType.USER, ub.getLogin());
        Object o = m.invoke(cache, cKey, ub.getLogin(), tIP, 1000);
        assertNotNull(o);
        assertTrue(o instanceof Set);
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
        UserBean ub = new UserBean(tIP.getConfig().getGoid(), USER_NAME);
        //CacheKey in GroupPrincipalCache uses User.getId, so want it to be non null
        ub.setUniqueIdentifier(ub.getLogin());
        TestIdentityProvider.addUser(ub, USER_NAME, PASSWORD.toCharArray());

        GroupManager<User,Group>  gM = tIP.getGroupManager();
        Set<Group> groupSet = new HashSet<Group>();
        InternalGroup iG = new InternalGroup("Group1");
        groupSet.add(iG);
        gM.addUser(ub, groupSet);

        //Populate group membership in cache for user
        Set<IdentityHeader> sP = cache.getCachedValidatedGroups(ub, tIP, false);
        assertNotNull(sP);

        //Call the private getCacheEntry method of GroupPrincipalCache to ensure that the expire
        //logic is working correctly. If we call the public getCachedValidatedPrincipals it will just
        //create a new entry if it finds that it's current value has expired
        //getCacheEntry(ckey, u.getLogin(), ip, maxAge);

        Class c = cache.getClass();
        Method m = c.getDeclaredMethod("getCacheEntry", GroupCache.CacheKey.class, String.class, IdentityProvider.class, Integer.TYPE);
        assertNotNull(m);
        m.setAccessible(true);
        //Sleep just to ensure that the expiry time of 1 second below will work
        Thread.sleep(1000);
        GroupCache.CacheKey cKey = new GroupCache.CacheKey(tIP.getConfig().getGoid(), EntityType.USER, ub.getLogin());
        Object o = m.invoke(cache, cKey, ub.getLogin(), tIP, 1);
        assertNull(o);
    }

    @Test
    public void getCachedGroups() throws FindException {
        when(groupManager.getGroupHeadersForNestedGroup(Goid.toString(new Goid(0,1234L)))).thenReturn(Collections.singleton(new IdentityHeader(PROVIDER_ID, new Goid(0,1L), EntityType.GROUP, null, null, null, 0)));
        final InternalGroup group = new InternalGroup("TestGroup");
        group.setGoid(new Goid(0,1234L));
        final Set<IdentityHeader> result = cache.getCachedGroups(group, identityProvider);
        assertEquals(1, result.size());
    }

    @Test
    public void getCachedGroupsNone() throws FindException {
        when(groupManager.getGroupHeadersForNestedGroup("1234")).thenReturn(Collections.emptySet());
        final InternalGroup group = new InternalGroup("TestGroup");
        group.setGoid(new Goid(0,1234L));
        assertTrue(cache.getCachedGroups(group, identityProvider).isEmpty());
    }

    @Test
    public void getCachedGroupsOverMax() throws Exception {
        final GroupCache cacheWith1MaxGroup = getGroupCache(1);
        final Set<IdentityHeader> groups = new HashSet<>();
        groups.add(new IdentityHeader(PROVIDER_ID, new Goid(0,1L), EntityType.GROUP, null, null, null, 0));
        groups.add(new IdentityHeader(PROVIDER_ID, new Goid(0,2L), EntityType.GROUP, null, null, null, 0));
        when(groupManager.getGroupHeadersForNestedGroup(Goid.toString(new Goid(0,1234L)))).thenReturn(groups);
        final InternalGroup group = new InternalGroup("TestGroup");
        group.setGoid(new Goid(0,1234L));

        final Set<IdentityHeader> result = cacheWith1MaxGroup.getCachedGroups(group, identityProvider);
        assertEquals(1, result.size());
    }

    /*
    * Get the cache via reflection as it's a singleton, and want a separate cache per test method
    * */
    private GroupCache getGroupCache() throws Exception{
        return getGroupCache(50);
    }

    private GroupCache getGroupCache(final int maxGroups) throws Exception{
        return new GroupCache( "testgroupcache", 100, 1000, maxGroups );
    }
}
