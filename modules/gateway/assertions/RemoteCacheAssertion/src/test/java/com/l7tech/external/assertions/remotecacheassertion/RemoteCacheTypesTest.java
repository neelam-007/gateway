package com.l7tech.external.assertions.remotecacheassertion;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RemoteCacheTypesTest {

    @Test
    public void testGetEntityType() {
        assertEquals(RemoteCacheTypes.Memcached.getEntityType(), "memcached");
        assertEquals(RemoteCacheTypes.Terracotta.getEntityType(), "terracotta");
        assertEquals(RemoteCacheTypes.Coherence.getEntityType(), "coherence");
        assertEquals(RemoteCacheTypes.GemFire.getEntityType(), "gemfire");
        assertEquals(RemoteCacheTypes.Redis.getEntityType(), "redis");
    }

    @Test
    public void testGetEntityEnumType() {
        assertEquals(RemoteCacheTypes.getEntityEnumType("memcached"), RemoteCacheTypes.Memcached);
        assertEquals(RemoteCacheTypes.getEntityEnumType("terracotta"), RemoteCacheTypes.Terracotta);
        assertEquals(RemoteCacheTypes.getEntityEnumType("coherence"), RemoteCacheTypes.Coherence);
        assertEquals(RemoteCacheTypes.getEntityEnumType("gemfire"), RemoteCacheTypes.GemFire);
        assertEquals(RemoteCacheTypes.getEntityEnumType("redis"), RemoteCacheTypes.Redis);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEntityEnumTypeThrowsException() {
        RemoteCacheTypes.getEntityEnumType("illegalArg");
    }

    @Test
    public void testGetEntityTypes() {
        String[] types = new String[]{"memcached", "terracotta", "coherence", "gemfire", "redis"};
        assertArrayEquals(RemoteCacheTypes.getEntityTypes(), types);
        assertEquals(types.length, RemoteCacheTypes.getEntityTypes().length);
    }
}
