package com.l7tech.server.extension.provider.distributedcounter;

import com.l7tech.server.extension.provider.sharedstate.LocalCounterStore;
import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;

public class LocalCounterStoreTest {
    private LocalCounterStore underTest;

    @Before
    public void setup() {
        underTest = new LocalCounterStore();
    }

    /**
     * when init counter with value, expect to get the init value
     */
    @Test
    public void testInit() {
        underTest.init("test", 100L);
        assertEquals(Long.valueOf(100), underTest.get("test"));

        underTest.init("test1", -100L);
        assertEquals(Long.valueOf(-100), underTest.get("test1"));
    }

    /**
     * when getting an uninitialized counter, expect NoSuchElementException
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetUninitializedCounter() {
        underTest.get("non-init");
    }

    /**
     * when get and increment is called, expect
     *  - return previous value
     *  - next get would return previous value + 1
     */
    @Test
    public void testGetAndIncrement() {
        underTest.init("test", 100L);
        assertEquals(Long.valueOf(100), underTest.getAndIncrement("test"));
        assertEquals(Long.valueOf(101), underTest.get("test"));
    }

    /**
     * when get and increment is called on a non-existent counter, expect NoSucheElementException
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetAndIncrementOnUninitializedCounter() {
        underTest.getAndIncrement("test");
    }

    /**
     * when increment and get is called, expect
     *  - return new value
     */
    @Test
    public void testIncrementAndGet() {
        underTest.init("test", 100L);
        assertEquals(Long.valueOf(101), underTest.incrementAndGet("test"));
    }

    /**
     * when increment and get is called on uninitialized counter, expect NoSucheElementException
     */
    @Test(expected = NoSuchElementException.class)
    public void testIncrementAndGetOnUninitializedCounter() {
        underTest.incrementAndGet("test");
    }

    /**
     * When get and update is called, expect
     *  - return previous value
     *  - next get would return the new value
     */
    @Test
    public void testGetAndUpdate() {
        underTest.init("test", 1000L);
        assertEquals(Long.valueOf(1000), underTest.getAndUpdate("test", v -> v*5));
        assertEquals(Long.valueOf(5000), underTest.get("test"));
    }


    /**
     * When get and update is called on uninitialized counter, expect NoSuchElementException
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetAndUpdateOnUninitializedCounter() {
        underTest.getAndUpdate("test", v -> v*5);
    }

    /**
     * When get and update is called, expect
     *  - return previous value
     *  - next get would return the new value
     */
    @Test
    public void testUpdateAndGet() {
        underTest.init("test", 1000L);
        assertEquals(Long.valueOf(50), underTest.updateAndGet("test", v -> v/20));
    }

    /**
     * When get and update is called on uninitialized counter, expect NoSuchElementException
     */
    @Test(expected = NoSuchElementException.class)
    public void testUpdateAndGetOnUninitializedCounter() {
        underTest.updateAndGet("test", v -> v/20);
    }
}
