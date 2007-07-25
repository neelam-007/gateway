package com.l7tech.common.security;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;

/**
 * Test for MasterPasswordManager class.
 */
public class MasterPasswordManagerTest extends TestCase {
    private static final Logger log = Logger.getLogger(MasterPasswordManagerTest.class.getName());

    public MasterPasswordManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MasterPasswordManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testObfuscate() throws Exception {
        String cleartext = "secret password";
        String obfuscated = DefaultMasterPasswordFinder.obfuscate(cleartext);
        System.out.println(cleartext + " -> " + obfuscated);
    }

    public void testUnobfuscate() throws Exception {
        String obfuscated = "$L7O$LTc0MzIyOTY4NjYwODQ1MTk4ODU=$xU3WNeqb/t+tl+BtH4Be";
        String cleartext = DefaultMasterPasswordFinder.unobfuscate(obfuscated);
        System.out.println(obfuscated + " -> " + cleartext);
    }

    public void testRoundTrip() throws Exception {
        String cleartext = "mumbleahasdfoasdghuigh";
        String obfuscated = DefaultMasterPasswordFinder.obfuscate(cleartext);
        assertFalse(cleartext.equalsIgnoreCase(obfuscated));
        String unobfuscated = DefaultMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals(cleartext, unobfuscated);
    }

    public void testBackwardCompatibility() throws Exception {
        String obfuscated = "$L7O$LTQ5ODIzNTQ4ODUzOTIyNTc1MzM=$AH9iXRGlLs5hbsJ12LPT"; // this string must never change, since it's historical data
        String cleartext = DefaultMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals("secret password", cleartext);
    }
}
