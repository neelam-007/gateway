package com.l7tech.security.prov;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.*;

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

    /**
     * Get all known aliases for the specified named elliptic curve.  This can be used when, for example, the
     * user asks for "P-256" but the provider only recognizes this under the name "prime256v1".
     *
     * @param curveName  the curve name requested by the user.  Required.
     * @param includeSelf  if true, the returned set will include the requested curve name along with its synonyms.
     * @return a set of all known synonyms for the specified curve name.  May be empty if the curve name is unrecognized or has no known aliases.
     *         Never null.
     */
    public static Set<String> getCurveNameSynonyms(String curveName, boolean includeSelf) {
        String[] syns = synonymsByName.get(curveName.toLowerCase());
        Set<String> ret = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        ret.addAll(Arrays.asList(syns));
        if (!includeSelf)
            ret.remove(curveName);
        return ret;
    }

    private static Collection<String[]> NAMETABLE = Arrays.asList(
            new String[] { "sect163k1", "K-163" },
            new String[] { "sect163r2", "B-163" },
            new String[] { "sect233k1", "K-233" },
            new String[] { "sect233r1", "B-233" },
            new String[] { "sect283k1", "K-283" },
            new String[] { "sect283r1", "B-283" },
            new String[] { "sect409k1", "K-409" },
            new String[] { "sect409r1", "B-409" },
            new String[] { "sect571k1", "K-571" },
            new String[] { "sect571r1", "B-571" },
            new String[] { "secp192r1", "P-192", "prime192v1" },
            new String[] { "secp224r1", "P-224" },
            new String[] { "secp256r1", "P-256", "prime256v1" },
            new String[] { "secp384r1", "P-384" },
            new String[] { "secp521r1", "P-521" }
    );

    private static Map<String, String[]> synonymsByName = new HashMap<String, String[]>() {{
        for (String[] syns : NAMETABLE) {
            for (String syn : syns) {
                put(syn.toLowerCase(), syns);
            }
        }
    }};
}