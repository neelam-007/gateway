package com.l7tech.common.password;

import com.l7tech.util.Charsets;
import org.junit.Before;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.SecureRandom;

import static org.junit.Assert.*;

/**
 * Unit test for {@link Sha512Crypt}.
 */
public class Sha512CryptTest {
    private static final SecureRandom secureRandom = new SecureRandom();

    // Expected value of "testpassword" hashed with "$6$blahsalt"
    static final String testpasswordHash = "$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1";

    MessageDigest ctx;
    MessageDigest alt_ctx;
    byte[] p = "testpassword".getBytes(Charsets.UTF8);
    String s = "$6$blahsalt";

    @Before
    public void setUpMessageDigests() throws Exception {
        ctx = MessageDigest.getInstance("SHA-512");
        alt_ctx = MessageDigest.getInstance("SHA-512");
    }

    @Test
    public void testCrypt_referenceTestVectors() throws Exception {
        String msgs[] = {
                /* Ulrich Drepper's original test vectors from the C reference implementation */
                "$6$saltstring", "Hello world!", "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1",
                "$6$rounds=10000$saltstringsaltstring", "Hello world!", "$6$rounds=10000$saltstringsaltst$OW1/O6BYHV6BcXZu8QVeXbDWra3Oeqh0sbHbbMCVNSnCM/UrjmM0Dp8vOuZeHBy/YTBmSK6H9qs/y3RnOaw5v.",
                "$6$rounds=5000$toolongsaltstring", "This is just a test", "$6$rounds=5000$toolongsaltstrin$lQ8jolhgVRVhY4b5pZKaysCLi0QBxGoNeKQzQ3glMhwllF7oGDZxUhx1yxdYcz/e1JSbq3y6JMxxl8audkUEm0",
                "$6$rounds=1400$anotherlongsaltstring", "a very much longer text to encrypt.  This one even stretches over morethan one line.", "$6$rounds=1400$anotherlongsalts$POfYwTEok97VWcjxIiSOjiykti.o/pQs.wPvMxQ6Fm7I6IoYN3CmLs66x9t0oSwbtEW7o7UmJEiDwGqd8p4ur1",
                "$6$rounds=77777$short", "we have a short salt string but not a short password", "$6$rounds=77777$short$WuQyW2YR.hBNpjjRhpYD/ifIw05xdfeEyQoMxIXbkvr0gge1a1x3yRULJ5CCaUeOxFmtlcGZelFl5CxtgfiAc0",
                "$6$rounds=123456$asaltof16chars..", "a short string", "$6$rounds=123456$asaltof16chars..$BtCwjqMJGx5hrJhZywWvt0RLE8uZ4oPwcelCjmw2kSYu.Ec6ycULevoBK25fs2xXgMNrCzIMVcgEJAstJeonj1",
                "$6$rounds=10$roundstoolow", "the minimum number is still observed", "$6$rounds=1000$roundstoolow$kUMsbe306n21p9R.FRkW3IGn.S9NPN0x50YhH1xhLsPuWGsUSklZt58jaTfF4ZEQpyUNGc0dqbpBYYBaHHrsX.",

                /* Extra test vectors added locally by Layer 7 to attempt to obtain additional code coverage */
                "$6$rounds=10$verylongsaltthatislongerthan64characterslonginordertoexerciseaportionofthecodetheprofilertellsmedidnotgethit", "a very long salt string to trigger a dusty corner of the code", "$6$rounds=1000$verylongsaltthat$G08ZiTJUYPxa9dpOj0nHqgAInGO5LTcQyVmKCjT9uNhHrzDffmA0B66SHyZ5naZUidOg0SOgh6at0wPJlHSHu/",
                "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1", "Hello world!", "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1",
                "$6$$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1", "Empty salt string", "$6$$6B9OBs7yTMgDWPpqaAzI4nnQ37aDUD9fW.XPohFTR5nL9YlpfgAJ4PVDiM0W6dEffk1N0YmfY1uct4qBA25FN/",
                "$6$$emptysaltstring", "Hello world!", "$6$$.SKR9BCFmNlzTpsFbxLHKPVAMUdqxN8.85WISsmC.fRIPfZ78cePl/wQJcKzjcsDe8rRtdaVxJHS/E1LzWy3./",
                "$6$$.SKR9BCFmNlzTpsFbxLHKPVAMUdqxN8.85WISsmC.fRIPfZ78cePl/wQJcKzjcsDe8rRtdaVxJHS/E1LzWy3./", "Hello world!", "$6$$.SKR9BCFmNlzTpsFbxLHKPVAMUdqxN8.85WISsmC.fRIPfZ78cePl/wQJcKzjcsDe8rRtdaVxJHS/E1LzWy3./",
                "$6$rounds=1400$anotherlongsalts$POfYwTEok97VWcjxIiSOjiykti.o/pQs.wPvMxQ6Fm7I6IoYN3CmLs66x9t0oSwbtEW7o7UmJEiDwGqd8p4ur1", "a very much longer text to encrypt.  This one even stretches over morethan one line.", "$6$rounds=1400$anotherlongsalts$POfYwTEok97VWcjxIiSOjiykti.o/pQs.wPvMxQ6Fm7I6IoYN3CmLs66x9t0oSwbtEW7o7UmJEiDwGqd8p4ur1",
        };

        String result;
        for (int t = 0; t < (msgs.length / 3); t++) {
            // Test initial hashing
            result = Sha512Crypt.crypt(ctx, alt_ctx, msgs[t * 3 + 1].getBytes(Charsets.UTF8), msgs[t * 3]);
            assertEquals(msgs[t * 3 + 2], result);

            // Test verifier hashing (using the complete verifier string as the salt)
            result = Sha512Crypt.crypt(ctx, alt_ctx, msgs[t * 3 + 1].getBytes(Charsets.UTF8), msgs[t * 3 + 2]);
            assertEquals(msgs[t * 3 + 2], result);
        }
    }

