package com.l7tech.skunkworks;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

/**
 * Implement mechanism described in WS-Secure Conversation to derive
 * symmetric keys from a shared secret.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 8, 2004<br/>
 * $Id$
 */
public class SecureConversationKeyDeriver {

    /**
     * Function used to generate derived key as per WS-Secure Conversation. This mechanism
     * is inspired by RFC 2246 (TLS).
     *
     * P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
     *                        HMAC_hash(secret, A(2) + seed) +
     *                        HMAC_hash(secret, A(3) + seed) + ...
     * Where + indicates concatenation.
     *
     *  A(x) is defined as:
     *      A(0) = seed
     *      A(i) = HMAC_hash(secret, A(i-1))
     */
    public byte[] pSHA1(byte[] secret, byte[] seed, int requiredlength) throws NoSuchAlgorithmException, InvalidKeyException {
        // compute A(1)
        byte[] ai = hmacsha1(secret, seed);
        // compute A(1) + seed
        byte[] aiPlusSeed = new byte[ai.length + seed.length];
        System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
        System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
        // start the P_SHA-1
        byte[] output = hmacsha1(secret, aiPlusSeed);
        // continue until we get at least the desired length
        while (output.length < requiredlength) {
            ai = hmacsha1(secret, ai);
            if (aiPlusSeed.length == (ai.length + seed.length)) {
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
            } else {
                aiPlusSeed = new byte[ai.length + seed.length];
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
                System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
            }
            byte[] bytesToAppend = hmacsha1(secret, aiPlusSeed);
            byte[] newoutput = new byte[output.length + bytesToAppend.length];
            System.arraycopy(output, 0, newoutput, 0, output.length);
            System.arraycopy(bytesToAppend, 0, newoutput, output.length, bytesToAppend.length);
            output = newoutput;
        }
        // trim to desired size if necessary
        if (output.length > requiredlength) {
            byte[] trimmedoutput = new byte[requiredlength];
            System.arraycopy(output, 0, trimmedoutput, 0, requiredlength);
            output = trimmedoutput;
        }
        return output;
    }

    /**
     * @param secret the key used to compute the sha1 mac
     */
    public byte[] hmacsha1(byte[] secret, byte[] input) throws NoSuchAlgorithmException, InvalidKeyException {
        // todo, property + constants
        Mac mac = Mac.getInstance("HMacSHA1");
        Key key = new SecretKeySpec(secret, "SHA1");
        mac.init(key);
        return mac.doFinal(input);
    }
}
