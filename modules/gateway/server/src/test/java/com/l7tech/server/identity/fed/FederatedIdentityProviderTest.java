package com.l7tech.server.identity.fed;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.TestCertValidationProcessor;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.saml.SamlAssertion;
import org.junit.Test;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 *
 */
public class FederatedIdentityProviderTest {

    private static final String SAML_ISSUER_B64 =
            "MIIDizCCAnOgAwIBAgIQWaCxRe3INcSU8VNJ4/HerDANBgkqhkiG9w0BAQUFADAy\n" +
            "MQ4wDAYDVQQKDAVPQVNJUzEgMB4GA1UEAwwXT0FTSVMgSW50ZXJvcCBUZXN0IFJv\n" +
            "b3QwHhcNMDUwMzE5MDAwMDAwWhcNMTkwMzE5MjM1OTU5WjAwMQ4wDAYDVQQKDAVP\n" +
            "QVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmR2GR3IduCfoZfvmwYpepKNZN6iaDcm4Jmqq\n" +
            "C3nN5NiuQ4ROq2YCRhG90QW8puhsO6XaRiRO6WQQpwdtm/tgseDAAdw0bMPWrnja\n" +
            "FhgFlaEB0eK5fu9UiCPGkwurWNc8EQlk2r71uCwOx6BYGFsnSnBEfj64zoVri2ol\n" +
            "ksXc2aos6urhujP6zvixsCxfo8Jq2v1yLUZpDaiTp2GfyDMSZKROcBz4FnEIN7yK\n" +
            "ZDMYpHSx2SmcwmQnjeeAx1EH876+PpycsbJwStt3lIYchk5vWqJSZzN7PElEgzLW\n" +
            "v8QeWZ0Zb8wteQyWrG5wN2FCTcqF3W29FBeZig6u5Y3mibwDYQIDAQABo4GeMIGb\n" +
            "MBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAqoiiGJmh0dHA6Ly9pbnRl\n" +
            "cm9wLmJidGVzdC5uZXQvY3JsL3Jvb3QuY3JsMA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
            "HQ4EFgQUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wHwYDVR0jBBgwFoAU3/6RlcdWSCY9\n" +
            "wNw5PcYJ90z6SOIwDQYJKoZIhvcNAQEFBQADggEBADvsOGOmhnxjwW2+2c17/W7o\n" +
            "4BolmqqlVFppFyEB4pUd+kqJ3XFiyVxweVwGdJfpUQLKP/KBzpqo4D11ttMaE2io\n" +
            "at0RUGylAl9PG/yalOH/vMgFq4XkhokoHPPD1tUbiuY8+pD+5jXR0NNj25yv7iSu\n" +
            "tZ7xA7bcMx+RQpDO9Mzhlk03SZt5FjsLrimLiEOtkTkBt8Gw1wCu253+Bt5JHboB\n" +
            "hgEa9hTmdQ3hYqO/q54Gymmd/NsNCxZDbUxVqu/XzBxZer6AQ4domv5fc9efCOk0\n" +
            "k06aMmYjKXEYI5i9OqutWu442ZXJV6lnWKZ1akFi/sA4DNnYPrz825+hzOeesBI=";
    private static final String SAML_TOKEN = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"saml-1281050097294\" Issuer=\"OASIS Interop Test CA\" IssueInstant=\"2010-08-05T23:14:57.344Z\"><saml:Conditions NotBefore=\"2010-08-05T23:12:57.000Z\" NotOnOrAfter=\"2080-08-05T22:18:40.573Z\"/><saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:HardwareToken\" AuthenticationInstant=\"2010-08-05T23:14:57.344Z\"><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">username</saml:NameIdentifier></saml:Subject><saml:SubjectLocality IPAddress=\"127.0.0.1\"/></saml:AuthenticationStatement><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#saml-1281050097294\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>vy3OhI8vLrPV6a6ALmIfwyW9UvE=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>E4dTACNE4wMOatPNa3umm/MB2GojLud9v+1KaIqxY/LUpSFNvGEyVW4NwwcQGWJRM1v82FKgLfhbyaj882p33vB76nxuDk711AzjOvyV61Pa+NvKLmy41O7MuiFbvAK9dVj70KIlXZYbDIA0rZv+khag2Ihu1M+uiv4vK+nZ8JYZIE7dAU91qh/31VZBAAL2U3e/eRkogvuGQv2y3cBjArlmnavz/qmK+4+yhFKs4i25F2Gh457C8wpGpzwSnVoBnc1sdLswRzToaFdoksNRFuFsp8+SdHI/ZpTw5zu5Ihpw23MwZJvIXIHkpM435GdSVGGOxTfFykckBvjkS8IUUg==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=OASIS Interop Test CA,O=OASIS</X509SubjectName><X509Certificate>MIIDizCCAnOgAwIBAgIQWaCxRe3INcSU8VNJ4/HerDANBgkqhkiG9w0BAQUFADAyMQ4wDAYDVQQKDAVPQVNJUzEgMB4GA1UEAwwXT0FTSVMgSW50ZXJvcCBUZXN0IFJvb3QwHhcNMDUwMzE5MDAwMDAwWhcNMTkwMzE5MjM1OTU5WjAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmR2GR3IduCfoZfvmwYpepKNZN6iaDcm4JmqqC3nN5NiuQ4ROq2YCRhG90QW8puhsO6XaRiRO6WQQpwdtm/tgseDAAdw0bMPWrnjaFhgFlaEB0eK5fu9UiCPGkwurWNc8EQlk2r71uCwOx6BYGFsnSnBEfj64zoVri2olksXc2aos6urhujP6zvixsCxfo8Jq2v1yLUZpDaiTp2GfyDMSZKROcBz4FnEIN7yKZDMYpHSx2SmcwmQnjeeAx1EH876+PpycsbJwStt3lIYchk5vWqJSZzN7PElEgzLWv8QeWZ0Zb8wteQyWrG5wN2FCTcqF3W29FBeZig6u5Y3mibwDYQIDAQABo4GeMIGbMBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAqoiiGJmh0dHA6Ly9pbnRlcm9wLmJidGVzdC5uZXQvY3JsL3Jvb3QuY3JsMA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wHwYDVR0jBBgwFoAU3/6RlcdWSCY9wNw5PcYJ90z6SOIwDQYJKoZIhvcNAQEFBQADggEBADvsOGOmhnxjwW2+2c17/W7o4BolmqqlVFppFyEB4pUd+kqJ3XFiyVxweVwGdJfpUQLKP/KBzpqo4D11ttMaE2ioat0RUGylAl9PG/yalOH/vMgFq4XkhokoHPPD1tUbiuY8+pD+5jXR0NNj25yv7iSutZ7xA7bcMx+RQpDO9Mzhlk03SZt5FjsLrimLiEOtkTkBt8Gw1wCu253+Bt5JHboBhgEa9hTmdQ3hYqO/q54Gymmd/NsNCxZDbUxVqu/XzBxZer6AQ4domv5fc9efCOk0k06aMmYjKXEYI5i9OqutWu442ZXJV6lnWKZ1akFi/sA4DNnYPrz825+hzOeesBI=</X509Certificate></X509Data></KeyInfo></ds:Signature></saml:Assertion>";

