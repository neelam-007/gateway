package com.l7tech.common.io;

import com.l7tech.test.BugId;
import com.whirlycott.cache.Cache;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class WhirlycacheFactoryTest {

    /**
     * Test that the most recent entry in an LRU cache is available after being stored.
     */
    @Test
    public void testLruCache() {
        final Cache cache = WhirlycacheFactory.createCache( "lru test", 10, 1, WhirlycacheFactory.POLICY_LRU );
        try {
            final long startTime = System.currentTimeMillis();
            while ( startTime + 1400L > System.currentTimeMillis() ) {
                final String key = UUID.randomUUID().toString();
                final String value = UUID.randomUUID().toString();

                cache.store( key, value );
                Thread.sleep( 10L );
                assertEquals( "Cached value", value, cache.retrieve( key ) );
            }
        } catch ( InterruptedException e ) {
            fail( "Interrupted" );
        } finally {
            WhirlycacheFactory.shutdown( cache );
        }
    }

    /**
     * Test that the LFU cache policy doesn't fail under heavy concurrent load.
     * This test eventually results in "IllegalArgumentException: Comparison method violates its general contract!"
     * since the access count for an item may be changed by another thread while the cleaner thread is sorting
     * the cache contents by access count.
     */
    @Test
    @BugId("SSG-6661")
    @Ignore("Currently disabled because it runs forever and fills all memory after the cleaner thread dies")
    public void testLfuCacheComparator() throws Exception {
        final Cache cache = WhirlycacheFactory.createCache("testcache", 10, 5, WhirlycacheFactory.POLICY_LFU);

        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < 10000; ++i) {
            executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    exerciseCache(cache);
                    System.out.println("Finished a batch");
                    return null;
                }
            });
        }

        executor.awaitTermination(2000, TimeUnit.SECONDS);
    }


    void exerciseCache(Cache cache) {
        for (int j = 0; j < 10000000; ++j) {
            for (int i = 0; i < 100; ++i) {
                cache.store("val" + i, "blah" + i + " " + j);
            }
            for (int i = 0; i < 100; ++i) {
                cache.retrieve("val" + i);
            }
        }
    }
}