    @Test
    public void testCrypt() throws Exception {
        // Pass only the salt as the salt
        assertEquals(testpasswordHash, Sha512Crypt.crypt(ctx, alt_ctx, p, s));
    }

    @Test
    public void testCrypt_fullVerifier() throws Exception {
        // Pass full verifier string as the salt
        assertEquals(testpasswordHash, Sha512Crypt.crypt(ctx, alt_ctx, p, testpasswordHash));
    }

    @Test
    public void testCryptOmittedArgs() throws Exception {
        npe(null, null, p, s);
        npe(null, alt_ctx, p, s);
        npe(ctx, null, p, s);
        npe(ctx, alt_ctx, null, s);
        npe(ctx, alt_ctx, p, null);
        iae(ctx, ctx, p, s);
        iae(MessageDigest.getInstance("SHA-256"), alt_ctx, p, s);
        iae(ctx, MessageDigest.getInstance("MD5"), p, s);
    }

    private void npe(MessageDigest ctx, MessageDigest alt_ctx, byte[] password, String salt) {
        try {
            Sha512Crypt.crypt(ctx, alt_ctx, password, salt);
            fail("Expected NullPointerException not thrown");
        } catch (NullPointerException e) {
            // Ok
        }
    }

    private void iae(MessageDigest ctx, MessageDigest alt_ctx, byte[] password, String salt) {
        try {
            Sha512Crypt.crypt(ctx, alt_ctx, password, salt);
            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // Ok
        }
    }

    @Test
    public void testGenerateSalt_roundsNotSpecified() throws Exception {
        String salt = Sha512Crypt.generateSalt(secureRandom, 0);
        assertEquals(16, salt.length());
        assertTrue("salt should not include any rounds designation", !salt.startsWith("rounds="));
        assertTrue("salt should not include any dollar signs", salt.indexOf('$') < 0);
    }

    @Test
    public void testGenerateSalt_specifiedRounds() throws Exception {
        String salt = Sha512Crypt.generateSalt(secureRandom, 9997);
        final String wantPrefix = "rounds=9997$";
        assertEquals(16 + wantPrefix.length(), salt.length());
        assertTrue(salt.startsWith(wantPrefix));
        assertTrue("salt should contain only the one dollar sign", salt.substring(wantPrefix.length()).indexOf('$') < 0);
    }

