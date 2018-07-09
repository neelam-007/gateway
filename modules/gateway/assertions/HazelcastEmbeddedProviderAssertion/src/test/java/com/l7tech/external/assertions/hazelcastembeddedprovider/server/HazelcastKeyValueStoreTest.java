package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HazelcastKeyValueStoreTest {
    private static HazelcastInstance hcInstance1;
    private static HazelcastInstance hcInstance2;

    private HazelcastKeyValueStore<String, String> underTest1;
    private HazelcastKeyValueStore<String, String> underTest2;

    private static final String MAP_NAME = "testMap";

    @BeforeClass
    public static void initializeHazelcast() {
        Config config = new Config();
        GroupConfig groupConfig = config.getGroupConfig();
        groupConfig.setName("dev");
        groupConfig.setPassword("dev-pass");

        config.getNetworkConfig().setPort(NetworkConfig.DEFAULT_PORT);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);

        // setup TCPIP
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        tcpIpConfig.setConnectionTimeoutSeconds(5);
        tcpIpConfig.addMember("127.0.0.1");

        hcInstance1 = Hazelcast.newHazelcastInstance(config);
        hcInstance2 = Hazelcast.newHazelcastInstance(config);
        assert hcInstance1.getCluster().getMembers().size() == 2;
    }

    @AfterClass
    public static void shutdownHazelcast() {
        hcInstance2.shutdown();
        hcInstance1.shutdown();
    }

    @Before
    public void setup() {
        hcInstance1.getMap(MAP_NAME).destroy();

        IMap<String, String> map1 = hcInstance1.getMap(MAP_NAME);
        // Start from clean state
        map1.clear();
        underTest1 = new HazelcastKeyValueStore<>(map1);

        IMap<String, String> map2 = hcInstance2.getMap(MAP_NAME);
        // Start from clean state
        map2.clear();
        underTest2 = new HazelcastKeyValueStore<>(map2);
    }

    /**
     * When the key value store has no element
     * - expect isEmpty() to return true for all nodes referencing the same hazelcast map
     * When a new entry added to key value store
     * - expect isEmpty() to return false for all nodes referencing the same hazelcast map
     */
    @Test
    public void testIsEmpty() {
        assertTrue(underTest1.isEmpty());
        assertTrue(underTest2.isEmpty());

        underTest1.put("a", "a");

        assertFalse(underTest1.isEmpty());
        assertFalse(underTest2.isEmpty());
    }

    /**
     * When clear() is called
     * - expect key value store to be empty for all nodes referencing the same hazelcast map
     */
    @Test
    public void testClear() {
        underTest1.put("test", "abc");
        assertFalse(underTest2.isEmpty());
        assertFalse(underTest1.isEmpty());

        underTest1.clear();
        assertTrue(underTest2.isEmpty());
        assertTrue(underTest1.isEmpty());
    }

    /**
     * When key value store does not contain key
     * - expect it to return false
     * When key value store contains the key
     * - expect it to return true
     * applies to all key value store referencing the same hazelcast map
     */
    @Test
    public void testContainsKey() {
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));

        underTest1.put("test", "abc");
        assertTrue(underTest2.containsKey("test"));
        assertTrue(underTest1.containsKey("test"));

        underTest2.remove("test");
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));
    }

    /**
     * When an entry is added to key value store
     * - expect able to retrieve via all nodes referencing the same hazelcast map
     */
    @Test
    public void testGet() {
        assertEquals(null, underTest1.get("test"));

        underTest1.put("test", "abc");
        assertEquals("abc", underTest2.get("test"));
        assertEquals("abc", underTest1.get("test"));
    }

    /**
     * When put is called, expect new value in key store and return previous value or null
     */
    @Test
    public void testPut() {
        assertNull(underTest1.put("test", "abc"));
        assertEquals("abc", underTest2.put("test", "def"));
        assertEquals("def", underTest1.get("test"));
    }

    /**
     * When put is called, expect new value in key store and return previous value or null
     * if the entry does not exist or has expired
     */
    @Test(timeout = 1000)
    public void testPutWithTtl() {
        /*
        Unfortunately we can't use a fake time source with Hazelcast and using an
        EntryExpiredListener means modifiying the interface and still using a timeout in the test
         */
        int timeToLive = 250;
        assertNull(underTest1.put("test", "abc", timeToLive, TimeUnit.MILLISECONDS));
        assertEquals("abc", underTest2.put("test", "def", timeToLive, TimeUnit.MILLISECONDS));
        assertEntryIsExpired("test");
    }

    /**
     * Test an entry with a TTL of zero doesn't expire
     */
    @Test(timeout = 1000)
    public void testPutWithTtl_ttlIsZero_doesntExpire() {
        assertNull(underTest1.put("test", "abc", 0, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 30; i++) {
            assertTrue(underTest1.containsKey("test"));
        }
    }

    /**
     * Test an entry with a negative TTL results in map default (infinite if not specified initially in config)
     */
    @Test(timeout = 1000)
    public void testPutWithTtl_ttlIsNegative_expiryIsMapDefault() {
        assertNull(underTest2.put("test", "abc", -1, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 30; i++) {
            assertTrue(underTest1.containsKey("test"));
        }
    }

    /**
     * When putIfCondition is called, put should be performed only if the previous map value meets the condition
     */
    @Test
    public void putIfCondition() {
        // Using a TTL of zero (meaning infinite) as we are not interested in the expiry behaviour for this test case
        assertTrue(underTest1.putIfCondition("test", "value", Objects::isNull, 0, TimeUnit.MILLISECONDS));
        assertFalse(underTest1.putIfCondition("test", "updated", Objects::isNull, 0, TimeUnit.MILLISECONDS));
        assertEquals("value", underTest1.get("test"));
        assertTrue(underTest1.putIfCondition("test", "newValue", v -> v.equals("value"), 0, TimeUnit.MILLISECONDS));
        assertEquals("newValue", underTest1.get("test"));
        assertFalse(underTest1.putIfCondition("test", "shouldntUpdate", v -> v.contains("blah"), 0, TimeUnit.MILLISECONDS));
    }

    /**
     * When set is called, expect new value in key store
     */
    @Test
    public void testSet() {
        assertNull(underTest1.get("test"));
        underTest2.set("test", "def");
        assertEquals("def", underTest1.get("test"));
    }

    /**
     * When set is called, expect new value in key store if the entry exists and has not expired
     */
    @Test(timeout = 1000)
    public void testSetWithTtl() {
        /*
        Unfortunately we can't use a fake time source with Hazelcast and using an
        EntryExpiredListener means modifiying the interface and still using a timeout in the test
         */
        int timeToLive = 250;
        String key = "test";
        assertNull(underTest1.get(key));
        underTest1.set(key, "abc", timeToLive, TimeUnit.MILLISECONDS);
        assertEquals("abc", underTest2.get(key));
        underTest2.set(key, "def", timeToLive, TimeUnit.MILLISECONDS);
        assertEquals("def", underTest1.get(key));
        assertEntryIsExpired(key);
    }

    /**
     * When compute is called, expect the new value
     */
    @Test
    public void testCompute() {
        assertEquals("abc", (underTest1.compute("test", (k,v) -> "abc")));
        assertEquals("abcdef", underTest2.compute("test", (k,v) -> v + "def"));
    }

    /**
     * When computeIfAbsent is called,
     * - expect the computed value if key value store does not have the key
     * - expect the old value if key value store has the key
     */
    @Test
    public void testComputeIfAbsent() {
        assertFalse(underTest1.containsKey("test"));
        assertEquals("abc", underTest2.computeIfAbsent("test", (v) -> "abc"));

        underTest2.put("test1", "xyz");
        assertEquals("xyz", underTest1.computeIfAbsent("test1", (v) -> "abc"));
    }

    /**
     * When computeIfPresent is called,
     * - expect null if key value store does not have the key
     * - expect the computed value if key value store has the key
     */
    @Test
    public void testComputeIfPresent() {
        assertFalse(underTest1.containsKey("test"));
        assertEquals(null, underTest2.computeIfPresent("test", (k, v) -> "abc"));
        assertFalse(underTest1.containsKey("test"));

        underTest2.put("test1", "xyz");
        assertTrue(underTest1.containsKey("test1"));
        assertEquals("abc", underTest1.computeIfPresent("test1", (k, v) -> "abc"));
    }

    /**
     * When remove is called, expect the entry for key to be removed and returns
     * - null if key value store did not contain an entry for key
     * - previous value if key value store contained an entry for key
     */
    @Test
    public void testRemove() {
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));

        assertEquals(null, underTest1.put("test", "abc"));
        assertTrue(underTest1.containsKey("test"));
        assertTrue(underTest2.containsKey("test"));

        assertEquals("abc", underTest2.remove("test"));
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));
    }

    /**
     * When remove is called, expect the entry for key to be removed and returns
     */
    @Test
    public void testDelete() {
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));

        underTest1.put("test", "abc");
        assertTrue(underTest1.containsKey("test"));
        assertTrue(underTest2.containsKey("test"));

        underTest2.remove("test");
        assertFalse(underTest1.containsKey("test"));
        assertFalse(underTest2.containsKey("test"));
    }

    private void assertEntryIsExpired(final String key) {
        for(;;) {
            if (underTest1.get(key) == null) {
                return;
            }
        }
    }
}
