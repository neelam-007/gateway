/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
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
    final Object mutex = new Object();
    volatile int runnableInvocationsCounter = 0;

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

            synchronized (mutex) {
                runnableInvocationsCounter = 0;
            }

            Runnable testRunnable = new Runnable() {
                public void run() {
                    synchronized (mutex) {
                        runnableInvocationsCounter++;
                    }
                }
            };

            BenchmarkRunner rr = new BenchmarkRunner(testRunnable, count);
            rr.run();
            final int counter;
            synchronized (mutex) {
                counter = runnableInvocationsCounter;
            }
            assertTrue("Expected " + count + " invocations, received  " +
              counter, count == counter);
        }
    }
}
