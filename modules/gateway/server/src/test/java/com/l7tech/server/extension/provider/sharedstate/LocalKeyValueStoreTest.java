package com.l7tech.server.extension.provider.sharedstate;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LocalKeyValueStoreTest {
    private LocalKeyValueStore underTest;

    @Before
    public void setup() {
        underTest = new LocalKeyValueStore();
    }

    /**
     * when the key value store is empty returns true
     * when the key value store is not empty returns false
     */
    @Test
    public void testIsEmpty() {
        assertEquals(true, underTest.isEmpty());

        underTest.put("test", "abc");
        assertEquals(false, underTest.isEmpty());
    }

    /**
     * When calling clear, expect key value store to be empty
     */
    @Test
    public void testClear() {
        underTest.put("test", "abc");
        assertEquals(false, underTest.isEmpty());

        underTest.clear();
        assertEquals(true, underTest.isEmpty());
    }

    /**
     * When the key exists, contains key returns true otherwise false
     * when the key is removed, contains key returns false
     */
    @Test
    public void testContainsKey() {
        assertEquals(false, underTest.containsKey("test"));

        underTest.put("test", "abc");
        assertEquals(true, underTest.containsKey("test"));

        underTest.remove("test");
        assertEquals(false, underTest.containsKey("test"));
    }

    /**
     * When get is called and no mapping exists, return null
     * when get is called and a mapping exists, return the value
     */
    @Test
    public void testGet() {
        assertEquals(null, underTest.get("test"));

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
        assertEquals(null, underTest.computeIfPresent("test", (k, v) -> "abc"));
        assertFalse(underTest.containsKey("test"));

        underTest.put("test1", "xyz");
        assertTrue(underTest.containsKey("test1"));
        assertEquals("abc", underTest.computeIfPresent("test1", (k, v) -> "abc"));
    }
}
