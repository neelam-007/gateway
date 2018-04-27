package com.ca.siteminder;

import com.l7tech.objectmodel.Goid;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAgentContextCacheManagerTest {

    private SiteMinderAgentContextCacheManager manager;
    private List<SiteMinderAgentContextCache.AgentContextSubCache> subCaches;

    @Before
    public void setUp() {
        manager = new SiteMinderAgentContextCacheManagerImpl();
        subCaches = new ArrayList<>();
    }

    private void setupSubCaches(int resMaxSize, long resMaxAge,
                                int authnMaxSize, long authnMaxAge,
                                int authzMaxSize, long authzMaxAge,
                                int acoMaxSize, long acoMaxAge) {
        subCaches.clear();
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE,
                resMaxSize, resMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION,
                authnMaxSize, authnMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION,
                authzMaxSize, authzMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO,
                acoMaxSize, acoMaxAge));
    }

    private SiteMinderAgentContextCache.AgentContextSubCache verifySubCache(SiteMinderAgentContextCache agentCache,
                                                                            SiteMinderAgentContextCache.AgentContextSubCacheType cacheType) {
        SiteMinderAgentContextCache.AgentContextSubCache subCache = agentCache.getSubCache(cacheType);
        assertNotNull(subCache);
        assertNotNull(subCache.getCache());
        return subCache;
    }

    private SiteMinderAgentContextCache.AgentContextSubCache verifyDisabledSubCache(SiteMinderAgentContextCache agentCache,
                                                                                    SiteMinderAgentContextCache.AgentContextSubCacheType cacheType) {
        SiteMinderAgentContextCache.AgentContextSubCache subCache = agentCache.getSubCache(cacheType);
        assertNotNull(subCache);
        assertNull(subCache.getCache());
        return subCache;
    }

    @After
    public void tearDown() {
        manager.removeAllCaches();
    }

    @Test
    public void testCreateCache_AllEnabled() {
        setupSubCaches(
                10, 100000, // resource
                20, 200000, // authentication
                30, 300000, // authorization
                40, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);

        SiteMinderAgentContextCache.AgentContextSubCache subCache;
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");
        assertNotNull(cache);

        subCache = verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        assertEquals(10, subCache.getMaxSize());
        assertEquals(100000, subCache.getMaxAge());

        subCache = verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        assertEquals(20, subCache.getMaxSize());
        assertEquals(200000, subCache.getMaxAge());

        subCache = verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        assertEquals(30, subCache.getMaxSize());
        assertEquals(300000, subCache.getMaxAge());

        subCache = verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
        assertEquals(40, subCache.getMaxSize());
        assertEquals(400000, subCache.getMaxAge());
    }

    @Test
    public void testCreateCache_ResourceCacheDisabled() {
        setupSubCaches(
                0, 100000, // resource
                20, 200000, // authentication
                30, 300000, // authorization
                40, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
    }

    @Test
    public void testCreateCache_AuthenticationCacheDisabled() {
        setupSubCaches(
                10, 100000, // resource
                0, 200000, // authentication
                30, 300000, // authorization
                40, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
    }

    @Test
    public void testCreateCache_AuthorizationCacheDisabled() {
        setupSubCaches(
                10, 100000, // resource
                20, 200000, // authentication
                0, 300000, // authorization
                40, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
    }

    @Test
    public void testCreateCache_AgentConfigObjectCacheDisabled() {
        setupSubCaches(
                10, 100000, // resource
                20, 200000, // authentication
                30, 300000, // authorization
                0, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        verifySubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
    }

    @Test
    public void testCreateCache_AllCachesDisabled() {
        setupSubCaches(
                0, 100000, // resource
                0, 200000, // authentication
                0, 300000, // authorization
                0, 400000); // agent config object
        manager.createCache(Goid.DEFAULT_GOID, "sm1", subCaches);
        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION);
        verifyDisabledSubCache(cache, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO);
    }
}
