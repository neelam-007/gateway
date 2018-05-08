package com.l7tech.server.identity.cert;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.rsa.sslj.x.P;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Constructor;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class CertificateAuthenticatorJavaTest {
    private static final String SAML_TOKEN_STRING =
            "<saml:Assertion AssertionID=\"SamlAssertion-2414d0358170b20c36ebe5a3328602f0\" " +
            "IssueInstant=\"2005-08-17T23:56:20.609Z\" Issuer=\"data.l7tech.com\" " +
            "MajorVersion=\"1\" MinorVersion=\"1\" " +
            "xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"> " +
            "<saml:Conditions NotBefore=\"2005-08-17T23:56:00.000Z\" " +
            "NotOnOrAfter=\"2005-08-18T00:01:00.000Z\"/> " +
            "<saml:AuthenticationStatement AuthenticationMethod=\"urn:ietf:rfc:3075\">  <saml:Subject> " +
            "<saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">" +
            "CN=Alice</saml:NameIdentifier> <saml:SubjectConfirmation> " +
            "<saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:holder-of-key</saml:ConfirmationMethod> " +
            "<xd:KeyInfo xmlns:xd=\"http://www.w3.org/2000/09/xmldsig#\"> <xd:X509Data> " +
            "<xd:X509Certificate>MIICgjCCAWqgAwIBAgIJALLt3UmjdE/TMA0GCSqGSIb3DQEBBQUAMDAxDjAMBgNVBAoTBU9BU0l" +
            "TMR4wHAYDVQQDExVPQVNJUyBJbnRlcm9wIFRlc3QgQ0EwIBcNMTgwMzI2MjMxNjIxWhgPMjExODAzMDIyMzE2MjFaMEIxDj" +
            "AMBgNVBAoTBU9BU0lTMSAwHgYDVQQLExdPQVNJUyBJbnRlcm9wIFRlc3QgQ2VydDEOMAwGA1UEAxMFQWxpY2UwgZ8wDQYJK" +
            "oZIhvcNAQEBBQADgY0AMIGJAoGBAIUJPCtG7JthGRmQ8G1iryHRwCaU7aBRNqPl5dakkUsyoZLQ/ZBp7pOor6PnP/8l2FwF" +
            "39bT8cR2lo2CqQqZ2UE7lCVq0Iz210U8Z1I7BcZw/ucEFhYE3NBsrXmI1eMTe9hwdvqvCmN48SWK1T4LaeYNN+P+QUSnkSE" +
            "m8MvFQMiPAgMBAAGjDzANMAsGA1UdDwQEAwIEsDANBgkqhkiG9w0BAQUFAAOCAQEAFFJdESl83b5ChMRW6xfJJsfvG+Hdi5" +
            "5Ppgz98gKgoYMewX4dGAbaELLxeGH1iWvtuvUjxW4jEBkoV5iVtKDVwwJElt/mOFTnTguhdd/OJYDnNtVQd+URvk6QnG8tA" +
            "voKQxqsp6DSrs81W/pWVXMXe3xektcfq+NlGl1ylbmsj+oFd+XfiZbKKfE9YU4Q9oGs48T70a4uvIRhyaswZlO1vu+QNHHs" +
            "s40mwrhZV9s1QEOV8+l1EiyornIOQLVX/mQoBY+2wh5I5G7IzZp6oWA+L578D5KI84Yh4xXKR5+1r4P1+yCDNxIO6J+Iyiz" +
            "b5cEEcaS25Qw6GMUuVd/KKYqUWw==</xd:X509Certificate> </xd:X509Data> </xd:KeyInfo> " +
            "</saml:SubjectConfirmation>  </saml:Subject>  " +
            "<saml:SubjectLocality DNSAddress=\"Data.l7tech.com\" IPAddress=\"192.168.1.154\"/> " +
            "</saml:AuthenticationStatement></saml:Assertion>";

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
        user.setLogin("Alice");

        certificate = TestDocuments.getWssInteropAliceCert();
        credentials = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(certificate), SslAssertion.class);
    }

    @Before
    public void setUp() throws Exception {
        certificateAuthenticator = new CertificateAuthenticator(clientCertManager, certValidationProcessor);
    }

    /**
     * Expect successful authentication with valid external certificate
     */
    @Test
    public void testAuthenticate_ValidCredentialsExternalCertificate_ReturnsAuthenticationResult() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        AuthenticationResult result = certificateAuthenticator.authenticateX509Credentials(credentials,
                certificate, user, null, auditor, false);

        assertEquals(user, result.getUser());
        assertEquals(certificate, result.getAuthenticatedCert());
    }

    /**
     * Expect successful authentication with lookup of certificate for user
     */
    @Test
    public void testAuthenticate_ValidCredentialsInternalCertificate_ReturnsAuthenticationResult() throws Exception {
        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        when(clientCertManager.getUserCert(eq(user))).thenReturn(certificate);

        AuthenticationResult result =
                certificateAuthenticator.authenticateX509Credentials(credentials, user, null, auditor);

        assertEquals(user, result.getUser());
        assertEquals(certificate, result.getAuthenticatedCert());
    }

    /**
     * Expect failure on no request certificate provided
     */
    @Test
    public void testAuthenticate_CredentialsWithoutCertificate_MissingCredentialsExceptionThrown() throws Exception {
        when(clientCertManager.getUserCert(eq(user))).thenReturn(certificate);

        LoginCredentials httpBasicCredentials =
                LoginCredentials.makeLoginCredentials(new HttpBasicToken("Alice", new char[0]), HttpBasic.class);

        try {
            certificateAuthenticator.authenticateX509Credentials(httpBasicCredentials, user, null, auditor);
            fail("Expected MissingCredentialsException");
        } catch (MissingCredentialsException e) {
            assertEquals("Request was supposed to contain a certificate, but does not", e.getMessage());
        }
    }

    /**
     * Expect null return value when ObjectModelException thrown attempting to forbid internal certificate reset.
     */
    @Test
    public void testAuthenticate_ObjectModelExceptionOnForbidCertReset_NullReturnValue() throws Exception {
        when(clientCertManager.getUserCert(eq(user))).thenReturn(certificate);

        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        doThrow(new UpdateException()).doNothing().when(clientCertManager).forbidCertReset(eq(user));

        AuthenticationResult result = certificateAuthenticator.authenticateX509Credentials(credentials,
                certificate, user, null, auditor, true);

        assertNull(result);
    }

    /**
     * Expect failure on no internal or external certificate present for user.
     */
    @Test
    public void testAuthenticate_NoCertificateFoundForUser_InvalidClientCertificateExceptionThrown() throws Exception {
        when(clientCertManager.getUserCert(eq(user))).thenReturn(null);

        try {
            certificateAuthenticator.authenticateX509Credentials(credentials, user, null, auditor);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("No certificate found for user Alice", e.getMessage());
        }
    }

    /**
     * Expect an InvalidClientCertificateException as a result of a FindException when searching for an internal
     * certificate for a user. The FindException results in a null value being specified for the "validCert" parameter
     * to the main authentication method.
     */
    @Test
    public void testAuthenticate_FindExceptionOnGetUserCert_InvalidClientCertificateExceptionThrown() throws Exception {
        when(clientCertManager.getUserCert(eq(user))).thenThrow(new FindException());

        try {
            certificateAuthenticator.authenticateX509Credentials(credentials, user, null, auditor);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("No certificate found for user Alice", e.getMessage());
        }
    }

    /**
     * Expect successful authentication from valid SAML credentials
     */
    @Test
    public void testAuthenticate_ValidSAMLCredentials_ReturnsAuthenticationResult() throws Exception {
        LoginCredentials aliceSamlTokenCredentials = LoginCredentials.makeLoginCredentials(
                new SamlAssertionV1(XmlUtil.parse(SAML_TOKEN_STRING).getDocumentElement(), null), RequireWssSaml.class);

        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.OK);

        AuthenticationResult result = certificateAuthenticator.authenticateX509Credentials(aliceSamlTokenCredentials,
                certificate, user, null, auditor, false);

        assertEquals(user, result.getUser());
        assertEquals(certificate, result.getAuthenticatedCert());
    }

    /**
     * Expect failure on invalid SAML payload in LoginCredentials
     */
    @Test
    public void testAuthenticate_InvalidSAMLPayload_BadCredentialsExceptionThrown() throws Exception {
        // create some invalid LoginCredentials - this shouldn't be possible in normal circumstances
        Constructor<LoginCredentials> constructor = LoginCredentials.class.getDeclaredConstructor(String.class,
                char[].class, CredentialFormat.class, SecurityToken.class, boolean.class, SecurityToken[].class,
                Class.class, String.class, Object.class);
        constructor.setAccessible(true);

        LoginCredentials invalidSamlCredentials = constructor.newInstance(null, new char[0], CredentialFormat.SAML,
                null, true, new SecurityToken[0], null, null, "not a SamlAssertion instance");

        try {
            certificateAuthenticator.authenticateX509Credentials(invalidSamlCredentials,
                    certificate, user, null, auditor, false);
            fail("Expected BadCredentialsException");
        } catch (BadCredentialsException e) {
            assertEquals("Unsupported SAML Assertion type: java.lang.String", e.getMessage());
        }
    }

    /**
     * Expect failure when the request certificate doesn't match the user's stored certificate.
     */
    @Test
    public void testAuthenticate_RequestCertMismatch_InvalidClientCertificateExceptionThrown() throws Exception {
        X509Certificate invalidCertificate = TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64);
        LoginCredentials invalidCredentials =
                LoginCredentials.makeLoginCredentials(new TlsClientCertToken(invalidCertificate), SslAssertion.class);

        when(certValidationProcessor.check(
                any(X509Certificate[].class), any(CertificateValidationType.class),
                any(CertificateValidationType.class), any(CertValidationProcessor.Facility.class), eq(auditor)))
                .thenReturn(CertificateValidationResult.REVOKED);

        try {
            certificateAuthenticator.authenticateX509Credentials(invalidCredentials,
                    certificate, user, null, auditor, false);
            fail("Expected InvalidClientCertificateException");
        } catch (InvalidClientCertificateException e) {
            assertEquals("Failed to authenticate user Alice using a client certificate [cn=test_rsa_1024] - request " +
                    "certificate doesn't match database certificate [cn=alice,ou=oasis interop test cert,o=oasis]",
                    e.getMessage());
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
            assertEquals("Certificate [cn=alice,ou=oasis interop test cert,o=oasis] path validation and/or " +
                    "revocation checking failed", e.getMessage());
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
            assertEquals("Certificate [cn=alice,ou=oasis interop test cert,o=oasis] validation failed:" +
                    "forced test exception", e.getMessage());
        }
    }
}