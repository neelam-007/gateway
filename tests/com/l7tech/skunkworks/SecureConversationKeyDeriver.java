package com.l7tech.skunkworks;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

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
     *      A(i) = HMAC_hash(secret, A(i-1)
     */
    public byte[] pSHA1(byte[] secret, byte[] seed, int requiredlength) {
        // todo, use hmacsha1 and implement algo in header
        return null;
    }

    public byte[] hmacsha1(byte[] secret, byte[] input) throws Exception {
        // todo, property + constants
        Mac mac = Mac.getInstance("HMacSHA1");
        Key key = new SecretKeySpec(secret, "SHA1");
        mac.init(key);
        return mac.doFinal(input);
    }
}
