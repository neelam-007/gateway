/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author mike
 */
public class BufferPoolTest {
    private static Logger log = Logger.getLogger(BufferPoolTest.class.getName());

    private int[] reqPerSizeClass = new int[BufferPool.getNumSizeClasses()];

    @Before
    public void clearHistogram() {
        Arrays.fill(reqPerSizeClass, 0);
        BufferPool.clearAllPools();
    }

    @Test
    public void testBufferPool() throws Exception {
        boolean pooled;

        Random rand = new Random(827357);
        for (int i = 0; i < 5000; ++i) {
            boolean big = rand.nextInt(100) > 50;
            int randRange = big ? 1200000 : 8000;
            int size = rand.nextInt(randRange);
            final int sizeClass = BufferPool.getSizeClass(size);
            reqPerSizeClass[sizeClass]++;
            byte[] buf = BufferPool.getBuffer(size);
            assertTrue(buf.length >= size);
            assertTrue(buf.length <= BufferPool.getSizeOfSizeClass(sizeClass + 1));
            pooled = BufferPool.returnBuffer(buf);
            assertTrue(pooled);
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
        pooled = BufferPool.returnBuffer(b3mb);
        assertTrue(pooled);

        byte[] a = BufferPool.getBuffer(2 * 1024 * 1024);
        // Should have reused the one we just put back since it's bigger
        assertTrue(a == b3mb);

        pooled = BufferPool.returnBuffer(b2mb);
        assertTrue(pooled);
        byte[] c = BufferPool.getBuffer(2 * 1024 * 1024);
        assertTrue(c == b2mb);

        final int size = 2 * 1024 * 1024 + 17;
        byte[] d = BufferPool.getBuffer(size);
        assertTrue(d.length == size);
    }

    @Test
    public void testHugePoolIntOverflow() {
        BufferPool.HugePool pool = new BufferPool.HugePool();
        byte[] buff = pool.getOrCreateBuffer(2 * 1024 * 1024);
        assertNotNull(buff);
        assertEquals(2 * 1024 * 1024, buff.length);

        assertTrue(pool.returnBuffer(buff));

        buff = pool.getBuffer(Integer.MAX_VALUE - 4096);
        assertNull("HugePool should not reuse a buffer more than double the requested size, even if requested size is > 2^31/2", buff);
    }

    @Test
    public void testHugePoolReuseHugeBuffer() {
        boolean pooled = BufferPool.returnBuffer(new byte[3 * 1024 * 1024]);
        assertTrue(pooled);

        byte[] buff = BufferPool.getBuffer(1024 * 1024);
        assertEquals("should not have reused existing buffer (too much bigger than we needed)", 1024 * 1024, buff.length);

        buff = BufferPool.getBuffer(2 * 1024 * 1024);
        assertEquals("should have reused existing buffer (overlarge, but less than 2x the desired size)", 3 * 1024 * 1024, buff.length);
    }
}
