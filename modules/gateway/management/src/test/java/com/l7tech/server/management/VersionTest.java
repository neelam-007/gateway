/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/** @author alex */
public class VersionTest extends TestCase {
    private static final Logger log = Logger.getLogger(VersionTest.class.getName());

    public VersionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(VersionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testRoundTrip() throws Exception {
        assertEquals(SoftwareVersion.fromString("5.0").toString(), "5.0");
        assertEquals(SoftwareVersion.fromString("5.1").toString(), "5.1");
        assertEquals(SoftwareVersion.fromString("5.1b1").toString(), "5.1b1");
        assertEquals(SoftwareVersion.fromString("5.2b3-b1").toString(), "5.2b3-b1");
    }

    public void testValidation() throws Exception {
        die("5");
        die("asdf");
        die("5asadf");
        die("5.asdf");
        die("5.1.2.3");
        die("5.1.stuff");
        die("5pre1");
        live("5.1", 5, 1, null, null, null);
        live("5.1b1", 5, 1, null, "b1", null);
        live("5.1b1-b1234", 5, 1, null, "b1", 1234);
        die("5.1b1-b");
        die("5.1b1-b1a");
        die("5.1b1-ba1");
        live("5.1.2", 5, 1, 2, null, null);
        live("5.1.2poop", 5, 1, 2, "poop", null);
        live("5.1.2pre1", 5, 1, 2, "pre1", null);
    }

    private void live(String s, int major, int minor, Integer revision, String suffix, Integer build) {
        SoftwareVersion v = SoftwareVersion.fromString(s);
        assertEquals(v.getMajor(), major);
        assertEquals(v.getMinor(), minor);
        assertEquals(v.getRevision(), revision);
        assertEquals(v.getRevisionSuffix(), suffix);
    }

    private void die(String s) {
        try {
            SoftwareVersion.fromString(s);
            fail("Expected IAE for " + s);
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}