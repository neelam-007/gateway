/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class BufferPoolTest {
    private static Logger log = Logger.getLogger(BufferPoolTest.class.getName());

    private int[] reqPerSizeClass = new int[BufferPool.getNumSizeClasses()];

    @Before
    public void clearHistogram() {
        Arrays.fill(reqPerSizeClass, 0);
    }

    @Test
    public void testBufferPool() throws Exception {
        Random rand = new Random(827357);
        for (int i = 0; i < 5000; ++i) {
            boolean big = rand.nextInt(100) > 50;
            int randRange = big ? 1200000 : 8000;
            int size = rand.nextInt(randRange);
            reqPerSizeClass[BufferPool.getSizeClass(size)]++;
            byte[] buf = BufferPool.getBuffer(size);
            assertTrue(buf.length >= size);
            BufferPool.returnBuffer(buf);
        }

        // Show histogram to ensure adequate coverage
        System.out.println("Sz\tCnt");
        for (int sc = 0; sc < reqPerSizeClass.length; sc++) {
            int count = reqPerSizeClass[sc];
            System.out.println(sc + "\t" + count);
        }

        // Get a teeny buffer
        byte[] tiny = BufferPool.getBuffer(1);
        assertTrue(tiny.length == 1024);

        // Get some big buffers
        byte[] b3mb = BufferPool.getBuffer(3 * 1024 * 1024);
        assertTrue(b3mb.length >= 3 * 1024 * 1024);
        byte[] b2mb = BufferPool.getBuffer(2 * 1024 * 1024);
        assertTrue(b2mb.length >= 2 * 1024 * 1024);
        BufferPool.returnBuffer(b3mb);

        byte[] a = BufferPool.getBuffer(2 * 1024 * 1024);
        // Should have reused the one we just put back since it's bigger
        assertTrue(a == b3mb);

        BufferPool.returnBuffer(b2mb);
        byte[] c = BufferPool.getBuffer(2 * 1024 * 1024);
        assertTrue(c == b2mb);

        final int size = 2 * 1024 * 1024 + 17;
        byte[] d = BufferPool.getBuffer(size);
        assertTrue(d.length == size);
    }
}
