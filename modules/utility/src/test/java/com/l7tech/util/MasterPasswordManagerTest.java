package com.l7tech.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test for MasterPasswordManager class.
 */
public class MasterPasswordManagerTest {
    private static final String CIPHERTEXT = "$L7C$XeKVziKOzNnDHXebZNXMkg==$AqL4EnD+Rz9V8J/ez6hilPFM6e9ZDvLwqwvgPMrzPuk=";

    @Test
    public void testObfuscate() throws Exception {
        String cleartext = "secret password";
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext.getBytes(Charsets.UTF8));
        System.out.println(cleartext + " -> " + obfuscated);
    }

    @Test
    public void testUnobfuscate() throws Exception {
        String obfuscated = "$L7O$LTc0MzIyOTY4NjYwODQ1MTk4ODU=$xU3WNeqb/t+tl+BtH4Be";
        byte[] cleartext = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        System.out.println(obfuscated + " -> " + new String(cleartext, Charsets.UTF8));
    }

    @Test
    public void testObfuscationRoundTrip() throws Exception {
        String cleartext = "mumbleahasdfoasdghuigh";
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext.getBytes(Charsets.UTF8));
        assertFalse(cleartext.equalsIgnoreCase(obfuscated));
        byte[] unobfuscated = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertTrue(Arrays.equals(cleartext.getBytes(Charsets.UTF8), unobfuscated));
    }

    @Test
    public void testObfuscationRoundTrip_binaryData() throws Exception {
        byte[] cleartext = new byte[] { -2, 4, 6, 22, -128, 127, 4, 55 };
        String obfuscated = ObfuscatedFileMasterPasswordFinder.obfuscate(cleartext);
        byte[] unobfuscated = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertTrue(Arrays.equals(cleartext, unobfuscated));
    }

    @Test
    public void testBackwardCompatibility() throws Exception {
        // !!! this string must never change, since it's historical data to guarantee backward compatibility with existing client data
        String obfuscated = "$L7O$LTQ5ODIzNTQ4ODUzOTIyNTc1MzM=$AH9iXRGlLs5hbsJ12LPT";
        byte[] cleartext = ObfuscatedFileMasterPasswordFinder.unobfuscate(obfuscated);
        assertEquals("secret password", new String(cleartext, Charsets.UTF8));
    }

    private MasterPasswordManager.MasterPasswordFinder staticFinder(final String masterPassword) {
        return new MasterPasswordManager.MasterPasswordFinder() {
            public byte[] findMasterPasswordBytes() {
                return masterPassword.getBytes(Charsets.UTF8);
            }
        };
    }

    @Test
    public void testEncrypt() throws Exception {
        String cleartext = "big secret password";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));
        assertTrue(mpm.isKeyAvailable());

        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        System.out.println(cleartext + " -> " + ciphertext);
    }
    
    @Test
    public void testDecrypt() throws Exception {
        // !!! this string must never change, since it's historical data to guarantee backward compatibility with existing client data
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("my master password"));
        assertTrue(mpm.isKeyAvailable());

        char[] cleartextChars = mpm.decryptPassword(CIPHERTEXT);
        System.out.println(CIPHERTEXT + " -> " + new String(cleartextChars));
    }

    @Test
    public void testEncryptionRoundTrip() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPassword(ciphertext));
        assertEquals(cleartext, decrypted);
    }

    @Test
    public void testMasterKeyMissing() throws Exception {
        MasterPasswordManager mpm = new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            public byte[] findMasterPasswordBytes() {
                throw new IllegalStateException("No master key available");
            }
        });
        assertFalse(mpm.isKeyAvailable());
        assertEquals("blah", mpm.encryptPassword("blah".toCharArray()));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPassword(CIPHERTEXT)));
        assertEquals(CIPHERTEXT, new String(mpm.decryptPasswordIfEncrypted(CIPHERTEXT)));

        mpm = new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            public byte[] findMasterPasswordBytes() {
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

    @Test
    public void testGenerateTestPasswords() throws Exception {
        String master = "7layer";
        String masterObf = ObfuscatedFileMasterPasswordFinder.obfuscate(master.getBytes(Charsets.UTF8));
        show(master, masterObf);
        System.out.println();

        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder(master));
        show(mpm, "tralala");
        show(mpm, "7layer");
        show(mpm, "password");
    }

    @Test
    public void testFixedKeyBytes() {
        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(Arrays.equals(new byte[] { 2, 3, 4 }, mpm.getMasterPasswordBytes()));
    }

    @Test
    public void testFixedPassword() {
        MasterPasswordManager mpm = new MasterPasswordManager("foo234".getBytes(Charsets.UTF8));
        assertEquals("foo234", new String(mpm.getMasterPasswordBytes(), Charsets.UTF8));
    }

    @Test
    public void testLooksLikeEncryptedPassword() {
        MasterPasswordManager mpm = new MasterPasswordManager(new byte[] { 2, 3, 4});
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdf"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdfg$asdfg5asdfg"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$asdf askjghaeugh a3957 "));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$a sdfaksdjfhalkjrhg arepgiuhaeg"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$$  as4%%$df"));
        assertTrue(mpm.looksLikeEncryptedPassword("$L7C$$$"));

        assertFalse(mpm.looksLikeEncryptedPassword(" $L7C$"));
        assertFalse(mpm.looksLikeEncryptedPassword("$ L7C$"));
        assertFalse(mpm.looksLikeEncryptedPassword("$L7D$"));
        assertFalse(mpm.looksLikeEncryptedPassword("L7D"));
        assertFalse(mpm.looksLikeEncryptedPassword("7layer"));
        assertFalse(mpm.looksLikeEncryptedPassword("password"));
    }

    @Test
    public void testDecryptPasswordIfEncrypted() throws Exception {
        String cleartext = "round trip password 23423432";
        MasterPasswordManager mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String ciphertext = mpm.encryptPassword(cleartext.toCharArray());
        mpm = new MasterPasswordManager(staticFinder("rt master password grewge"));
        String decrypted = new String(mpm.decryptPasswordIfEncrypted(ciphertext));
        assertEquals(cleartext, decrypted);

        String notDecrypted = new String(mpm.decryptPasswordIfEncrypted("x" + ciphertext));
        assertEquals("x" + ciphertext, notDecrypted);
    }
}
