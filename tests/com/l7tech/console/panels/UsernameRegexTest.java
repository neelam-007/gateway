/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.regex.Matcher;

/**
 * @author alex
 */
public class UsernameRegexTest extends TestCase {
    public UsernameRegexTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(UsernameRegexTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static String[] GOOD = { "good boy", "goodboy", " ok go", "no you go " };
    private static String[] BAD = { "bad#boy", "bad,boy", "no<damned>good" };

    public void testFoo() throws Exception {
        for (String s : GOOD) {
            Matcher mat = NewInternalUserDialog.CERT_NAME_CHECKER.matcher(s);
            assertFalse(s + " is supposed to be good", mat.find());
        }

        for (String s : BAD) {
            assertTrue(s + " is supposed to be bad", NewInternalUserDialog.CERT_NAME_CHECKER.matcher(s).find());
        }
    }

}
