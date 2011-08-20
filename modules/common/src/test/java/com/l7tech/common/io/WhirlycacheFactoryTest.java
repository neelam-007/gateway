package com.l7tech.common.io;

import com.whirlycott.cache.Cache;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.UUID;

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
}
