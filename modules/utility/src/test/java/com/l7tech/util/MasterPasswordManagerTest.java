package com.l7tech.util;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;


/**
 * Test for MasterPasswordManager class.
 */
public class MasterPasswordManagerTest extends TestCase {
    private static final String CIPHERTEXT = "$L7C$XeKVziKOzNnDHXebZNXMkg==$AqL4EnD+Rz9V8J/ez6hilPFM6e9ZDvLwqwvgPMrzPuk=";

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

    public void testObfuscationRoundTrip() throws Exception {
        String cleartext = "mumbleahasdfoasdghuigh";
        String obfuscated = DefaultMasterPasswordFinder.obfuscate(cleartext);
        assertFalse(cleartext.equalsIgnoreCase(obfuscated));
        String unobfuscated = DefaultMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals(cleartext, unobfuscated);
    }

    public void testBackwardCompatibility() throws Exception {
        // !!! this string must never change, since it's historical data to guarantee backward compatibility with existing client data
        String obfuscated = "$L7O$LTQ5ODIzNTQ4ODUzOTIyNTc1MzM=$AH9iXRGlLs5hbsJ12LPT";
        String cleartext = DefaultMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals("secret password", cleartext);
    }

    private MasterPasswordManager.MasterPasswordFinder staticFinder(final String masterPassword) {
        return new MasterPasswordManager.MasterPasswordFinder() {
            public char[] findMasterPassword() {
                return masterPassword.toCharArray();
            }
        };
    }

    public void testEncrypt() throws Exception {
        String cleartext = "big secret password";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));
        assertTrue(mpm.isKeyAvailable());

        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        System.out.println(cleartext + " -> " + ciphertext);
    }
    
    public void testDecrypt() throws Exception {
        // !!! this string must never change, since it's historical data to guarantee backward compatibility with existing client data
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));
        assertTrue(mpm.isKeyAvailable());

        char[] cleartextChars = mpm.decryptPassword(CIPHERTEXT);
        System.out.println(CIPHERTEXT + " -> " + new String(cleartextChars));
    }

    public void testEncryptionRoundTrip() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPassword(ciphertext));
        assertEquals(cleartext, decrypted);
    }

    public void testMasterKeyMissing() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            public char[] findMasterPassword() {
                throw new IllegalStateException("No master key available");
            }
        });
        assertFalse(mpm.isKeyAvailable());
        assertEquals("blah", mpm.encryptPassword("blah".toCharArray()));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPassword(CIPHERTEXT)));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPasswordIfEncrypted(CIPHERTEXT)));

        mpm = new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            public char[] findMasterPassword() {
                return null;
            }
        });
        assertFalse(mpm.isKeyAvailable());
        assertEquals("blah", mpm.encryptPassword("blah".toCharArray()));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPassword(CIPHERTEXT)));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPasswordIfEncrypted(CIPHERTEXT)));
    }
    
    private void show(String password, String obf) {
        System.out.println(password + "\t OBF-->\t " + obf);
    }

    private void show(MasterPasswordManager mpm, String plaintextPassword) {
        String enc = mpm.encryptPassword(plaintextPassword.toCharArray());
        System.out.println(plaintextPassword + "\t ENC-->\t " + enc);
    }

    public void testGenerateTestPasswords() throws Exception {
        String master = "7layer";
        String masterObf = DefaultMasterPasswordFinder.obfuscate(master);
        show(master, masterObf);
        System.out.println();

        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder(master));
        show(mpm, "tralala");
        show(mpm, "7layer");
        show(mpm, "password");
    }
}
