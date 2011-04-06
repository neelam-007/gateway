/*
   Sha512Crypt.java

   Created: 18 December 2007
   Last Changed By: $Author: broccol $
   Version: $Revision: 7692 $
   Last Mod Date: $Date: 2007-12-30 01:55:31 -0600 (Sun, 30 Dec 2007) $

   Java Port By: James Ratcliff, falazar@arlut.utexas.edu

   This class implements the new generation, scalable, SHA512-based
   Unix 'crypt' algorithm developed by a group of engineers from Red
   Hat, Sun, IBM, and HP for common use in the Unix and Linux
   /etc/shadow files.

   The Linux glibc library (starting at version 2.7) includes support
   for validating passwords hashed using this algorithm.

   The algorithm itself was released into the Public Domain by Ulrich
   Drepper <drepper@redhat.com>.  A discussion of the rationale and
   development of this algorithm is at

   http://people.redhat.com/drepper/sha-crypt.html

   and the specification and a sample C language implementation is at

   http://people.redhat.com/drepper/SHA-crypt.txt

   This Java Port is  

     Copyright (c) 2008 The University of Texas at Austin.

     All rights reserved.

     Redistribution and use in source and binary form are permitted
     provided that distributions retain this entire copyright notice
     and comment. Neither the name of the University nor the names of
     its contributors may be used to endorse or promote products
     derived from this software without specific prior written
     permission. THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY
     EXPRESS OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE
     IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
     PARTICULAR PURPOSE.

*/

package com.l7tech.gateway.common.security.password;

import com.l7tech.util.Charsets;

import java.security.MessageDigest;
import java.security.SecureRandom;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     Sha512Crypt

------------------------------------------------------------------------------*/

/**
 * This class defines a method, {@link
 * Sha512Crypt#crypt(MessageDigest, MessageDigest, byte[], java.lang.String)
 * Sha512_crypt()}, which takes a password, a salt string and a pair of MessageDigest.SHA-512 instances, and
 * generates a Sha512 encrypted password entry.
 * <p/>
 * This class implements the new generation, scalable, SHA512-based
 * Unix 'crypt' algorithm developed by a group of engineers from Red
 * Hat, Sun, IBM, and HP for common use in the Unix and Linux
 * /etc/shadow files.
 * <p/>
 * The Linux glibc library (starting at version 2.7) includes support
 * for validating passwords hashed using this algorithm.
 * <p/>
 * The algorithm itself was released into the Public Domain by Ulrich
 * Drepper &lt;drepper@redhat.com&gt;.  A discussion of the rationale and
 * development of this algorithm is at
 * <p/>
 * http://people.redhat.com/drepper/sha-crypt.html
 * <p/>
 * and the specification and a sample C language implementation is at
 * <p/>
 * http://people.redhat.com/drepper/SHA-crypt.txt
 * <p/>
 * Original Java Port By: James Ratcliff, falazar@arlut.utexas.edu
 * Modified by Layer 7.  Any bugs introduced by Layer 7 modifications are not his fault.
 */
public final class Sha512Crypt {
    static final String sha512_salt_prefix = "$6$";
    static private final String sha512_rounds_prefix = "rounds=";
    static private final int SALT_LEN_MAX = 16;
    static private final int ROUNDS_DEFAULT = 5000;
    static private final int ROUNDS_MIN = 1000;
    static private final int ROUNDS_MAX = 999999999;
    static private final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    static private final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Util class, prevent instantiation
    private Sha512Crypt() {
    }

    /**
     * Generate a salt string, optionally including a specified number of work rounds.
     *
     * @param secureRandom a SecureRandom to use for generating the salt.  Required.
     * @param rounds a specific number of rounds to use, between {@link #ROUNDS_MIN} and {@link #ROUNDS_MAX}, or 0 to use {@link #ROUNDS_DEFAULT}.
     * @return a salt string, ie "8UoSVapNtMuq1ukK" (if rounds was 0) or "rounds=7777$8UoSVapNtMuq1ukK" (if rounds was specified).
     */
    public static String generateSalt(SecureRandom secureRandom, int rounds) {
        StringBuffer saltBuf = new StringBuffer();

        while (saltBuf.length() < 16) {
            int index = secureRandom.nextInt(SALTCHARS.length());
            saltBuf.append(SALTCHARS.substring(index, index+1));
        }

        if (rounds >= ROUNDS_MIN && rounds <= ROUNDS_MAX) {
            return sha512_rounds_prefix + rounds + "$" + saltBuf.toString();
        } else
            return saltBuf.toString();
    }

