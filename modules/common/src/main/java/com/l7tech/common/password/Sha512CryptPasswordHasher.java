package com.l7tech.common.password;

import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * A PasswordHasher that uses James Ratcliff of utexas's Java implementation of Ulrich Drepper of Red Hat's SHA-512 based password hashing scheme.
 */
public class Sha512CryptPasswordHasher implements PasswordHasher {
    public static final String PROP_DEFAULT_ROUNDS = "com.l7tech.Sha512Crypt.defaultRounds";
    private final int DEFAULT_ROUNDS = SyspropUtil.getInteger(PROP_DEFAULT_ROUNDS, 0);
    private final SecureRandom secureRandom;
    private final ThreadLocal<Pair<MessageDigest, MessageDigest>> digests = new ThreadLocal<Pair<MessageDigest, MessageDigest>>();
    private final Functions.Unary<MessageDigest,String> messageDigestFactory;

    public static final Functions.Unary<MessageDigest,String> DEFAULT_MESSAGE_DIGEST_FACTORY = new Functions.Unary<MessageDigest, String>() {
        @Override
        public MessageDigest call(String algorithm) {
            try {
                return MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public Sha512CryptPasswordHasher() {
        this(DEFAULT_MESSAGE_DIGEST_FACTORY, new SecureRandom());
    }

    public Sha512CryptPasswordHasher(Functions.Unary<MessageDigest,String> messageDigestFactory, SecureRandom secureRandom) {
        if (messageDigestFactory == null)
            throw new NullPointerException("messageDigestFactory is required");
        if (secureRandom == null)
            throw new NullPointerException("secureRandom is required");
        this.secureRandom = secureRandom;
        this.messageDigestFactory = messageDigestFactory;
    }

    @Override
    public String hashPassword(byte[] passwordBytes) {
        Pair<MessageDigest, MessageDigest> digs = getDigests();
        return Sha512Crypt.crypt(digs.left, digs.right, passwordBytes, Sha512Crypt.generateSalt(secureRandom, DEFAULT_ROUNDS));
    }

    @Override
    public void verifyPassword(byte[] passwordBytes, String expectedHashedPassword) throws IncorrectPasswordException {
        Pair<MessageDigest, MessageDigest> digs = getDigests();
        if (!Sha512Crypt.verifyPassword(digs.left, digs.right, passwordBytes, expectedHashedPassword))
            throw new IncorrectPasswordException();
    }

    @Override
    public boolean isVerifierRecognized(String verifierString) {
        return Sha512Crypt.verifyHashTextFormat(verifierString);
    }

    @Override
    public String getPrefix() {
        return Sha512Crypt.sha512_salt_prefix;
    }

    @Override
    public byte[] extractSaltFromVerifier(String hashedPassword) {
        return Sha512Crypt.extractSalt(hashedPassword).getBytes(Charsets.UTF8);
    }

    private Pair<MessageDigest, MessageDigest> getDigests() {
        try {
            Pair<MessageDigest, MessageDigest> d = digests.get();
            if (d == null) {
                MessageDigest mda = messageDigestFactory.call("SHA-512");
                MessageDigest mdb = messageDigestFactory.call("SHA-512");
                d = new Pair<MessageDigest, MessageDigest>(mda, mdb);
                digests.set(d);
            }
            return d;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
