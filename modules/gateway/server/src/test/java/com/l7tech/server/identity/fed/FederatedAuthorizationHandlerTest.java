package com.l7tech.server.identity.fed;

import com.l7tech.common.TestKeys;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class FederatedAuthorizationHandlerTest {
    private static X509Certificate x509Certificate;

    @Mock
    private FederatedIdentityProvider federatedIdentityProvider;

    @Mock
    private FederatedIdentityProviderConfig providerConfig;

    @Mock
    private TrustedCertServices trustedCertServices;

    @Mock
    private ClientCertManager clientCertManager;

    @Mock
    private CertValidationProcessor certValidationProcessor;

    @Mock
    private Auditor auditor;

    @Mock
    private Set<Goid> trustedCertGoids;

    @Mock
    private FederatedUser federatedUser;

    private FederatedAuthorizationHandler authorizationHandler;

    @BeforeClass
    public static void beforeClass() throws Exception {
        x509Certificate = TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64);
    }

    @Before
    public void setUp() {
        when(federatedIdentityProvider.getConfig()).thenReturn(providerConfig);

        when(providerConfig.getCertificateValidationType())
                .thenReturn(CertificateValidationType.CERTIFICATE_ONLY);

//        /**
//         * N.B, This is a brittle workaround for our inability to mock static methods, in this case
//         * CertUtils.getCertificateIdentifyingInformation(). If this method is changed, this unit
//         * test is likely to fail.
//         */
//        when(testCert.getSubjectX500Principal()).thenReturn(new X500Principal("cn=test_cert"));

        authorizationHandler = new FederatedAuthorizationHandler(
                federatedIdentityProvider,
                trustedCertServices,
                clientCertManager,
                certValidationProcessor,
                auditor,
                trustedCertGoids);
    }

    @Test
    public void testValidateCertificate_ValidCertificate_Succeeds() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        try {
            authorizationHandler.validateCertificate(x509Certificate, false);
        } catch (Exception e) {
            fail("Method should succeed without exception");
        }
    }

    @Test
    public void testValidateCertificate_RevokedNonClientCertificate_AuthenticationExceptionThrown()
            throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.REVOKED);

        try {
            authorizationHandler.validateCertificate(x509Certificate, false);  // isClient set to false
            fail("Expected AuthenticationException");
        } catch (InvalidClientCertificateException e) {
            fail("Expected AuthenticationException");
        } catch (AuthenticationException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation checking failed", e.getMessage());
        }
    }

    @Test
    public void testValidateCertificate_RevokedClientCertificate_InvalidClientCertificateExceptionThrown()
            throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.REVOKED);

        try {
            authorizationHandler.validateCertificate(x509Certificate, true);  // isClient set to true
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation checking failed", e.getMessage());
        } catch (AuthenticationException e) {
            fail("Expected InvalidClientCertificateException");
        }
    }

    @Test
    public void testValidateCertificate_NonClientCertificateCheckThrowsException_AuthenticationExceptionThrown()
            throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenThrow(new CertificateException());

        try {
            authorizationHandler.validateCertificate(x509Certificate, false);  // isClient set to false
            fail("Expected AuthenticationException");
        } catch (InvalidClientCertificateException e) {
            fail("Expected AuthenticationException");
        } catch (AuthenticationException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation checking error", e.getMessage());
        }
    }

    @Test
    public void testValidateCertificate_ClientCertificateCheckThrowsException_InvalidClientCertificateExceptionThrown()
            throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenThrow(new CertificateException());

        try {
            authorizationHandler.validateCertificate(x509Certificate, true);  // isClient set to true
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation checking error", e.getMessage());
        } catch (AuthenticationException e) {
            fail("Expected InvalidClientCertificateException");
        }
    }

    @Test
    public void testCheckCertificateMatch_RequestCertFound_Succeeds()
            throws Exception {
        when(clientCertManager.getUserCert(eq(federatedUser))).thenReturn(x509Certificate);

        try {
            authorizationHandler.checkCertificateMatch(new FederatedUser(), x509Certificate);
        } catch (BadCredentialsException e) {
            fail("Method should succeed without exception");
        }
    }

    @Test
    public void testCheckCertificateMatch_NoCertForUserAndProviderHasNoCerts_BadCredentialsExceptionThrown()
            throws Exception {
        when(clientCertManager.getUserCert(eq(federatedUser))).thenReturn(null);
        when(trustedCertGoids.isEmpty()).thenReturn(true);

        try {
            authorizationHandler.checkCertificateMatch(new FederatedUser(), x509Certificate);
            fail("Expected BadCredentialsException");
        } catch (BadCredentialsException e) {
            assertEquals("User com.l7tech.identity.User.\n" +
                    "\tName=null\n" +
                    "\tFirst name=null\n" +
                    "\tLast name=null\n" +
                    "\tLogin=null\n" +
                    "\tproviderId=0000000000000000ffffffffffffffff " +
                    "has no client certificate imported, and this Federated Identity Provider " +
                    "has no CA certs that are trusted", e.getMessage());
        }
    }

    @Test
    public void testCheckCertificateMatch_UserCertDoesNotMatchImportedCert_BadCredentialsExceptionThrown()
            throws Exception {
        when(clientCertManager.getUserCert(eq(federatedUser))).thenReturn(x509Certificate);

        try {
            authorizationHandler.checkCertificateMatch(federatedUser, null);
            fail("Expected BadCredentialsException");
        } catch (BadCredentialsException e) {
            assertEquals("Request certificate [] for user federatedUser] does not match " +
                    "previously imported certificate [cn=test_rsa_512]", e.getMessage());
        }
    }
}