    /**
     * This method actually generates an
     * Sha512 crypted password hash from a plaintext password and a
     * salt.
     * <p/>
     * <p>The resulting string will be in the form '$6$&lt;rounds=n&gt;$&lt;salt&gt;$&lt;hashed mess&gt;</p>
     *
     * @param ctx       first of a pair of MessageDigest.SHA-512 instances for the exclusive use of this thread for the duration of this method.  Required.
     * @param alt_ctx  second of a pair of MessageDigest.SHA-512 instances for the exclusive use of this thread for the duration of this method.  Required.
     * @param key      Plaintext password, already converted to bytes in some correct way (like String.getBytes(Charsets.UTF8)).  Required.
     * @param saltStr     An encoded salt/rounds which will be consulted to determine the salt and round count.  Required.
     *                    See {@link #generateSalt(java.security.SecureRandom, int)} for an easy way to generate a correctly-formatted salt string.
     * @return The Sha512 Unix Crypt hash text for the keyStr.  Never null.
     */

    public static String crypt(MessageDigest ctx, MessageDigest alt_ctx, byte[] key, String saltStr) {
        if (ctx == null)
            throw new NullPointerException("Two SHA-512 contexts must be provided (missing ctx)");
        if (alt_ctx == null)
            throw new NullPointerException("Two SHA-512 contexts must be provided (missing alt_ctx)");
        if (key == null)
            throw new NullPointerException("Password bytes must be provided.");
        if (saltStr == null)
            throw new NullPointerException("A salt string is required.");
        if (ctx == alt_ctx)
            throw new IllegalArgumentException("ctx and alt_ctx must be different SHA-512 contexts.");
        if (!"SHA-512".equals(ctx.getAlgorithm()))
            throw new IllegalArgumentException("ctx is not MessageDigest.SHA-512");
        if (!"SHA-512".equals(alt_ctx.getAlgorithm()))
            throw new IllegalArgumentException("alt_ctx is not MessageDigest.SHA-512");

        byte[] alt_result;
        byte[] temp_result;
        byte[] p_bytes;
        byte[] s_bytes;
        int cnt, cnt2;
        int rounds = ROUNDS_DEFAULT; // Default number of rounds.
        boolean rounds_custom = false;
        StringBuffer buffer;

        /* -- */

        if (saltStr.startsWith(sha512_salt_prefix)) {
            saltStr = saltStr.substring(sha512_salt_prefix.length());
        }

        if (saltStr.startsWith(sha512_rounds_prefix)) {
            String num = saltStr.substring(sha512_rounds_prefix.length(), saltStr.indexOf('$'));
            int srounds = Integer.valueOf(num);
            saltStr = saltStr.substring(saltStr.indexOf('$') + 1);
            rounds = Math.max(ROUNDS_MIN, Math.min(srounds, ROUNDS_MAX));
            rounds_custom = true;
        }

        int dollarPos = saltStr.indexOf('$');
        if (dollarPos >= 0) {
            // Salt string is actually a complete password verifier
            saltStr = saltStr.substring(0, dollarPos);
        }

        if (saltStr.length() > SALT_LEN_MAX) {
            saltStr = saltStr.substring(0, SALT_LEN_MAX);
        }

        byte[] salt = saltStr.getBytes(Charsets.UTF8);

        ctx.reset();
        ctx.update(key);
        ctx.update(salt);

        alt_ctx.reset();
        alt_ctx.update(key);
        alt_ctx.update(salt);
        alt_ctx.update(key);

        alt_result = alt_ctx.digest();

        for (cnt = key.length; cnt > 64; cnt -= 64) {
            ctx.update(alt_result, 0, 64);
        }

        ctx.update(alt_result, 0, cnt);

        for (cnt = key.length; cnt > 0; cnt >>= 1) {
            if ((cnt & 1) != 0) {
                ctx.update(alt_result, 0, 64);
            } else {
                ctx.update(key, 0, key.length);
            }
        }

        alt_result = ctx.digest();

        alt_ctx.reset();

        for (cnt = 0; cnt < key.length; ++cnt) {
            alt_ctx.update(key, 0, key.length);
        }

        temp_result = alt_ctx.digest();

        p_bytes = new byte[key.length];

        for (cnt2 = 0, cnt = p_bytes.length; cnt >= 64; cnt -= 64) {
            System.arraycopy(temp_result, 0, p_bytes, cnt2, 64);
            cnt2 += 64;
        }

        System.arraycopy(temp_result, 0, p_bytes, cnt2, cnt);

        alt_ctx.reset();

        for (cnt = 0; cnt < 16 + (alt_result[0] & 0xFF); ++cnt) {
            alt_ctx.update(salt, 0, salt.length);
        }

        temp_result = alt_ctx.digest();

        s_bytes = new byte[salt.length];

        for (cnt2 = 0, cnt = s_bytes.length; cnt >= 64; cnt -= 64) {
            // TODO figure what this code was supposed to do, and if it is important
            // This code does not seem to do anything, and this inner loop is never reached in any of the test vectors
            // because it only runs if the salt is longer than 64 bytes and the salt has already been truncated to 16 bytes by this point.
            // I couldn't find corresponding code in the C reference implementation (after what was admittedly a quick glance)
            System.arraycopy(temp_result, 0, s_bytes, cnt2, 64);
            cnt2 += 64;
        }

        System.arraycopy(temp_result, 0, s_bytes, cnt2, cnt);

        /* Repeatedly run the collected hash value through SHA512 to burn CPU cycles.  */

        for (cnt = 0; cnt < rounds; ++cnt) {
            ctx.reset();

            if ((cnt & 1) != 0) {
                ctx.update(p_bytes, 0, key.length);
            } else {
                ctx.update(alt_result, 0, 64);
            }

            if (cnt % 3 != 0) {
                ctx.update(s_bytes, 0, salt.length);
            }

            if (cnt % 7 != 0) {
                ctx.update(p_bytes, 0, key.length);
            }

            if ((cnt & 1) != 0) {
                ctx.update(alt_result, 0, 64);
            } else {
                ctx.update(p_bytes, 0, key.length);
            }

            alt_result = ctx.digest();
        }

        buffer = new StringBuffer(sha512_salt_prefix);

        if (rounds_custom) {
            buffer.append(sha512_rounds_prefix);
            buffer.append(rounds);
            buffer.append("$");
        }

        buffer.append(saltStr);
        buffer.append("$");

        buffer.append(b64_from_24bit(alt_result[0], alt_result[21], alt_result[42], 4));
        buffer.append(b64_from_24bit(alt_result[22], alt_result[43], alt_result[1], 4));
        buffer.append(b64_from_24bit(alt_result[44], alt_result[2], alt_result[23], 4));
        buffer.append(b64_from_24bit(alt_result[3], alt_result[24], alt_result[45], 4));
        buffer.append(b64_from_24bit(alt_result[25], alt_result[46], alt_result[4], 4));
        buffer.append(b64_from_24bit(alt_result[47], alt_result[5], alt_result[26], 4));
        buffer.append(b64_from_24bit(alt_result[6], alt_result[27], alt_result[48], 4));
        buffer.append(b64_from_24bit(alt_result[28], alt_result[49], alt_result[7], 4));
        buffer.append(b64_from_24bit(alt_result[50], alt_result[8], alt_result[29], 4));
        buffer.append(b64_from_24bit(alt_result[9], alt_result[30], alt_result[51], 4));
        buffer.append(b64_from_24bit(alt_result[31], alt_result[52], alt_result[10], 4));
        buffer.append(b64_from_24bit(alt_result[53], alt_result[11], alt_result[32], 4));
        buffer.append(b64_from_24bit(alt_result[12], alt_result[33], alt_result[54], 4));
        buffer.append(b64_from_24bit(alt_result[34], alt_result[55], alt_result[13], 4));
        buffer.append(b64_from_24bit(alt_result[56], alt_result[14], alt_result[35], 4));
        buffer.append(b64_from_24bit(alt_result[15], alt_result[36], alt_result[57], 4));
        buffer.append(b64_from_24bit(alt_result[37], alt_result[58], alt_result[16], 4));
        buffer.append(b64_from_24bit(alt_result[59], alt_result[17], alt_result[38], 4));
        buffer.append(b64_from_24bit(alt_result[18], alt_result[39], alt_result[60], 4));
        buffer.append(b64_from_24bit(alt_result[40], alt_result[61], alt_result[19], 4));
        buffer.append(b64_from_24bit(alt_result[62], alt_result[20], alt_result[41], 4));
        buffer.append(b64_from_24bit((byte) 0x00, (byte) 0x00, alt_result[63], 2));

        /* Clear the buffer for the intermediate result so that people
       attaching to processes or reading core dumps cannot get any
       information. */

        ctx.reset();

        return buffer.toString();
    }

