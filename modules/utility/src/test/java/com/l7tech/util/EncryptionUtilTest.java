package com.l7tech.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author $Author$
 * @version $Revision$
 */
public class EncryptionUtilTest extends TestCase {

    public EncryptionUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EncryptionUtilTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testStringEncrytpion() {
        String clear = "Clear text to encrypt.";
        String encrypted = EncryptionUtil.obfuscate(clear, clear);
        String decrypted = EncryptionUtil.deobfuscate(encrypted, clear);
        assertEquals("Encrypt/Decrypt round trip", clear, decrypted);
    }

    public void testKeyedStringEncryption() {
        byte[] key = new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        String clear = "Clear text to encrypt.";
        String encrypted = EncryptionUtil.encrypt(clear, key);
        String decrypted = EncryptionUtil.decrypt(encrypted, key);
        assertEquals("Encrypt/Decrypt round trip", clear, decrypted);
    }

    public void testStringDecryptionFails() {
        String clear = "Clear text to encrypt.";
        String encrypted = EncryptionUtil.obfuscate(clear, clear);

        try {
            String decrypted = EncryptionUtil.deobfuscate(encrypted, "incorrect value");
            fail("Encrypt/Decrypt round tripped");
        }
        catch(IllegalArgumentException iae) {
            // an error is expected.
        }
    }
}
