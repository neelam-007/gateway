/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test the RegEx assertion
 */
public class RegexAssertionTest extends TestCase {
    public RegexAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(RegexAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testBackReference() throws Exception {
    }
}
