package com.l7tech.server.identity.cert;

import com.l7tech.common.TestKeys;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.cert.CertValidationProcessor;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class CertificateAuthenticatorJavaTest {
    private static X509Certificate certificate;
    private static LoginCredentials credentials;

    @Mock
    private ClientCertManager clientCertManager;

    @Mock
    private CertValidationProcessor certValidationProcessor;

    @Mock
    private Auditor auditor;

    private static LdapUser user;

    private CertificateAuthenticator certificateAuthenticator;

    @BeforeClass
    public static void beforeClass() throws Exception {
        user = new LdapUser();
        user.setLogin("test_user");

        certificate = TestKeys.getCert(TestKeys.RSA_512_CERT_X509_B64);
        credentials = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(certificate), SslAssertion.class);
    }

    @Before
    public void setUp() throws Exception {
        certificateAuthenticator = new CertificateAuthenticator(clientCertManager, certValidationProcessor);
    }

    @Test
    public void testAuthenticate_ValidCredentials_ReturnsAuthenticationResult() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        AuthenticationResult result = certificateAuthenticator.authenticateX509Credentials(credentials,
                certificate, user, null, auditor, false);

        assertEquals(user, result.getUser());
        assertEquals(certificate, result.getAuthenticatedCert());
    }

    @Test
    public void testAuthenticate_RequestCertMismatchValid_InvalidClientCertificateExceptionThrown() throws Exception {
        X509Certificate validCertificate = TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64);

        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.REVOKED);

        when(clientCertManager.getUserCert(eq(user))).thenReturn(certificate);

        try {
            certificateAuthenticator.authenticateX509Credentials(credentials,
                    validCertificate, user, null, auditor, false);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Failed to authenticate user test_user using a client certificate [cn=test_rsa_512] - " +
                    "request certificate doesn't match database certificate [cn=test_rsa_1024]", e.getMessage());
        }
    }

    @Test
    public void testAuthenticate_RequestCertificateRevoked_InvalidClientCertificateExceptionThrown() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.REVOKED);

        try {
            certificateAuthenticator.authenticateX509Credentials(credentials,
                    certificate, user, null, auditor, false);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Certificate [cn=test_rsa_512] path validation and/or revocation " +
                    "checking failed", e.getMessage());
        }
    }

    @Test
    public void testAuthenticate_CertValidationThrowsCertificateException_InvalidClientCertificateExceptionThrown()
            throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenThrow(new CertificateException("forced test exception"));

        try {
            certificateAuthenticator.authenticateX509Credentials(credentials,
                    certificate, user, null, auditor, false);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Certificate [cn=test_rsa_512] validation failed:forced test exception", e.getMessage());
        }
    }
}