/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Test case for {@link FailoverStrategy} implementations.
 */
public class FailoverStrategyTest {
    static {
        System.setProperty("com.l7tech.common.io.failover.robin.retryMillis", "200");
    }

    public static final String SA = "a";
    public static final String SB = "b";
    public static final String SC = "c";
    private final String[] servers = { SA, SB, SC };
    private final String[] twoservers = { SA, SB };

    @Test
    public void testStickyFailoverStrategy() throws Exception {
        FailoverStrategy<String> s = AbstractFailoverStrategy.makeSynchronized(new StickyFailoverStrategy<String>(servers));

        // Must be sticky
        final String a1 = s.selectService();
        final String a2 = s.selectService();
        assertEquals(a1, a2);

        // Must failover once...
        s.reportFailure(a1);
        final String b1 = s.selectService();
        assertNotSame(a1, b1);

        // Must failover twice...
        s.reportFailure(b1);
        final String c1 = s.selectService();
        assertNotSame(b1, c1);

        // After third failover, must recommend least-recently-failed service
        s.reportFailure(c1);
        final String d1 = s.selectService();
        assertEquals(d1, a1);

        // Report one as "back up"
        s.reportSuccess(SB);
        final String e1 = s.selectService();
        assertEquals(SB, e1);
    }

    @Test
    public void testRoundRobinFailoverStrategy() throws Exception {
        FailoverStrategy<String> s = new RoundRobinFailoverStrategy<String>(servers);

        // Strict round-robin must do blacklisting
        assertEquals(SA, s.selectService());
        s.reportSuccess(SA);
        assertEquals(SB, s.selectService());
        s.reportFailure(SB);
        assertEquals(SC, s.selectService());
        s.reportSuccess(SC);
        assertEquals(SA, s.selectService());
        s.reportFailure(SA);
        assertEquals(SC, s.selectService());
        s.reportSuccess(SC);
        Thread.sleep(300);
        assertEquals(SB, s.selectService());
        s.reportSuccess(SB);
    }

    @Test
    public void testRandomFailoverStrategy() throws Exception {
        FailoverStrategy<String> s = new RandomFailoverStrategy<String>(servers);

        // Strict random must report random order, ignoring success or failure
        Set<String> set = new HashSet<String>();
        for (int i = 0; i < servers.length * 500; ++i) {
            String next = s.selectService();
            assertNotNull(next);
            set.add(next);
            s.reportFailure(SA);
            s.reportSuccess(SC);
        }

        // Make sure we got them all
        assertEquals(set.size(), servers.length);
    }

    @Test
    public void testOrderedStickyBug3930() throws Exception {
        String sa = "SA";
        String sb = "SB";
        String got;

        OrderedStickyFailoverStrategy<String> s = new OrderedStickyFailoverStrategy<String>(new String[] { sa, sb });
        //s.setProbeTime(100);

        // 1. With both SSG2 & SSG3 up, issue a request to SSG1 -- this should work
        got = s.selectService();
        assertEquals(got, sa);
        s.reportSuccess(got);

        // 2. Bring both SSG2 & SSG3 down and issue a request again -- this should fail
        got = s.selectService();
        assertEquals(got, sa);
        s.reportFailure(got);

        got = s.selectService();
        assertEquals(got, sb);
        s.reportFailure(got);

        // 3. Of SSG2 or SSG3, startup the one that is second in the list of IPs SSG1's BRA Properties

        // 4. Issue a request to SSG1 -- this should not fail (bug #3930 was that it was failing)
        got = s.selectService();
        assertEquals(got, sa);
        s.reportFailure(got);

        got = s.selectService();
        assertEquals(got, sb);
        s.reportSuccess(got);
    }

    @Test
    public void testOrderedStickyFailoverStrategy() throws Exception {
        OrderedStickyFailoverStrategy<String> s = new OrderedStickyFailoverStrategy<String>(servers);
        s.setProbeTime(100);

        assertEquals("Must initially prefer first server", SA, s.selectService()); s.reportSuccess(SA);
        assertEquals("Must be sticky", SA, s.selectService());

        s.reportFailure(SA);
        assertEquals("Must use preference order for failover", SB, s.selectService()); s.reportSuccess(SB);

        for (int i = 0; i < 20; ++i) {
            assertEquals("Must be sticky", SB, s.selectService()); s.reportSuccess(SB);
        }

        Thread.sleep(110);

        // Should start seeing probes of SA now
        int saCount = 0;
        for (int i = 0; i < 20; ++i) {
            String got = s.selectService();
            //noinspection StringEquality
            if (got == SA) {
                saCount++;
                s.reportFailure(got);
            } else {
                assertEquals("Must be sticky", SB, got);
                s.reportSuccess(got);
            }
        }

        assertTrue("Should have probed for higher-pref server", saCount > 0);
        assertTrue("Should not have probe for higher-pref server more than once", saCount < 2);

        // Simulate A coming back up

        // Time for another probe
        Thread.sleep(110);
        boolean sawSa = false;
        for (int i = 0; i < 20; ++i) {
            String got = s.selectService();
            s.reportSuccess(got);
            //noinspection StringEquality
            if (got == SA) {
                sawSa = true;
            } else {
                assertFalse("Should have upgraded to higher-pref server", sawSa);
            }
        }

        assertEquals("Should be sticky after recover", SA, s.selectService()); s.reportSuccess(SA);

        // TODO test two servers going down
    }

    @Test
    public void testBug4232_OrderedStickyAllServersDownProbed() throws Exception {
        // Simulate 3 server failure (Bug #4232)
        OrderedStickyFailoverStrategy<String> s = new OrderedStickyFailoverStrategy<String>(new String[] { SA, SB, SC });
        s.setProbeTime(100);
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        Thread.sleep(110);
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        Thread.sleep(110);
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());
        s.reportFailure(s.selectService());

    }

    @Test
    public void testBug1718_OrderedStickyAllServersDown() throws Exception {
        OrderedStickyFailoverStrategy<String> s = new OrderedStickyFailoverStrategy<String>(twoservers);

        String got = s.selectService();
        assertEquals(got, SA);
        s.reportFailure(got);

        got = s.selectService();
        assertEquals(got, SB);
        s.reportFailure(got);

        got = s.selectService();
        //noinspection StringEquality
        assertTrue(got == SA || got == SB);
    }
}
