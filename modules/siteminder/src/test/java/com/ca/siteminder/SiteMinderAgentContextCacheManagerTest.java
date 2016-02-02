package com.ca.siteminder;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAgentContextCacheManagerTest {

    @Test
    public void testCreateCache() {
        SiteMinderAgentContextCacheManager manager = new SiteMinderAgentContextCacheManagerImpl();

        manager.createCache(Goid.DEFAULT_GOID, "sm1", 10, 10, 10);

        SiteMinderAgentContextCache cache = manager.getCache(Goid.DEFAULT_GOID, "sm1");

        assertNotNull(cache);
        assertNotNull(cache.getAuthenticationCache());
        assertNotNull(cache.getAuthorizationCache());
        assertNotNull(cache.getResourceCache());
    }
}
