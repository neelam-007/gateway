package com.l7tech.server.util;

import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;
import com.l7tech.util.Cacheable;
import com.l7tech.util.TimeSource;

/**
 *
 */
public class EntityCachingFactoryBeanTest {

    private static TestInterface tester;
    private static final int[] hitCount = new int[1];
    private static final long[] time = new long[]{System.currentTimeMillis()};

    @BeforeClass
    public static void init() throws Exception {
        final String value = "value";
        TestImpl testImpl = new TestImpl(){
            public String doGet(String key) {
                hitCount[0]++;
                return key.equals("a") ? value : null;
            }
        };

        tester = (TestInterface) new EntityCachingFactoryBean(new TimeSource(){
            public long currentTimeMillis() {
                return time[0];
            }
        }, "TestECFB1", TestInterface.class, testImpl).createInstance();
    }

    /**
     * Basic cache test
     */
    @Test
    public void testCached() {
        // encache
        String value = tester.getCached("a");
        Assert.assertEquals("Correct value", "value", value);

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getCached("a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("From cache", countBefore == hitCount[0]);
    }

    /**
     * Basic cache test for 2nd arg
     */
    @Test
    public void testCached2() {
        // encache
        String value = tester.getCached("eee", "a");
        Assert.assertEquals("Correct value", "value", value);

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getCached("eee", "a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("From cache", countBefore == hitCount[0]);
    }

    /**
     * Cache expiry test
     */
    @Test
    public void testCacheExpires() {
        // encache
        String value = tester.getCached("a");
        Assert.assertEquals("Correct value", "value", value);

        // cache almost expires
        time[0] += 999;

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getCached("a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("From cache", countBefore == hitCount[0]);

        // cache expires
        time[0] += 1;

        // ensure from cache
        countBefore = hitCount[0];
        value = tester.getCached("a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("Not From cache", countBefore != hitCount[0]);
    }

    /**
     * Test cache of null result
     */
    @Test
    public void testCachedNull() {
        // encache
        String value = tester.getCached("ab");
        Assert.assertEquals("Correct value", null, value);

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getCached("ab");
        Assert.assertEquals("Correct value", null, value);
        Assert.assertTrue("From cache", countBefore == hitCount[0]);
    }

    /**
     * Test cache skipped when 0 expiry
     */
    @Test
    public void testCacheSkipped() {
        // encache
        String value = tester.getNonCached("a");
        Assert.assertEquals("Correct value", "value", value);

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getNonCached("a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("Not from cache", countBefore != hitCount[0]);
    }

    /**
     * Test cache skipped when not annotated
     */
    @Test
    public void testCacheSkipped2() {
        // encache
        String value = tester.getNotCachable("a");
        Assert.assertEquals("Correct value", "value", value);

        // ensure from cache
        int countBefore = hitCount[0];
        value = tester.getNotCachable("a");
        Assert.assertEquals("Correct value", "value", value);
        Assert.assertTrue("Not from cache", countBefore != hitCount[0]);
    }

    public static interface TestInterface {
        @Cacheable(relevantArg=1, maxAge=1000)
        String getCached(String bogus, String key);

        @Cacheable(relevantArg=0, maxAge=1000)
        String getCached(String key);

        @Cacheable(relevantArg=0, maxAge=0)
        String getNonCached(String key);

        String getNotCachable(String key);
    }

    public static abstract class TestImpl implements TestInterface {
        public abstract String doGet(String key);
        public String getCached(String bogus, String key) { return doGet(key); }
        public String getCached(String key) { return doGet(key); }
        public String getNonCached(String key)  { return doGet(key); }
        public String getNotCachable(String key)  { return doGet(key); }
    }
}
