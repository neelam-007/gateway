package com.l7tech.util;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * @author $Author$
 * @version $Revision$
 */
public class EncryptionUtilTest {

    @Test
    public void testStringEncrytpion() {
        String clear = "Clear text to encrypt.";
        String encrypted = EncryptionUtil.obfuscate(clear, clear);
        String decrypted = EncryptionUtil.deobfuscate(encrypted, clear);
        assertEquals("Encrypt/Decrypt round trip", clear, decrypted);
    }

    @Test
    public void testKeyedStringEncryption() {
        byte[] key = new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        String clear = "Clear text to encrypt.";
        String encrypted = EncryptionUtil.encrypt(clear, key);
        String decrypted = EncryptionUtil.decrypt(encrypted, key);
        assertEquals("Encrypt/Decrypt round trip", clear, decrypted);
    }

    @Test
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
