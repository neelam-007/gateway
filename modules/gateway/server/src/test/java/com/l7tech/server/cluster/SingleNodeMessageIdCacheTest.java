package com.l7tech.server.cluster;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class SingleNodeMessageIdCacheTest {
    MessageIdCache cache = new SingleNodeMessageIdCache("myfunkynodeid");

    @Test
    public void testToDatabaseId() {
        assertEquals("myfunkynodeid12345", cache.toDatabaseId("12345"));
        assertEquals("myfunkynodeidnull", cache.toDatabaseId(null));
    }

    @Test
    public void testFromDatabaseId() {
        assertEquals("12345", cache.fromDatabaseId("myfunkynodeid12345"));
        assertNull(cache.fromDatabaseId("myfunkynodeid"));
        assertNull(cache.fromDatabaseId("myfunkynode"));
        assertNull(cache.fromDatabaseId("12345"));
        assertNull(cache.fromDatabaseId(null));
        assertNull(cache.fromDatabaseId(""));
    }
}
