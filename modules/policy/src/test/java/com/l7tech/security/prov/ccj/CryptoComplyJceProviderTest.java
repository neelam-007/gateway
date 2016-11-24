package com.l7tech.security.prov.ccj;

import com.safelogic.cryptocomply.jce.provider.SLProvider;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * Test cases are designed to test CCJ crypto provider in some special areas such as encryption and decryption using RSA/ECB/NoPadding.
 */
public class CryptoComplyJceProviderTest {

    @Test
    public void testPaddingDecryptedTextUsingRsaEcbNoPaddingWithDifferentProviders() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final int keySizeInBits = 512; // bits
        final int keySizeInBytes = keySizeInBits / 8; // bytes
        final String transformation = "RSA/ECB/NoPadding";
        final String dummyPlainText = "12345678901234567890123456789012345678901234567890";
        final byte[] inputData = dummyPlainText.getBytes(StandardCharsets.UTF_8);
        final byte[][] outputs = new byte[2][];

        for (int i = 0; i < outputs.length; i++) {
            // Set different crypto provider in each iteration.
            // First iteration: CCJ
            // Second iteration: other provider SunJCE
            if (i == 0) {
                Provider provider = new SLProvider();
                Security.removeProvider("WF");
                Security.insertProviderAt(provider, 1);
            } else {
                Security.removeProvider("WF");
            }

            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keySizeInBits);
            KeyPair pair = kpg.generateKeyPair();

            // Encryption
            final Cipher cipher1 = Cipher.getInstance(transformation);
            cipher1.init(Cipher.ENCRYPT_MODE, pair.getPublic());
            final byte[] outputEnc = cipher1.doFinal(inputData);

            // Decryption
            final Cipher cipher2 = Cipher.getInstance(transformation);
            cipher2.init(Cipher.DECRYPT_MODE, pair.getPrivate());
            byte[] outputDec = cipher2.doFinal(outputEnc);

            // If it is CCJ, then add padding
            if (i == 0) {
                outputDec = CryptoComplyJceProviderEngine.paddingDecryptionOutputUsingRsaEcbNoPadding(outputDec, keySizeInBytes);
            }

            // Check if the decryped output length is the same as key size in bytes.
            Assert.assertEquals(keySizeInBytes, outputDec.length);

            outputs[i] = outputDec;
        }

        // Check if two decrypted outputs are same for two different providers
        Assert.assertTrue(java.util.Arrays.equals(outputs[0], outputs[1]));
    }
}
