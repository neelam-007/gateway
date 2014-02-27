package com.l7tech.server.identity.internal;

import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.Functions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Test password hasher
 */
public class TestPasswordHasher  extends Sha512CryptPasswordHasher {
    public TestPasswordHasher() {
        super(new MessageDigestFactory(), new SecureRandom());
    }

    private static class MessageDigestFactory implements Functions.Unary<MessageDigest, String> {
        @Override
        public MessageDigest call(String algorithm) {
            try {
                return JceProvider.getInstance().getMessageDigest(algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