    /**
     * Test virtual user authentication with non email/subject DN name identifier format
     */
    @BugNumber(8974)
    @Test
    public void testVirtualUserAuthentication() throws Exception {
        final SamlSecurityToken samlToken = SamlAssertion.newInstance( XmlUtil.parse( SAML_TOKEN ).getDocumentElement() );
        final FederatedIdentityProviderImpl provider = buildTestProvider(true);
        final AuthenticationResult result = provider.authenticate( LoginCredentials.makeLoginCredentials( samlToken, null ));

        assertNotNull( "Authentication result null", result );
        assertNotNull( "Authentication result user null", result.getUser() );
        assertEquals("Unexpected login", "username", result.getUser().getLogin());
    }

    /**
     * Ensure virtual user authentication fails if there are no trusted certs.
     */
    @Test
    public void testVirtualUserAuthenticationFails() throws Exception {
        final SamlSecurityToken samlToken = SamlAssertion.newInstance( XmlUtil.parse( SAML_TOKEN ).getDocumentElement() );
        final FederatedIdentityProviderImpl provider = buildTestProvider(false);
        final AuthenticationResult result = provider.authenticate( LoginCredentials.makeLoginCredentials( samlToken, null ));

        assertNull( "Authentication result not null", result );
    }

    private FederatedIdentityProviderImpl buildTestProvider( boolean includeCerts ) throws InvalidIdProviderCfgException {
        final FederatedIdentityProviderImpl provider;

        final FederatedIdentityProviderConfig config = new FederatedIdentityProviderConfig();
        config.setName( "TEST-FIP" );
        if ( includeCerts ) {
            config.setTrustedCertGoids( new Goid[]{new Goid(0, 1)} );
        }
        config.setSamlSupported( true );
        config.setX509Supported( false );

        final FederatedGroupManager groupManager = new FederatedGroupManagerImpl();

        final FederatedUserManager userManager = new FederatedUserManagerImpl(null, null){
            @Override public FederatedUser findBySubjectDN( final String dn ) throws FindException {return null;}
            @Override public FederatedUser findByEmail( final String email ) throws FindException {return null;}
            @Override public FederatedUser findByLogin( final String login ) throws FindException {return null;}
        };

        provider = new FederatedIdentityProviderImpl();
        provider.setGroupManager( groupManager );
        provider.setUserManager( userManager );
        provider.setCertValidationProcessor( new TestCertValidationProcessor(){
            @Override
            public CertificateValidationResult check( final X509Certificate[] certificatePath,
                                                      final CertificateValidationType minimumValidationType,
                                                      final CertificateValidationType requestedValidationType,
                                                      final Facility facility,
                                                      final Audit auditor ) throws CertificateException {
                return certificatePath.length>0 && CertUtils.certsAreEqual( certificatePath[0], CertUtils.decodeCert( HexUtils.decodeBase64(SAML_ISSUER_B64))) ?
                        CertificateValidationResult.OK :
                        CertificateValidationResult.CANT_BUILD_PATH;
            }
        } );
        provider.setTrustedCertManager( new TestTrustedCertManager( new TrustedCert(){{ setGoid(new Goid(0, 1)); setCertBase64(SAML_ISSUER_B64); }} ) );
        provider.setTrustedCertServices( new TrustedCertServices(){
            @Override
            public void checkSslTrust( final X509Certificate[] serverCertChain, Set<Goid> requiredOids ) throws CertificateException {
            }

            @Override
            public Collection<TrustedCert> getCertsBySubjectDnFiltered( final String subjectDn,
                                                                        final boolean omitExpired,
                                                                        final Set<TrustedCert.TrustedFor> requiredTrustFlags,
                                                                        final Set<Goid> requiredOids ) throws FindException {
                return Collections.<TrustedCert>singleton( new TrustedCert(){{ setCertBase64(SAML_ISSUER_B64); }} );
            }

            @Override
            public Collection<TrustedCert> getAllCertsByTrustFlags( final Set<TrustedCert.TrustedFor> requiredTrustFlags ) throws FindException {
                return Collections.<TrustedCert>singleton( new TrustedCert(){{ setCertBase64(SAML_ISSUER_B64); }} );
            }
        } );
        provider.setIdentityProviderConfig( config );

        return provider;
    }


}
