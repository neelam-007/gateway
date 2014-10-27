package com.l7tech.server.transport.http;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class SslClientTrustManagerTest {
    private static final String RSA_256 = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA";

    private static X509Certificate certificate;

    @Mock
    private TrustedCertServices trustedCertServices;

    @Mock
    private CertValidationProcessor certValidationProcessor;

    private SslClientTrustManager sslClientTrustManager;

    private KeyUsageChecker defaultKeyUsageChecker;

    @BeforeClass
    public static void beforeClass() throws Exception {
        certificate = TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64);
    }

    @Before
    public void setUp() throws Exception {
        sslClientTrustManager = new SslClientTrustManager(trustedCertServices, certValidationProcessor,
                CertValidationProcessor.Facility.ROUTING, new HashSet<Goid>());

        defaultKeyUsageChecker = KeyUsageChecker.getDefault();
    }

    @After
    public void tearDown() {
        KeyUsageChecker.setDefault(defaultKeyUsageChecker);
    }

    @Test
    public void testGetAcceptedIssuers_ZeroLengthArrayReturned() throws Exception {
        X509Certificate[] issuers = sslClientTrustManager.getAcceptedIssuers();

        assertEquals(0, issuers.length);
    }

    @Test
    public void testCheckClientTrusted_UnsupportedOperationExceptionThrown() throws CertificateException {
        try {
            sslClientTrustManager.checkClientTrusted(new X509Certificate[] {certificate}, RSA_256);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("This trust manager can only be used for outbound SSL connections", e.getMessage());
        }
    }

    @Test
    public void testCheckServerTrusted_ValidCertificate_Succeeds() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), any(Auditor.class)))
                .thenReturn(CertificateValidationResult.OK);

        try {
            sslClientTrustManager.checkServerTrusted(new X509Certificate[]{certificate}, RSA_256);
        } catch (Exception e) {
            fail("Method should succeed without exception");
        }
    }

    @Test
    public void testCheckServerTrusted_ExpiredCertificate_CertificateExceptionThrown() throws Exception {
        X509Certificate expiredCert = TestDocuments.getExpiredServerCertificate();

        try {
            sslClientTrustManager.checkServerTrusted(new X509Certificate[] {expiredCert}, RSA_256);
            fail("Expected CertificateException");
        } catch (CertificateException e) {
            assertEquals("Certificate expired or not yet valid: CN=riker", e.getMessage());
        }
    }

    @Test
    public void testCheckServerTrusted_RevokedCertificate_CertificateExceptionThrown() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), any(Auditor.class)))
                .thenReturn(CertificateValidationResult.REVOKED);

        try {
            sslClientTrustManager.checkServerTrusted(new X509Certificate[] {certificate}, RSA_256);
            fail("Expected CertificateException");
        } catch (CertificateException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation checking failed",
                    e.getMessage());
        }
    }

    @Test
    public void testCheckServerTrusted_CertMissingKeyUsageActivity_CertificateExceptionThrown() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), any(Auditor.class)))
                .thenReturn(CertificateValidationResult.OK);

        // force a key usage failure
        KeyUsageChecker.setDefault(new KeyUsageChecker(null, null));

        try {
            sslClientTrustManager.checkServerTrusted(new X509Certificate[] {certificate}, RSA_256);
            fail("Expected KeyUsageException");
        } catch (KeyUsageException e) {
            assertEquals("Certificate key usage or extended key usage disallowed by key usage enforcement " +
                    "policy for activity: sslServerRemote", e.getMessage());
        }
    }
}