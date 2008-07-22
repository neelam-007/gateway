/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class VariableMetadataTest extends TestCase {
    private static final Logger log = Logger.getLogger(VariableMetadataTest.class.getName());

    public VariableMetadataTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(VariableMetadataTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testStuff() throws Exception {
        expectFail("$foo");
        expectFail("9foo");
        expectFail(" foo");
        expectFail("foo{}");
        expectFail("foo$bar");
        expectFail("foo bar");
        expectFail("${foo}");
        new VariableMetadata("foo");
        new VariableMetadata("_foo");
        new VariableMetadata("foo_bar");
        new VariableMetadata("foo_");
    }

    private void expectFail(String s) {
        try {
            new VariableMetadata(s);
            fail("Expected IAE for " + s);
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}