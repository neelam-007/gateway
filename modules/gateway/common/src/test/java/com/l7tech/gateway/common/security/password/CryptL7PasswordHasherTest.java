package com.l7tech.gateway.common.security.password;

import com.l7tech.util.Charsets;
import org.junit.Test;

import java.security.SecureRandom;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for CryptL7PasswordHasher.
 */
public class CryptL7PasswordHasherTest {

    static final byte[] SEKRIT_PASS = "sekrit".getBytes(Charsets.UTF8);
    static final String SEKRIT_HASH = "$L7H$BLzzjTZpUgf/0sY=$j5l2mcXu/cylM0FcoxsXsWPQaLiiIyyg4PZb0qv9pjg=";

    private CryptL7PasswordHasher hasher = new CryptL7PasswordHasher(new SecureRandom());

    @Test
    public void testHashPassword() throws Exception {
        String verifier = hasher.hashPassword(SEKRIT_PASS);
        assertNotNull(verifier);
        assertTrue(verifier.length() > 5);
        assertTrue(verifier.startsWith(CryptL7PasswordHasher.PREFIX));
    }

    @Test
    public void testVerifyPassword() throws Exception {
        hasher.verifyPassword(SEKRIT_PASS, SEKRIT_HASH);
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testIncorrectPassword_tooLong() throws Exception {
        hasher.verifyPassword("sekritt".getBytes(Charsets.UTF8), SEKRIT_HASH);
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testIncorrectPassword_tooShort() throws Exception {
        hasher.verifyPassword("sekri".getBytes(Charsets.UTF8), SEKRIT_HASH);
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testIncorrectPassword_empty() throws Exception {
        hasher.verifyPassword("".getBytes(Charsets.UTF8), SEKRIT_HASH);
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testIncorrectPassword_totallyWrong() throws Exception {
        hasher.verifyPassword("Green Dots On Mars Baste Gazebos".getBytes(Charsets.UTF8), SEKRIT_HASH);
    }

    @Test
    public void testHashRoundTrip() throws Exception {
        String verifier = hasher.hashPassword("asdfasdf".getBytes(Charsets.UTF8));
        hasher.verifyPassword("asdfasdf".getBytes(Charsets.UTF8), verifier);
    }

    @Test(expected = PasswordHashingException.class)
    public void testBadVerifier_empty() throws Exception {
        hasher.verifyPassword(SEKRIT_PASS, "");
    }

    @Test(expected = PasswordHashingException.class)
    public void testBadVerifier_wrongPrefix() throws Exception {
        hasher.verifyPassword(SEKRIT_PASS, "$L7h$BLzzjTZpUgf/0sY=$j5l2mcXu/cylM0FcoxsXsWPQaLiiIyyg4PZb0qv9pjg=");
    }

    @Test(expected = PasswordHashingException.class)
    public void testBadVerifier_missingSalt() throws Exception {
        hasher.verifyPassword(SEKRIT_PASS, "$L7H$j5l2mcXu/cylM0FcoxsXsWPQaLiiIyyg4PZb0qv9pjg=");
    }

    @Test
    public void testIsVerifierRecognized() {
        assertTrue(hasher.isVerifierRecognized(SEKRIT_HASH));
        assertFalse(hasher.isVerifierRecognized("$L7H$j5l2mcXu/cylM0FcoxsXsWPQaLiiIyyg4PZb0qv9pjg="));
        assertFalse(hasher.isVerifierRecognized("$L7h$BLzzjTZpUgf/0sY=$j5l2mcXu/cylM0FcoxsXsWPQaLiiIyyg4PZb0qv9pjg="));
    }

    @Test
    public void testGetPrefix() {
        assertEquals("$L7H$", hasher.getPrefix());
    }
}
