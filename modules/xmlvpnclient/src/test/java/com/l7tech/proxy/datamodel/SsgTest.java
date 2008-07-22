/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class SsgTest extends TestCase {
    private static Logger log = Logger.getLogger(SsgTest.class.getName());

    public SsgTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSsg() throws Exception {
        Ssg ssg = new Ssg();
        assertTrue(ssg.getLocalEndpoint().length() > 0);
    }

    public void testCopyFrom() throws Exception {
        Ssg ssgA = new Ssg(1);
        Ssg ssgB = new Ssg(2);
        ssgA.setSsgAddress("blah.blah.blah");
        ssgB.setSsgAddress("foo.foo.foo");
        ssgB.setSavePasswordToDisk(true);
        ssgB.setOverrideIpAddresses(new String[] {"1.2.3.4", "4.5.6.7"});

        ssgA.copyFrom(ssgB);

        assertEquals(ssgA.getSsgAddress(), "foo.foo.foo");
        assertEquals(1, ssgA.getId());
        assertEquals(2, ssgB.getId());
        assertEquals("1.2.3.4", ssgA.getOverrideIpAddresses()[0]);

    }
}
