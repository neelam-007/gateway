package com.l7tech.common.io;

import com.l7tech.common.TestKeys;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.bouncycastle.util.test.FixedSecureRandom;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SignatureOutputStream
 */
public class SignatureOutputStreamTest {
    public static final byte[] RNG_SEED = "asjdkhfaksdjfhalsdfjhasdjkfahsdfjkahdfljkahldjkasfh".getBytes(Charsets.UTF8);

    public static final byte[] DATA = ("Some test data to sign, blah blah blah\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "more testing data.  Lots of stuff. $!@(*$&%#(&*%)(*&%.  More testing still.\n" +
            "asdf").getBytes(Charsets.UTF8);

    @Test
    @BugNumber(8968)
    public void testSigningWriteBlocks() throws Exception {
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
        Signature sigDirect = Signature.getInstance("SHA512withRSA");

        // Compute signature without signature output stream
        sigDirect.initSign(k.right, new FixedSecureRandom(RNG_SEED));
        sigDirect.update(DATA);
        byte[] rawSig = sigDirect.sign();

        // Compute signature using SignatureOutputStream
        Signature sigOs = Signature.getInstance("SHA512withRSA");
        sigOs.initSign(k.right, new FixedSecureRandom(RNG_SEED));
        SignatureOutputStream os = new SignatureOutputStream(sigOs);
        IOUtils.copyStream(new ByteArrayInputStream(DATA), os);
        byte[] osSig = os.sign();

        assertTrue("Must compute the same signature value when using SignatureOutputStream as when signing directly", Arrays.equals(rawSig, osSig));
    }

    @Test
    public void testSigningWriteBytes() throws Exception {
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
        Signature sigDirect = Signature.getInstance("SHA512withRSA");

        // Compute signature without signature output stream
        sigDirect.initSign(k.right, new FixedSecureRandom(RNG_SEED));
        sigDirect.update(DATA);
        byte[] rawSig = sigDirect.sign();

        // Compute signature using SignatureOutputStream
        Signature sigOs = Signature.getInstance("SHA512withRSA");
        sigOs.initSign(k.right, new FixedSecureRandom(RNG_SEED));
        SignatureOutputStream os = new SignatureOutputStream(sigOs);
        for (byte b : DATA) {
            os.write(b);
        }
        byte[] osSig = os.sign();

        assertTrue("Must compute the same signature value when using SignatureOutputStream as when signing directly", Arrays.equals(rawSig, osSig));
    }
}
