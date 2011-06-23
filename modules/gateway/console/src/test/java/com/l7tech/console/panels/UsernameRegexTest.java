/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import org.junit.Test;
import static org.junit.Assert.*;


import java.util.regex.Matcher;

import org.junit.Ignore;

/**
 * @author alex
 */
public class UsernameRegexTest {
    private static String[] GOOD = { "good boy", "goodboy", " ok go", "no you go ", "Mr. James O'Punctuation", };
    private static String[] BAD = { "bad#boy", "bad,boy", "no<damned>good", "you+suck", "Tom \"Maverick\" Cruise", "You can't \\escape", "and then;" };

    @Test
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
