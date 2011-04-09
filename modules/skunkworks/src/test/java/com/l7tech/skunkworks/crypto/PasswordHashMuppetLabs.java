package com.l7tech.skunkworks.crypto;

import com.l7tech.common.password.CryptL7;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.security.MessageDigest;

/**
 * Test class for experimenting with password hashing algorithm.
 */
public class PasswordHashMuppetLabs {


    public static void main(String[] args) throws Exception {
        CryptL7 crypt = new CryptL7();

        //Security.insertProviderAt(new JsafeJCE(), 1);
        for (int i = 0; i < 31; ++i) {
            System.out.println("Work factor " + i + " results in " + crypt.getNumRoundsForWorkFactor(i) + " rounds");
        }

        CryptL7.Hasher sha256hasher = new CryptL7.SingleThreadedJceMessageDigestHasher(MessageDigest.getInstance("SHA-256"));
        long before = System.nanoTime();
        System.out.println("Hash of 'sekrit' with salt 'blah' with work factor 8: " + HexUtils.hexDump(crypt.computeHash("sekrit".getBytes(Charsets.UTF8), "blah".getBytes(Charsets.UTF8), 8, sha256hasher)));
        long after = System.nanoTime();
        System.out.println("Total time: " + ((after - before) / 1000000L) + " ms");
    }
}
