package com.l7tech.common.password;

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

    public Sha512CryptPasswordHasher(SecureRandom secureRandom) {
        if (secureRandom == null)
            throw new NullPointerException("secureRandom is required");
        this.secureRandom = secureRandom;
    }

    public Sha512CryptPasswordHasher() {
        secureRandom = new SecureRandom();
    }

    @Override
    public String hashPassword(byte[] passwordBytes) {
        Pair<MessageDigest, MessageDigest> digs = getDigests();
        return Sha512Crypt.crypt(digs.left, digs.right, passwordBytes, Sha512Crypt.generateSalt(secureRandom, DEFAULT_ROUNDS));
    }

    @Override
    public void verifyPassword(byte[] passwordBytes, String expectedHashedPassword) throws IncorrectPasswordException, PasswordHashingException {
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

    private Pair<MessageDigest, MessageDigest> getDigests() {
        try {
            Pair<MessageDigest, MessageDigest> d = digests.get();
            if (d == null) {
                MessageDigest mda = MessageDigest.getInstance("SHA-512");
                MessageDigest mdb = MessageDigest.getInstance("SHA-512");
                d = new Pair<MessageDigest, MessageDigest>(mda, mdb);
                digests.set(d);
            }
            return d;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
