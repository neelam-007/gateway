/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * Test for OversizedTextAssertion bean.
 */
public class OversizedTextAssertionTest extends TestCase {
    private static Logger log = Logger.getLogger(OversizedTextAssertionTest.class.getName());

    public OversizedTextAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(OversizedTextAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testNestingLimitXpath() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitNestingDepth(true);
        ota.setMaxNestingDepth(3);
        assertEquals("/*/*/*/*", ota.makeNestingXpath());
    }
}
