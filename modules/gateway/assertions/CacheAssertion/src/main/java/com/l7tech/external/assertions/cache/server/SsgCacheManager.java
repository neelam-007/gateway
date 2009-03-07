package com.l7tech.external.assertions.cache.server;

import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.system.Stopping;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides access to cached information.
 * TODO shut down and remove caches that haven't been used recently
 */
public class SsgCacheManager {
    protected static final Logger logger = Logger.getLogger(SsgCacheManager.class.getName());
    private static final AtomicReference<SsgCacheManager> INSTANCE = new AtomicReference<SsgCacheManager>();

    private final StashManagerFactory stashManagerFactory;
    private final ConcurrentHashMap<String, SsgCache> cachesByName = new ConcurrentHashMap<String, SsgCache>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private SsgCacheManager(StashManagerFactory stashManagerFactory, ApplicationEventProxy appEventProxy) {
        this.stashManagerFactory = stashManagerFactory;
        appEventProxy.addApplicationListener(makeCloseListener());
    }

    private ApplicationListener makeCloseListener() {
        return new ApplicationListener() {
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof Stopping) {
                    close();
                }
            }
        };
    }

    /**
     * Get the current cache manager, creating a new one if necessary using the specified BeanFactory.
     *
     * @param beanFactory BeanFactory to use if a new cache manager needs to be created.  Ignored if a cache manager
     *                    already exists.  Must not be null unless caller is certain a cache manager already exists.
     * @return the cache manager instance.  Never null.
     */
    public static SsgCacheManager getInstance(BeanFactory beanFactory) {
        SsgCacheManager ret = INSTANCE.get();
        if (ret != null)
            return ret;

        synchronized (SsgCacheManager.class) {
            ret = INSTANCE.get();
            if (ret != null)
                return ret;

            StashManagerFactory smf = (StashManagerFactory)beanFactory.getBean("stashManagerFactory", StashManagerFactory.class);
            ApplicationEventProxy aep = (ApplicationEventProxy)beanFactory.getBean("applicationEventProxy", ApplicationEventProxy.class);
            ret = new SsgCacheManager(smf, aep);
            INSTANCE.set(ret);
            return ret;
        }
    }

    /**
     * Get or create a cache with the specified name.
     *
     * @param name the name of the cache to get or create.  Required.
     * @return the new or existing cache instance.  Never null.
     */
    public SsgCache getCache(String name) {
        if (closed.get()) return SsgCache.getNullCache();

        SsgCache cache = cachesByName.get(name);
        if (cache != null) return cache;

        cache = new SsgCache(stashManagerFactory);
        SsgCache existing = cachesByName.putIfAbsent(name, cache);
        if (existing != null) {
            cache.close();
            cache = existing;
        }
        return cache;
    }

    private void close() {
        closed.set(true);
        List<SsgCache> caches = new ArrayList<SsgCache>(cachesByName.values());
        for (SsgCache cache : caches)
            if (cache != null) cache.close();
    }

    /**
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        logger.info("SsgCacheManager is preparing itself to be unloaded");
        // if we haven't created one yet, avoid creating a new one just to immediately close it
        SsgCacheManager manager = INSTANCE.get();
        if (manager != null) manager.close();
    }
}
