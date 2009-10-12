package com.l7tech.security.prov;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

/**
 * Utilities relating to JCE that may need to be used before the JceProvider is initialized and locked in.
 */
public class JceUtil {

    /**
     * Test whether strong cryptography is enabled in the JDK.
     *
     * @return true if we were able to initialize an AES 256 cipher.
     *         false if this was disallowed by crypto policy.
     * @throws GeneralSecurityException if an unexpected error occurred while testing for strong crypto.
     */
    public static boolean isStrongCryptoEnabledInJvm() throws GeneralSecurityException {
        try {
            SecretKeySpec key = new SecretKeySpec(new byte[32], 0, 32, "AES");
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
            return true;
        } catch ( InvalidKeyException ike ) {
            return false;
        }
    }

    /**
     * Throws an exception if strong crypto is not enabled in the current JVM.
     *
     * @throws StrongCryptoNotAvailableException if strong crypto is not enabled.
     * @throws GeneralSecurityException if an unexpected error occurred while testing for strong crypto.
     */
    public static void requireStrongCryptoEnabledInJvm() throws StrongCryptoNotAvailableException, GeneralSecurityException {
        if (!isStrongCryptoEnabledInJvm())
            throw new StrongCryptoNotAvailableException("The current Java virtual machine does not have strong cryptography enabled.  The unlimited strength juristiction JCE policy files may need to be installed.");
    }
}
