package com.ca.siteminder;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.objectmodel.Goid;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAgentContextCacheManagerTest {

    private SiteMinderAgentContextCacheManager manager;

    @Before
    public void setUp() {
        manager = new SiteMinderAgentContextCacheManagerImpl();
    }

    @After
    public void tearDown() {
        manager.removeAllCaches();
    }

    @Test
    public void testCreateCache_AllEnabled() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 10, 300000, 10, 300000, 10, 300000);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNotNull(cache.getAuthenticationCache());
        assertNotNull(cache.getAuthorizationCache());
        assertNotNull(cache.getResourceCache());
    }

    @Test
    public void testCreateCache_ResourceCacheDisabled() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 0, 300000, 10, 300000, 10, 300000);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNotNull(cache.getAuthenticationCache());
        assertNotNull(cache.getAuthorizationCache());
        assertNull(cache.getResourceCache());
    }

    @Test
    public void testCreateCache_AuthenticationCacheDisabled() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 10, 300000, 0, 300000, 10, 300000);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNull(cache.getAuthenticationCache());
        assertNotNull(cache.getAuthorizationCache());
        assertNotNull(cache.getResourceCache());
    }

    @Test
    public void testCreateCache_AuthorizationCacheDisabled() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 10, 300000, 10, 300000, 0, 300000);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNotNull(cache.getAuthenticationCache());
        assertNull(cache.getAuthorizationCache());
        assertNotNull(cache.getResourceCache());
    }

    @Test
    public void testCreateCache_AllCachesDisabled() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 0, 300000, 0, 300000, 0, 300000);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNull(cache.getAuthenticationCache());
        assertNull(cache.getAuthorizationCache());
        assertNull(cache.getResourceCache());
    }
}
