package com.l7tech.util;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 *
 */
public class CachedCallableTest implements Callable<Boolean> {
    private static final TestTimeSource ts = new TestTimeSource();
    private boolean ret;
    private boolean called;

    @BeforeClass
    public static void beforeClass() {
        CachedCallable.timeSource = ts;
    }

    @Before
    public void before() {
        ts.sync();
        ret = true;
        called = false;
    }

    @Test
    public void testCacheHit() throws Exception {
        Callable<Boolean> c = new CachedCallable<Boolean>(333L, this);

        called = false;
        assertTrue("should obtain true value by invoking wrapped callable", c.call());
        assertTrue("wrapped callable should have been invoked for initial fill of cache", called);
        ret = false;

        called = false;
        assertTrue("should use cached value of true even though ret is now false", c.call());
        assertFalse("should have filled from cache and not invoked wrapped callable", called);

        // Expire cached value
        ts.advanceByMillis(334);

        called = false;
        assertFalse("should refill expired cache and see new false value of ret", c.call());
        assertTrue("should have invoked wrapped callable since cached value has expired", called);
    }

    @Override
    public Boolean call() throws Exception {
        called = true;
        return ret;
    }
}
