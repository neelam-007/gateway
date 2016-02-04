package com.ca.siteminder;

import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAgentContextCacheManagerTest {

    private SiteMinderAgentContextCacheManager manager;

    @Before
    public void setUp() {
        manager = new SiteMinderAgentContextCacheManagerImpl();
    }

    @Test
    public void testCreateCache() {
        manager.createCache(Goid.DEFAULT_GOID, "sm1", 10, 10, 10);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNotNull(cache.getAuthenticationCache());
        assertNotNull(cache.getAuthorizationCache());
        assertNotNull(cache.getResourceCache());
    }
}
