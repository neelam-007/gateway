/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author mike
 */
public class BufferPoolByteArrayOutputStreamTest extends TestCase {

    public BufferPoolByteArrayOutputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BufferPoolByteArrayOutputStreamTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testOutputStream() throws Exception {
        final String STR = "blah blah blah asdfasdfas ";
        StringBuffer sb = new StringBuffer();
        BufferPoolByteArrayOutputStream bo = new BufferPoolByteArrayOutputStream(4096);
        for (int i = 0; i < 3000; ++i) {
            bo.write(STR.getBytes());
            sb.append(STR);
        }
        assertEquals(bo.toString(), sb.toString());
    }
}
