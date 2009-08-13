package com.l7tech.skunkworks;

import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.BufferPool;
import org.junit.*;
import static org.junit.Assert.assertTrue;

import java.util.Random;

/**
 *
 */
public class BufferPoolBenchmark {
    @Test
    public void testSingleThreaded() throws Exception {
        runTest("testSingleThreaded", 1);
    }

    @Test
    public void testLowConcurrency() throws Exception {
        runTest("testLowConcurrency", 25);
    }

    @Test
    public void testHighConcurrency() throws Exception {
        runTest("testHighConcurrency", 200);
    }

    private void runTest(String name, int numThreads) throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Random rand = new Random(827357);
                for (int i = 0; i < 10000; ++i) {
                    boolean big = rand.nextInt(100) > 50;
                    int randRange = big ? 1200000 : 8000;
                    int size = rand.nextInt(randRange);
                    byte[] buf = BufferPool.getBuffer(size);
                    try {
                        assertTrue(buf.length >= size);
                    } finally {
                        BufferPool.returnBuffer(buf);
                    }
                }
            }
        };

        int times = 1000;
        BenchmarkRunner br = new BenchmarkRunner(runnable, times, numThreads, name);
        br.run();
    }
}
