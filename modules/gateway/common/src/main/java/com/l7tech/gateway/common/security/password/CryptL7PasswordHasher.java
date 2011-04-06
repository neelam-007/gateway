package com.l7tech.gateway.common.security.password;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A password hasher that uses CryptL7-SHA256 to hash passwords, encoding the salt into the encoded password
 * string, and encoding the work factor into the salt.
 */
public class CryptL7PasswordHasher implements PasswordHasher {
    public static final String MESSAGE_DIGEST_ALGORITHM_NAME = SyspropUtil.getString("com.l7tech.CryptL7.messageDigestAlgorithm", "SHA-256");
    public static final int SALT_BYTES = SyspropUtil.getInteger("com.l7tech.CryptL7.saltBytes", 10);
    public static final String PREFIX = "$L7H$";

    private static final String B64STR = "[a-zA-Z0-9\\+/=]+"; // Matches one or more characters of Base-64
    private static final Pattern VERIFIER_PARSER = Pattern.compile(Pattern.quote(PREFIX) + "(" + B64STR + ")\\$(" + B64STR + ")");

    private final CryptL7 crypt = new CryptL7();
    private final SecureRandom secureRandom;
    private final ThreadLocal<CryptL7.Hasher> hasher = new ThreadLocal<CryptL7.Hasher>();

    public CryptL7PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public String hashPassword(byte[] passwordBytes) {
        try {
            byte workFactor = crypt.getDefaultWorkFactor();
            byte[] salt = newSalt();
            byte[] hashed = crypt.computeHash(passwordBytes, salt, workFactor, getHasher());
            return encodeVerifier(workFactor, salt, hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to hash password: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void verifyPassword(byte[] passwordBytes, String expectedHashedPassword) throws IncorrectPasswordException, PasswordHashingException {
        try {
            VerifierInfo info = decodeVerifier(expectedHashedPassword);
            byte[] wantHashed = info.hash;
            byte[] gotHashed = crypt.computeHash(passwordBytes, info.salt, info.workFactor, getHasher());

            if (gotHashed.length != wantHashed.length)
                throw new PasswordHashingException("Hash verifier settings differ from given verifier string and current verifier settings -- verifier length mismatch");

            // Use comparison that always takes the same amount of time regardless of the position of
            // the first difference (if any)
            int result = 0;
            for (int i = 0; i < gotHashed.length; i++) {
                byte got = gotHashed[i];
                byte want = wantHashed[i];
                result |= got ^ want;
            }

            if (result == 0)
                return; // Correct password

        } catch (NoSuchAlgorithmException e) {
            throw new PasswordHashingException("Unable to check password: " + ExceptionUtils.getMessage(e), e);
        }

        throw new IncorrectPasswordException();
    }

    @Override
    public boolean isVerifierRecognized(String verifierString) {
        try {
            decodeVerifier(verifierString);
            return true;
        } catch (PasswordHashingException e) {
            return false;
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    private byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private String encodeVerifier(byte workFactor, byte[] salt, byte[] hashed) {
        byte[] workSalt = new byte[salt.length + 1];
        workSalt[0] = workFactor;
        System.arraycopy(salt, 0, workSalt, 1, salt.length);
        return PREFIX + HexUtils.encodeBase64(workSalt, true) + "$" + HexUtils.encodeBase64(hashed, true);
    }

    private VerifierInfo decodeVerifier(String expectedHashedPassword) throws PasswordHashingException {
        if (!expectedHashedPassword.startsWith(PREFIX))
            throw new PasswordHashingException("Password verifier does not start with " + PREFIX + " prefix");
        final Matcher matcher = VERIFIER_PARSER.matcher(expectedHashedPassword);
        if (!matcher.matches())
            throw new PasswordHashingException("Unrecognized password verifier format");
        String workSaltB64 = matcher.group(1);
        String hashedB64 = matcher.group(2);
        byte[] workSalt = HexUtils.decodeBase64(workSaltB64, true);
        if (workSalt.length < 2)
            throw new PasswordHashingException("Verifier does not include enough salt bytes");
        byte[] hashed = HexUtils.decodeBase64(hashedB64, true);
        if (hashed.length < 2)
            throw new PasswordHashingException("Verifier does not include enough hash bytes");

        byte[] salt = new byte[workSalt.length - 1];
        byte workFactor = workSalt[0];
        System.arraycopy(workSalt, 1, salt, 0, salt.length);

        return new VerifierInfo(workFactor, salt, hashed);
    }

    private CryptL7.Hasher getHasher() throws NoSuchAlgorithmException {
        CryptL7.Hasher h = hasher.get();
        if (h == null) {
            h = new CryptL7.SingleThreadedJceMessageDigestHasher(MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_NAME));
            hasher.set(h);
        }
        return h;
    }

    private static class VerifierInfo {
        final byte workFactor;
        final byte[] salt;
        final byte[] hash;

        private VerifierInfo(byte workFactor, byte[] salt, byte[] hash) {
            this.workFactor = workFactor;
            this.salt = salt;
            this.hash = hash;
        }
    }
}
