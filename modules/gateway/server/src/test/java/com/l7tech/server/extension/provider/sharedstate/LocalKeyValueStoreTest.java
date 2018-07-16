package com.l7tech.server.extension.provider.sharedstate;

import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LocalKeyValueStoreTest {

    private LocalKeyValueStore<String, String> underTest;
    private static final String mapName = "localStore";

    @Before
    public void setup() {
        underTest = new LocalKeyValueStore<>(mapName);
    }

    /**
     * when the key value store is empty returns true
     * when the key value store is not empty returns false
     */
    @Test
    public void testIsEmpty() {
        assertTrue(underTest.isEmpty());

        underTest.put("test", "abc");
        assertFalse(underTest.isEmpty());
    }

    /**
     * When calling clear, expect key value store to be empty
     */
    @Test
    public void testClear() {
        underTest.put("test", "abc");
        assertFalse(underTest.isEmpty());

        underTest.clear();
        assertTrue(underTest.isEmpty());
    }

    /**
     * When the key exists, contains key returns true otherwise false
     * when the key is removed, contains key returns false
     */
    @Test
    public void testContainsKey() {
        assertFalse(underTest.containsKey("test"));

        underTest.put("test", "abc");
        assertTrue(underTest.containsKey("test"));

        underTest.remove("test");
        assertFalse(underTest.containsKey("test"));
    }

    /**
     * When get is called and no mapping exists, return null
     * when get is called and a mapping exists, return the value
     */
    @Test
    public void testGet() {
        assertNull(underTest.get("test"));

        underTest.put("test", "abc");
        assertEquals("abc", underTest.get("test"));
    }

    /**
     * When put is called, expect new value in key store and return previous value or null
     */
    @Test
    public void testPut() {
        assertNull(underTest.put("test", "abc"));
        assertEquals("abc", underTest.get("test"));
    }

    /**
     * When put is called, expect new value in key store and return previous value or null
     * if the entry does not exist or has expired
     */
    @Test(timeout = 1000)
    public void testPutWithTtl() {
        int ttl = 50;
        assertNull(underTest.put("test", "abc", ttl, TimeUnit.MILLISECONDS));
        assertEntryIsExpired("test");
    }

    /**
     * Test an entry with a TTL of zero doesn't expire
     */
    @Test(timeout = 1000)
    public void testPutWithTtl_ttlIsZero_doesntExpire() {
        assertNull(underTest.put("test", "abc", 0, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 30; i++) {
            assertTrue(underTest.containsKey("test"));
        }
    }

    /**
     * Test an entry with a negative TTL results in map default (always infinite for LocalKeyValueStore)
     */
    @Test(timeout = 1000)
    public void testPutWithTtl_ttlIsNegative_expiryIsMapDefault() {
        assertNull(underTest.put("test", "abc", -1, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 30; i++) {
            assertTrue(underTest.containsKey("test"));
        }
    }

    /**
     * When putIfCondition is called, put should be performed only if the previous map value meets the condition
     */
    @Test
    public void putIfCondition() {
        // Using a TTL of zero (meaning infinite) as we are not interested in the expiry behaviour for this test case
        assertTrue(underTest.putIfCondition("test", "value", Objects::isNull, 0, TimeUnit.MILLISECONDS));
        assertFalse(underTest.putIfCondition("test", "updated", Objects::isNull, 0, TimeUnit.MILLISECONDS));
        assertEquals("value", underTest.get("test"));
        assertTrue(underTest.putIfCondition("test", "newValue", v -> v.equals("value"), 0, TimeUnit.MILLISECONDS));
        assertEquals("newValue", underTest.get("test"));
        assertFalse(underTest.putIfCondition("test", "shouldntUpdate", v -> v.contains("blah"), 0, TimeUnit.MILLISECONDS));
    }

    /**
     * When set is called, expect new value in key store
     */
    @Test
    public void testSet() {
        assertNull(underTest.get("test"));
        underTest.set("test", "def");
        assertEquals("def", underTest.get("test"));
    }

    /**
     * When set is called, expect new value in key store if the entry exists and has not expired
     */
    @Test(timeout=1000)
    public void testSetWithTtl() {
        int ttl = 50;
        underTest.set("test", "abc", ttl, TimeUnit.MILLISECONDS);
        assertEntryIsExpired("test");
    }

    /**
     * When compute is called, expect the new value
     */
    @Test
    public void testCompute() {
        assertEquals("abc", (underTest.compute("test", (k,v) -> "abc")));
        assertEquals("abcdef", underTest.compute("test", (k,v) -> v + "def"));
    }

    /**
     * When computeIfAbsent is called,
     * - expect the computed value if key value store does not have the key
     * - expect the old value if key value store has the key
     */
    @Test
    public void testComputeIfAbsent() {
        assertFalse(underTest.containsKey("test"));
        assertEquals("abc", underTest.computeIfAbsent("test", (v) -> "abc"));

        underTest.put("test1", "xyz");
        assertEquals("xyz", underTest.computeIfAbsent("test1", (v) -> "abc"));
    }

    /**
     * When computeIfPresent is called,
     * - expect null if key value store does not have the key
     * - expect the computed value if key value store has the key
     */
    @Test
    public void testComputeIfPresent() {
        assertFalse(underTest.containsKey("test"));
        assertNull(underTest.computeIfPresent("test", (k, v) -> "abc"));
        assertFalse(underTest.containsKey("test"));

        underTest.put("test1", "xyz");
        assertTrue(underTest.containsKey("test1"));
        assertEquals("abc", underTest.computeIfPresent("test1", (k, v) -> "abc"));
    }

    /**
     * When remove is called, expect the key-value pair associated with the key to be removed and returns
     * - expect null if key value store does not have the key
     * - expect the value associated with key if the store contained an entry for key
     */
    @Test
    public void testRemove() {
        assertFalse(underTest.containsKey("test"));
        underTest.put("test", "abc");
        assertTrue(underTest.containsKey("test"));
        assertEquals("abc", underTest.remove("test"));
        assertFalse(underTest.containsKey("test"));
        assertNull(underTest.remove("nonExistentKey"));
    }

    /**
     * When remove is called, expect the key-value pair associated with the key to be removed
     */
    @Test
    public void testDelete() {
        assertFalse(underTest.containsKey("test"));
        underTest.put("test", "abc");
        assertTrue(underTest.containsKey("test"));
        underTest.delete("test");
        assertFalse(underTest.containsKey("test"));
        underTest.delete("nonExistentKey"); // Testing that removing a non-existent key will not throw an exception 
    }

    private void assertEntryIsExpired(final String key) {
        for(;;) {
            if (underTest.get(key) == null) {
                return;
            }
        }
    }
}