    @Test
    public void testGenerateSalt_roundsTooLow() throws Exception {
        String salt = Sha512Crypt.generateSalt(secureRandom, 500);
        assertEquals(16, salt.length());
        assertTrue("with rounds < min, salt should not include any rounds designation", !salt.startsWith("rounds="));
        assertTrue("with rounds < min, salt should not include any dollar signs", salt.indexOf('$') < 0);
    }

    @Test
    public void testGenerateSalt_roundsTooHigh() throws Exception {
        String salt = Sha512Crypt.generateSalt(secureRandom, Integer.MAX_VALUE);
        assertEquals(16, salt.length());
        assertTrue("with rounds > max, salt should not include any rounds designation", !salt.startsWith("rounds="));
        assertTrue("with rounds > max, salt should not include any dollar signs", salt.indexOf('$') < 0);
    }

    @Test
    public void testVerifyPassword() throws Exception {
        assertTrue(Sha512Crypt.verifyPassword(ctx, alt_ctx, "Hello world!".getBytes(Charsets.UTF8),
                "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1"));
    }

    @Test
    public void testVerifyPassword_wrongPassword() throws Exception {
        assertFalse(Sha512Crypt.verifyPassword(ctx, alt_ctx, "Goodbye world!".getBytes(Charsets.UTF8),
                "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1"));
    }

    @Test
    public void testVerifyPassword_verifierTooShort() throws Exception {
        assertFalse(Sha512Crypt.verifyPassword(ctx, alt_ctx, "Hello world!".getBytes(Charsets.UTF8),
                "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35i"));
    }

    @Test
    public void testVerifyPassword_verifierTooLong() throws Exception {
        assertFalse(Sha512Crypt.verifyPassword(ctx, alt_ctx, "Hello world!".getBytes(Charsets.UTF8),
                "$6$saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1XXX"));
    }

    @Test(expected = RuntimeException.class)
    public void testVerifyPassword_missingPrefix() throws Exception {
        Sha512Crypt.verifyPassword(ctx, alt_ctx, "Hello world!".getBytes(Charsets.UTF8),
                "saltstring$svn8UoSVapNtMuq1ukKS4tPQd8iKwSMHWjl/O817G3uBnIFNjnQJuesI68u4OTLiBFdcbYEdFCoEOfaS35inz1XXX");
    }

    @Test
    public void testVerifyHashFormat() {
        assertTrue(Sha512Crypt.verifyHashTextFormat(testpasswordHash));
        assertTrue(Sha512Crypt.verifyHashTextFormat("$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY"));
        assertTrue(Sha512Crypt.verifyHashTextFormat("$6$rounds=5000$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY"));
        assertTrue(Sha512Crypt.verifyHashTextFormat("$6$$6B9OBs7yTMgDWPpqaAzI4nnQ37aDUD9fW.XPohFTR5nL9YlpfgAJ4PVDiM0W6dEffk1N0YmfY1uct4qBA25FN/"));

        assertFalse(Sha512Crypt.verifyHashTextFormat("6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$rounds=asdf$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$rounds=$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrYjUx7q3T1Y1"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$$rounds=5000$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$saltthatiswaywaywaytoolongtousecorrectly$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$rounds=2000$saltthatiswaywaywaytoolongtousecorrectly$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY"));
        assertFalse(Sha512Crypt.verifyHashTextFormat("$6$blahsalt$QO7R6BG1VY.HEC0GLiR.qdDaKfU3v.Eug35ENsE29KmeDQrL71vI3P7FLQq2/7325QWfjgAdLnrY$"));
    }

    @Test
    public void testFullRoundTrip() throws Exception {
        String verifier = Sha512Crypt.crypt(ctx, alt_ctx, p, Sha512Crypt.generateSalt(secureRandom, 87028));
        assertTrue(Sha512Crypt.verifyHashTextFormat(verifier));
        assertTrue(Sha512Crypt.verifyPassword(ctx, alt_ctx, p, verifier));
    }
}
