/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Test case for {@link FailoverStrategy} implementations.
 */
public class FailoverStrategyTest extends TestCase {
    private static Logger log = Logger.getLogger(FailoverStrategyTest.class.getName());

    public FailoverStrategyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FailoverStrategyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static final String SA = "a";
    public static final String SB = "b";
    public static final String SC = "c";
    private final Object[] servers = { SA, SB, SC };

    public void testStickyFailoverStrategy() throws Exception {
        FailoverStrategy s = AbstractFailoverStrategy.makeSynchronized(new StickyFailoverStrategy(servers));

        // Must be sticky
        final Object a1 = s.selectService();
        final Object a2 = s.selectService();
        assertEquals(a1, a2);

        // Must failover once...
        s.reportFailure(a1);
        final Object b1 = s.selectService();
        assertNotSame(a1, b1);

        // Must failover twice...
        s.reportFailure(b1);
        final Object c1 = s.selectService();
        assertNotSame(b1, c1);

        // After third failover, must recommend least-recently-failed service
        s.reportFailure(c1);
        final Object d1 = s.selectService();
        assertEquals(d1, a1);

        // Report one as "back up"
        s.reportSuccess(SB);
        final Object e1 = s.selectService();
        assertEquals(SB, e1);
    }

    public void testRoundRobinFailoverStrategy() throws Exception {
        FailoverStrategy s = new RoundRobinFailoverStrategy(servers);

        // Strict round-robin must ignore success or failure
        assertEquals(SA, s.selectService());
        s.reportFailure(SB);
        assertEquals(SB, s.selectService());
        s.reportSuccess(SA);
        assertEquals(SC, s.selectService());
        s.reportFailure(SA);
        assertEquals(SA, s.selectService());
    }

    public void testRandomFailoverStrategy() throws Exception {
        FailoverStrategy s = new RandomFailoverStrategy(servers);

        // Strict random must report random order, ignoring success or failure
        Set set = new HashSet();
        for (int i = 0; i < servers.length * 500; ++i) {
            Object next = s.selectService();
            assertNotNull(next);
            set.add(next);
            s.reportFailure(SA);
            s.reportSuccess(SC);
        }

        // Make sure we got them all
        assertEquals(set.size(), servers.length);
    }
}
