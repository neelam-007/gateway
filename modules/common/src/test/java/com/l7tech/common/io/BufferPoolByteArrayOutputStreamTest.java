/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * @author mike
 */
public class BufferPoolByteArrayOutputStreamTest extends TestCase {
    private static Logger log = Logger.getLogger(BufferPoolByteArrayOutputStreamTest.class.getName());

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
