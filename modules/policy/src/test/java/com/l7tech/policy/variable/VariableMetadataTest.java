/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class VariableMetadataTest {
    private static final Logger log = Logger.getLogger(VariableMetadataTest.class.getName());

    @Test
    public void testStuff() throws Exception {
        expectFail("$foo");
        expectFail("9foo");
        expectFail(" foo");
        expectFail("foo{}");
        expectFail("foo$bar");
        expectFail("foo bar");
        expectFail("${foo}");
        expectFail("9");
        expectFail(" foo");
        expectFail(".foo");
        expectFail("foo ");
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