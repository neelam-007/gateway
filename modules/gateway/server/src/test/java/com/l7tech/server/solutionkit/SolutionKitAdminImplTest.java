package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.SignatureException;

/**
 * Solution Kit Admin implementation test
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitAdminImplTest {

    private static SignatureVerifier SIGNATURE_VERIFIER;
    private static final String[] SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com",
            "cn=signer.team3.apim.ca.com",
            "cn=signer.team4.apim.ca.com"
    };

    @Mock
    private LicenseManager licenseManager;
    @Mock
    private SolutionKitManager solutionKitManager;

    private SolutionKitAdmin solutionKitAdmin;

    @BeforeClass
    public static void beforeClass() throws Exception {
        SignatureTestUtils.beforeClass();

        SIGNATURE_VERIFIER = SignatureTestUtils.createSignatureVerifier(SIGNER_CERT_DNS);
        Assert.assertNotNull("signature verifier is created", SIGNATURE_VERIFIER);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SignatureTestUtils.afterClass();
    }

    @Before
    public void before() throws Exception {
        solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager, SIGNATURE_VERIFIER);
        Assert.assertNotNull("SolutionKitAdmin is created", solutionKitAdmin);
    }


    @Test
    public void toDo() throws Exception {
        // TODO
    }

    @Test
    public void testVerifySkarSignature() throws Exception {
        final byte[] sampleBytes = "test data".getBytes(Charsets.UTF8);
        final byte[] sampleBytesDigest = getSha256Digest(sampleBytes);

        // test using a trusted signer with all signing cert DNs
        for (final String signerDN : SIGNER_CERT_DNS) {
            final byte[] signedBytes = SignatureTestUtils.sign(SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleBytes), signerDN);
            solutionKitAdmin.verifySkarSignature(sampleBytesDigest, SignatureTestUtils.getSignature(signedBytes));
        }

        // create untrusted signer with same DNs plus a new one
        final String[] untrustedDNs = ArrayUtils.concat(
                SIGNER_CERT_DNS,
                new String[] {
                        "cn=signer.untrusted.apim.ca.com"
                }
        );
        final SignatureVerifier untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

        // test using untrusted signer with all signing cert DNs
        for (final String signerDN : untrustedDNs) {
            final byte[] signedBytes = SignatureTestUtils.sign(untrustedSigner, new ByteArrayInputStream(sampleBytes), signerDN);
            try {
                solutionKitAdmin.verifySkarSignature(sampleBytesDigest, SignatureTestUtils.getSignature(signedBytes));
                Assert.fail("verifySkarSignature should have failed with SignatureException.");
            } catch (SignatureException e) {
                Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to verify signer certificate"));
            }
        }

//        // test data tampering after signing
//        byte[] signedBytes = SignatureTestUtils.sign(SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleBytes), SIGNER_CERT_DNS[0]);
//        Assert.assertThat(signedBytes[3], Matchers.not(Matchers.is((byte)2)));
//        signedBytes[3] = 2;
//        try {
//            solutionKitAdmin.verifySkarSignature(sampleBytesDigest, SignatureTestUtils.getSignature(signedBytes));
//            Assert.fail("verifySkarSignature should have failed with SignatureException.");
//        } catch (SignatureException e) {
//            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to verify signer certificate"));
//        }
    }

    private static byte[] getSha256Digest(final byte[] bytes) throws Exception {
        return getSha256Digest(new ByteArrayInputStream(bytes));
    }

    private static byte[] getSha256Digest(final InputStream stream) throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final DigestInputStream dis = new DigestInputStream(stream, messageDigest);
        IOUtils.copyStream(dis, new com.l7tech.common.io.NullOutputStream());
        return dis.getMessageDigest().digest();
    }


}
