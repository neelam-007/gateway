package com.l7tech.external.assertions.ncesval.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.CertificateInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.security.cert.TestCertValidationProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Test the NcesValidatorAssertion.
 */
public class ServerNcesValidatorAssertionTest {

    @Test
    public void testUntrustedSigner() throws Exception {
        NcesValidatorAssertion assertion = new NcesValidatorAssertion();
        assertion.setCertificateValidationType( CertificateValidationType.PATH_VALIDATION );
        assertion.setSamlRequired( true );
        assertion.setTrustedCertificateInfo( new CertificateInfo[]{
                new CertificateInfo( TestDocuments.getWssInteropBobCert() )
        } );

        final CertValidationProcessor certValidationProcessor = new TestCertValidationProcessor();
        final SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver();
        final TrustedCertServices trustedCertServices = getTrustedCertServices();

        // Test failure
        {
            ServerNcesValidatorAssertion ncesValidator =
                    new ServerNcesValidatorAssertion( assertion, certValidationProcessor, securityTokenResolver, trustedCertServices, false  );

            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(XmlUtil.parse(REQUEST_COMBINED_SIGNATURE)), new Message() );
            AssertionStatus status = ncesValidator.checkRequest( pec );
            Assert.assertEquals( "Fails with FALSIFIED", AssertionStatus.FALSIFIED, status );
        }
    }

    @Test
    public void testCombinedSignatureAttack() throws Exception {
        NcesValidatorAssertion assertion = new NcesValidatorAssertion();
        assertion.setCertificateValidationType( CertificateValidationType.PATH_VALIDATION );
        assertion.setSamlRequired( true );
        assertion.setTrustedCertificateInfo( new CertificateInfo[]{
                new CertificateInfo( TestDocuments.getWssInteropAliceCert() )
        } );

        final CertValidationProcessor certValidationProcessor = new TestCertValidationProcessor();
        final SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver();
        final TrustedCertServices trustedCertServices = getTrustedCertServices();

        // Test failure
        {
            ServerNcesValidatorAssertion ncesValidator =
                    new ServerNcesValidatorAssertion( assertion, certValidationProcessor, securityTokenResolver, trustedCertServices, true  );

            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(XmlUtil.parse(REQUEST_COMBINED_SIGNATURE)), new Message() );
            AssertionStatus status = ncesValidator.checkRequest( pec );
            Assert.assertEquals( "Fails with BAD_REQUEST", AssertionStatus.BAD_REQUEST, status );
        }

        // Test success (single signature check disabled)
        {
            ServerNcesValidatorAssertion ncesValidator =
                    new ServerNcesValidatorAssertion( assertion, certValidationProcessor, securityTokenResolver, trustedCertServices, false  );

            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(XmlUtil.parse(REQUEST_COMBINED_SIGNATURE)), new Message() );
            AssertionStatus status = ncesValidator.checkRequest( pec );
            Assert.assertEquals( "Success", AssertionStatus.NONE, status );
        }
    }

    private TrustedCertServices getTrustedCertServices() {
        return new TrustedCertServices(){
            @Override
            public void checkSslTrust(X509Certificate[] serverCertChain, Set<Goid> requiredOids) throws CertificateException {
            }
            @Override
            public Collection<TrustedCert> getCertsBySubjectDnFiltered(String subjectDn,
                                                                       boolean omitExpired,
                                                                       Set<TrustedCert.TrustedFor> requiredTrustFlags,
                                                                       Set<Goid> requiredOids) throws FindException {
                return Collections.emptyList();
            }
            @Override
            public Collection<TrustedCert> getAllCertsByTrustFlags(Set<TrustedCert.TrustedFor> requiredTrustFlags) throws FindException {
                return Collections.emptyList();
            }
        };
    }

    private static final String REQUEST_COMBINED_SIGNATURE =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "<soapenv:Header>\n" +
                    "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soapenv:mustUnderstand=\"1\">\n" +
                    "<wsu:Timestamp wsu:Id=\"Timestamp-2-2453144e029ac3cb6ec66caa6a5af53a\"><wsu:Created>2009-06-05T23:20:44.328020959Z</wsu:Created><wsu:Expires>2009-06-05T23:25:44.328Z</wsu:Expires></wsu:Timestamp>\n" +
                    "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"SamlAssertion-0191feb8e346b9c9ac7df0a639421742\" IssueInstant=\"2009-06-05T23:20:44.315Z\" Version=\"2.0\"><saml2:Issuer>localhost.localdomain</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">test</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\"><saml2:NameID>CN=localhost.localdomain</saml2:NameID></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2009-06-05T23:18:00.000Z\" NotOnOrAfter=\"2009-06-05T23:25:44.316Z\"/><saml2:AuthnStatement AuthnInstant=\"2009-06-05T23:20:44.315Z\"><saml2:SubjectLocality Address=\"127.0.0.1\" DNSName=\"localhost.localdomain\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>\n" +
                    "<wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-75fad806afeb70f8dff07c77b1ebb44d\">MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==</wsse:BinarySecurityToken>\n" +
                    "<wsse:SecurityTokenReference wsu:Id=\"SamlSTR-1-e4258a8c6dd4bcc87cb27939e6362ff3\"><wsse:KeyIdentifier ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID\">SamlAssertion-0191feb8e346b9c9ac7df0a639421742</wsse:KeyIdentifier></wsse:SecurityTokenReference>\n" +
                    "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#SamlSTR-1-e4258a8c6dd4bcc87cb27939e6362ff3\"><ds:Transforms><ds:Transform Algorithm=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform\"><wsse:TransformationParameters><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></wsse:TransformationParameters></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>mSVlhPwRQCj69aZw/c2hUopUr58=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>Yyv7wDpQR7iqt/zh5pYyrzTsZFm0GLSJItl6su41sdawLWTRtuaVQcCrmi1DNYsxP+uekY5c90E65G7e2UN7m8vNCMO7pSKGOJ1ZRPYyfaS48lEbD8r2owfMsW0kKikWJyrJBH4Z8cpyrvjcpiAH8T+Ok2T4M7mTrzKRwo9ErF0=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#BinarySecurityToken-0-75fad806afeb70f8dff07c77b1ebb44d\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature>\n" +
                    "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#Timestamp-2-2453144e029ac3cb6ec66caa6a5af53a\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>9G1gdmG52/pGtFbiCiZ1htVr7jQ=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#MessageID-1-558c70d819db471b82a11e6866f8ee05\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ekCMWt6noo2SDoKaecviWlj4caY=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#Body-1-273efa19c3b253759454d71fdfda7786\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>cCA06j51sIHhqGXToGVfAcjyuKQ=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>XXAaoLLCe8Is0xH0X7tMN0ju0Zr4M9P2DS2Lx50xRmD75EcWzo6UOBUYzChy1NosROBWmE9FJAbki20Knh//cCoebX5xPQ2O/eU0S4rd33Jz/kc/Ds4YsA3MP0++d8OEZYrKmcS5PYgzNca1zgt/AcFNToK1dhysFVh5hBoXVHs=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#BinarySecurityToken-0-75fad806afeb70f8dff07c77b1ebb44d\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security>\n" +
                    "<wsa:MessageID xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"MessageID-1-558c70d819db471b82a11e6866f8ee05\">bc279e4f-6926-4a74-b838-72ddaca5d279</wsa:MessageID>\n" +
                    "</soapenv:Header><soapenv:Body xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"Body-1-273efa19c3b253759454d71fdfda7786\"><tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\"><tns:delay>0</tns:delay></tns:listProducts></soapenv:Body></soapenv:Envelope>";
}
