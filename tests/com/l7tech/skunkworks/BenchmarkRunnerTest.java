/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class BenchmarkRunnerTest extends TestCase {
    int runnableInvocationsCounter = 0;

    public BenchmarkRunnerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BenchmarkRunnerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleRunnable() throws Exception {
        int[] counts = {1000, 10000, 100000, 1000000};
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];

            runnableInvocationsCounter = 0;
            Runnable testRunnable = new Runnable() {
                public void run() {
                    runnableInvocationsCounter++;
                }
            };
            BenchmarkRunner rr = new BenchmarkRunner(testRunnable, count);
            rr.run();
            assertTrue("Expected " + count + " invocations, received  " +
              runnableInvocationsCounter, count == runnableInvocationsCounter);
        }
    }
}