    private static String b64_from_24bit(byte B2, byte B1, byte B0, int size) {
        int v = ((((int) B2) & 0xFF) << 16) | ((((int) B1) & 0xFF) << 8) | ((int) B0 & 0xff);

        StringBuffer result = new StringBuffer();

        while (--size >= 0) {
            result.append(itoa64.charAt(v & 0x3f));
            v >>>= 6;
        }

        return result.toString();
    }

    /**
     * This method tests a plaintext password against a SHA512 Unix
     * Crypt'ed hash and returns true if the password matches the hash.
     *
     * @param ctx       first of a pair of MessageDigest.SHA-512 instances for the exclusive use of this thread for the duration of this method.  Required.
     * @param alt_ctx  second of a pair of MessageDigest.SHA-512 instances for the exclusive use of this thread for the duration of this method.  Required.
     * @param password   The plaintext password to test, already converted to bytes using some secure method such as getBytes(Charsets.UTF8).  Required.
     * @param verifierString The hash text we're testing against. We'll extract the salt and the round count from this String.  Required.
     * @return true if the password was verified successfully; otherwise, false
     */
    public static boolean verifyPassword(MessageDigest ctx, MessageDigest alt_ctx, byte[] password, String verifierString) {
        if (!verifierString.startsWith(sha512_salt_prefix))
            throw new RuntimeException("Verifier string does not start with " + sha512_salt_prefix + " -- verifier is not for Sha512Crypt");

        final char[] want = verifierString.toCharArray();
        final char[] got = crypt(ctx, alt_ctx, password, verifierString).toCharArray();

        int result;
        char[] left;
        char[] right;
        if (want.length != got.length) {
            // different lengths, so force comparison to fail, but still go through the motions to mitigate timing attacks.
            // compare got string with itself, to avoid revealing any further timing information that depends on the contents of secret verifierString and the attacker-chosen password
            result = 1;
            left = got;
            right = got;
        } else {
            // Do full comparison
            result = 0;
            left = want;
            right = got;
        }

        for (int i = 0; i < got.length; i++) {
            result |= left[i] ^ right[i];
        }

        return result == 0;
    }

    public static boolean verifyHashTextFormat(String sha512CryptText) {
        if (!sha512CryptText.startsWith(sha512_salt_prefix)) {
            return false;
        }

        sha512CryptText = sha512CryptText.substring(sha512_salt_prefix.length());

        if (sha512CryptText.startsWith(sha512_rounds_prefix)) {
            String num = sha512CryptText.substring(sha512_rounds_prefix.length(), sha512CryptText.indexOf('$'));

            try {
                Integer.valueOf(num);
            } catch (NumberFormatException ex) {
                return false;
            }

            sha512CryptText = sha512CryptText.substring(sha512CryptText.indexOf('$') + 1);
        }

        if (sha512CryptText.indexOf('$') > (SALT_LEN_MAX + 1)) {
            return false;
        }

        sha512CryptText = sha512CryptText.substring(sha512CryptText.indexOf('$') + 1);

        for (int i = 0; i < sha512CryptText.length(); i++) {
            if (itoa64.indexOf(sha512CryptText.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }
}
