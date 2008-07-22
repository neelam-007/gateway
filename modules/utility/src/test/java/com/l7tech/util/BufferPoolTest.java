/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Random;

import com.l7tech.util.BufferPool;

/**
 * @author mike
 */
public class BufferPoolTest extends TestCase {
    private static Logger log = Logger.getLogger(BufferPoolTest.class.getName());

    public BufferPoolTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BufferPoolTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testBufferPool() throws Exception {
        Random rand = new Random(827357);
        for (int i = 0; i < 3000; ++i) {
            int size = rand.nextInt(500000);
            byte[] buf = BufferPool.getBuffer(size);
            assertTrue(buf.length >= size);
            BufferPool.returnBuffer(buf);
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
