package com.l7tech.common.password;

import com.l7tech.util.Charsets;
import org.junit.Ignore;
import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link Sha512CryptPasswordHasher}.
 */
public class Sha512CryptPasswordHasherTest {
    SecureRandom secureRandom = new SecureRandom();
    PasswordHasher hasher = new Sha512CryptPasswordHasher(secureRandom);
    byte[] pass = "blah".getBytes(Charsets.UTF8);

    @Test
    public void testHashPassword() throws Exception {
        String verifier = hasher.hashPassword(pass);
        assertTrue(Sha512Crypt.verifyHashTextFormat(verifier));
    }

    @Test
    public void testVerifyPassword() throws Exception {
        hasher.verifyPassword("testpassword".getBytes(Charsets.UTF8), "$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1");
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testVerifyPassword_incorrectPassword() throws Exception {
        hasher.verifyPassword("testpassworD".getBytes(Charsets.UTF8), "$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1");
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testVerifyPassword_incorrectVerifier() throws Exception {
        hasher.verifyPassword("testpassword".getBytes(Charsets.UTF8), "$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T2Y1");
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testVerifyPassword_incorrectSalt() throws Exception {
        hasher.verifyPassword("testpassword".getBytes(Charsets.UTF8), "$6$blahsalT$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1");
    }

    @Test
    public void testRoundTrip() throws Exception {
        String verifier = hasher.hashPassword(pass);
        assertTrue(Sha512Crypt.verifyHashTextFormat(verifier));
        hasher.verifyPassword(pass, verifier);
    }

    @Test(expected = IncorrectPasswordException.class)
    public void testRoundTrip_incorrectPassword() throws Exception {
        String verifier = hasher.hashPassword(pass);
        assertTrue(Sha512Crypt.verifyHashTextFormat(verifier));
        hasher.verifyPassword("wrong".getBytes(Charsets.UTF8), verifier);
    }

    @Test
    public void testOverrideNumRounds() throws Exception {
        System.setProperty(Sha512CryptPasswordHasher.PROP_DEFAULT_ROUNDS, "11386");
        final PasswordHasher customHasher = new Sha512CryptPasswordHasher(secureRandom);
        final String verifier = customHasher.hashPassword(pass);
        assertTrue(verifier.startsWith("$6$rounds=11386$"));
        customHasher.verifyPassword(pass, verifier);

        // Ensure regular non-custom hasher can still verify it (rounds encoded into salt)
        hasher.verifyPassword(pass, verifier);
    }

    @Test(expected = NullPointerException.class)
    public void testMissingSecureRandom() {
        new Sha512CryptPasswordHasher(null);
    }

    @Test
    public void testIsVerifierRecognized() {
        assertTrue(hasher.isVerifierRecognized("$6$$6B9OBs7yTMgDWPpqaAzI4nnQ37aDUD9fW.XPohFTR5nL9YlpfgAJ4PVDiM0W6dEffk1N0YmfY1uct4qBA25FN/"));
        assertFalse(hasher.isVerifierRecognized("6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
    }

    @Test
    public void testGetPrefix() {
        assertEquals("$6$", hasher.getPrefix());
    }

    @Test
    public void testExtractSaltFromVerifier() {
        assertEquals("", new String(hasher.extractSaltFromVerifier("")));
        assertEquals("asdf", new String(hasher.extractSaltFromVerifier("asdf")));
        assertEquals("$6$rounds=5000$blahsalt", new String(hasher.extractSaltFromVerifier("$6$rounds=5000$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY")));
        assertEquals("$6$", new String(hasher.extractSaltFromVerifier("$6$$6B9OBs7yTMgDWPpqaAzI4nnQ37aDUD9fW.XPohFTR5nL9YlpfgAJ4PVDiM0W6dEffk1N0YmfY1uct4qBA25FN/")));
        assertEquals("$6$blahsalt", new String(hasher.extractSaltFromVerifier("$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY")));
    }

    @Ignore("Enable to run perf test")
    @Test
    public void testPerformanceDefaultRounds() {
        testRoundPerf(0);
        // Results on Core i5 750 @ 2.67 GHz, quad core, circa 2011:   5667 ms
    }

    @Ignore("Enable to run perf test")
    @Test
    public void testPerformanceMinRounds() {
        testRoundPerf(1000);
        // Results on Core i5 750 @ 2.67 GHz, quad core, circa 2011:   1224 ms
    }

    @Ignore("Enable to run perf test")
    @Test
    public void testPerformance15kRounds() {
        testRoundPerf(15000);
        // Results on Core i5 750 @ 2.67 GHz, quad core, circa 2011:  16729 ms
    }

    private void testRoundPerf(int numRounds) {
        System.setProperty(Sha512CryptPasswordHasher.PROP_DEFAULT_ROUNDS, String.valueOf(numRounds));
        final PasswordHasher customHasher = new Sha512CryptPasswordHasher(secureRandom);
        long before = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            final String pass = "test" + i;
            customHasher.hashPassword(pass.getBytes(Charsets.UTF8));
        }
        long after = System.currentTimeMillis();
        long diff = after - before;
        System.out.println("1000 hashes using " + numRounds + " rounds: " + diff + " ms");
    }
}
