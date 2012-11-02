package com.l7tech.external.assertions.validatenonsoapsaml.server;

import static org.junit.Assert.*;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlTokenAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.saml.*;
import com.l7tech.security.token.http.HttpClientCertToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.xmlsec.SamlTestUtil;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Test the ValidateNonSoapSamlAssertion
 */
public class ServerValidateNonSoapSamlTokenAssertionTest {

    private static final Logger log = Logger.getLogger(ServerValidateNonSoapSamlTokenAssertionTest.class.getName());

    public static PolicyEnforcementContext getContext() throws Exception {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        policyEnforcementContext.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        return policyEnforcementContext;
    }

    @Test
    public void test_BearerToken_WithSignature() throws Exception {
        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_BEARER});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(XmlUtil.parse(SAML_V2_BEARER_SIGNED_ASSERTION)));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void test_BearerToken_WithSignature_AssertionModified() throws Exception {
        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_BEARER});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(XmlUtil.parse(SAML_V2_BEARER_SIGNED_MODIFIED_ASSERTION)));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.REQUIRE_WSS_SIGNATURE_CONFIRMATION_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("Unable to verify signature of SAML assertion: Validity not achieved."));
    }

    @Test
    public void test_BearerToken_WithSignature_SignatureDigestModified() throws Exception {
        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_BEARER});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(XmlUtil.parse(SAML_V2_BEARER_SIGNED_ASSERTION_INVALID_DIGEST)));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.REQUIRE_WSS_SIGNATURE_CONFIRMATION_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("SignatureValue mismatched"));
    }

    @Test
    public void test_BearerToken_NotSigned() throws Exception {
        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setRequireDigitalSignature(false);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_BEARER});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        SamlTestUtil.configureServerAssertionInjects(serverAssertion);
        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(XmlUtil.parse(SAML_V2_BEARER_NOT_SIGNED_ASSERTION)));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void test_BearerToken_Signaure_Required_Not_Provided() throws Exception {
        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setRequireDigitalSignature(true);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_BEARER});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(XmlUtil.parse(SAML_V2_BEARER_NOT_SIGNED_ASSERTION)));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_STMT_VALIDATE_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("Unsigned SAML assertion found"));
    }

    @Test
    public void testSenderVouches_IncorrectIncomingConfirmationMethod() throws Exception {
        final Document samlAssertion = SamlTestUtil.createSamlAssertion(true, true, SamlConstants.CONFIRMATION_SAML2_BEARER);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(samlAssertion));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_STMT_VALIDATE_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("Subject Confirmations mismatch presented/accepted [urn:oasis:names:tc:SAML:2.0:cm:bearer]/[urn:oasis:names:tc:SAML:2.0:cm:sender-vouches]"));
    }

    @Test
    public void testSenderVouches_ClientCertIsRequired() throws Exception {
        final Document samlAssertion = SamlTestUtil.createSamlAssertion(true, true, SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(samlAssertion));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_STMT_VALIDATE_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("SSL Client Certificate is required for Sender-Vouches Assertion and unsigned message"));
    }

    @Test
    public void testSenderVouches_NotSigned_ClientCertIsRequired() throws Exception {
        final Document samlAssertion = SamlTestUtil.createSamlAssertion(true, false, SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES});
        assertion.setRequireDigitalSignature(false);

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("samlAssertion", new Message(samlAssertion));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_STMT_VALIDATE_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("SSL Client Certificate is required for Sender-Vouches Assertion and unsigned message"));
    }

    @Test
    public void testSenderVouches_Signed_ClientCertAvailable() throws Exception {
        final Document samlAssertion = SamlTestUtil.createSamlAssertion(true, true, SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        final Message message = new Message(samlAssertion);
        final AuthenticationContext authContext = context.getAuthenticationContext(message);
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(TestDocuments.getDotNetServerCertificate()), SslAssertion.class);
        authContext.addCredentials(creds);

        context.setVariable("samlAssertion", message);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testHolderOfKey_Success() throws Exception {
        final Document samlAssertion = createSamlAssertionWithSubjectCert(true, true, SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        final Message message = new Message(samlAssertion);
        final AuthenticationContext authContext = context.getAuthenticationContext(message);
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(TestDocuments.getDotNetServerCertificate()), SslAssertion.class);
        authContext.addCredentials(creds);

        context.setVariable("samlAssertion", message);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testHolderOfKey_SSLCert_Mismatch_To_SubjectCert() throws Exception {
        final Document samlAssertion = createSamlAssertionWithSubjectCert(true, true, SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("samlAssertion");
        assertion.setVersion(2);
        assertion.setCheckAssertionValidity(false);
        assertion.setNameFormats(new String[]{SamlConstants.NAMEIDENTIFIER_UNSPECIFIED});
        assertion.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY});

        ServerValidateNonSoapSamlTokenAssertion serverAssertion = new ServerValidateNonSoapSamlTokenAssertion(assertion);
        final TestAudit testAudit = SamlTestUtil.configureServerAssertionInjects(serverAssertion);

        final PolicyEnforcementContext context = getContext();
        final Message message = new Message(samlAssertion);
        final AuthenticationContext authContext = context.getAuthenticationContext(message);
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(TestDocuments.getEttkClientCertificate()), SslAssertion.class);
        authContext.addCredentials(creds);

        context.setVariable("samlAssertion", message);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_STMT_VALIDATE_FAILED));
        assertTrue(testAudit.isAuditPresentContaining("SSL Certificate and Holder-Of-Key Subject Certificate mismatch"));
    }

    private Document createSamlAssertionWithSubjectCert(boolean version2, boolean signAssertion, String confirmationMethod) throws Exception{
        PrivateKey privateKey = TestDocuments.getDotNetServerPrivateKey();
        SamlAssertionGenerator sag = new SamlAssertionGenerator(new SignerInfo(privateKey,
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }));

        LoginCredentials creds = LoginCredentials.makeLoginCredentials(new HttpClientCertToken(TestDocuments.getDotNetServerCertificate()), SslAssertion.class);

        final Attribute[] attributes = {
                new Attribute("First Attr 32", "urn:me", "test value foo blah blartch"),
                new Attribute("2 Attribute: it is indeed!", "urn:me", "value for myotherattr blah"),
                new Attribute("moreattr", "urn:me", "value for moreattr blah"),
                new Attribute("multivalattr", "urn:mv1", "value one!"),
                new Attribute("multivalattr", "urn:mv1", "value two!")
        };

        AttributeStatement st = new AttributeStatement(creds, SubjectStatement.Confirmation.forUri(confirmationMethod), attributes, KeyInfoInclusionType.CERT,
                NameIdentifierInclusionType.FROM_CREDS, null, null, null );

        SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        options.setVersion(version2 ? 2 : 1);
        options.setSignAssertion(signAssertion);
        return sag.createAssertion(st, options);
    }

    private final String SAML_V2_BEARER_SIGNED_ASSERTION = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\" IssueInstant=\"2012-08-27T22:48:47.946Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer NameQualifier=\"Name Qualifier\">da70f2.l7tech.com</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ieLzx8zmUR92xmY6N5wSvKX/T2U=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>EoHq6juiYIrUZp5mg0f5lH3SjSrfReiZJ78sPZ0lN/rcdbSs8X5utUb9hK4LF6RZXeU16MG8f5RWJoBRyFRL13Vmj5jbjCaWRCNMHdcZB50iCFK9lzBYyo1KaFa2hflMkOO5zg34aFvRlQB6UpxPUXUy1yjFlHFZpQ9Gi4LiDX2F+UuflFL4WK47bHanovv/79K4BMA541BNcEUqQn/GU87kRrCmRqdx0wTw4+mOkxyXQHbuWsH+tp4LMNh2ErxgkKW5tu+k8zr1+OrtniD7CL6I7DDXJdUs6BjHSjSbwasgM7z6ppnndEVioRlvtAo7wXtmeaRM17SPNJgDIMGx0Q==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=da70f2.l7tech.com</X509SubjectName><X509Certificate>MIIC/TCCAeWgAwIBAgIJAKjZJ8uQHN1oMA0GCSqGSIb3DQEBDAUAMBwxGjAYBgNVBAMTEWRhNzBmMi5sN3RlY2guY29tMB4XDTEyMDgxNDE3MjQ0MloXDTIyMDgxMjE3MjQ0MlowHDEaMBgGA1UEAxMRZGE3MGYyLmw3dGVjaC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDA6UstJqkyP+x+pT9N0pzDqF1n+hUM3OMWaYgjDfomjRJ8XmAz9RUCQ7gcaPhAfyiC7+D3WM37n+ygW8SboEsD/mQrpafUgBrkPofTDsvDfgdl41y6fvmAlRBl9Ybp6J9aiRC9D3CFCaqKdD2ABKYOWoe8tMP+oADWS6DwmlYG9Hg2FNGeotAvNkDgv6zNieL9cQ6lAR7oIOvQfHwa0oWL0kr7JfMJen0dE0vTJz/TpWkxvMaGhB5drwc0aWIrLxhYoFdtBri1wiC+2jtCp0paFLoWzbP/D6Wq5PO/CF5VPBeZjmhFep1vYmoQp9k3bU6n8aQSCLoZXYNIno6cFURVAgMBAAGjQjBAMB0GA1UdDgQWBBRmt5nXfvZwazTJitglJfQzjGDXlzAfBgNVHSMEGDAWgBRmt5nXfvZwazTJitglJfQzjGDXlzANBgkqhkiG9w0BAQwFAAOCAQEAfV5sN3w054GukVpCZB8dQvK17X3/a1CCuBHoJpEn8E5bFQJgMrOXBIGgjTbDk+xe7WfV6n75RKpoU1ttpqmXCbOVNQNL0ad4CS5x0nX08UjWLwocw1Z/+oGhjoq8Gsg+KTIkh8xBNInTi3NPUwrD4E6m9K6B7SLmZfJhtWxVauARgsIOHNCyHi0p6m2FQcq33/kIcp2OQEdTCApS0+ExmqtfmYaEbqSMF3TlMutLsPee2YikT0DINNOwylmXz0P2+pIOmGsXyMaPwlmSii7dhffRUILphdQP+XbXbNYfcwFr6p/Ncd6oLlc9dORZCCpQQolcn4a11RJE1apj7i6NRg==</X509Certificate></X509Data></KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"Name Identifier Qualifier\">Name Identifier Value</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Address=\"http://address\" InResponseTo=\"http://inresponseto\" Recipient=\"http://recipient\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2012-08-27T22:46:47.946Z\" NotOnOrAfter=\"2012-08-27T22:53:47.946Z\"/><saml2:AuthnStatement AuthnInstant=\"2012-08-27T22:48:47.946Z\"><saml2:SubjectLocality Address=\"10.7.48.234\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";
    /**
     * The issue instant has been modified => the signature is now invalid.
     */
    private final String SAML_V2_BEARER_SIGNED_MODIFIED_ASSERTION = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\" IssueInstant=\"2012-08-27T20:48:47.946Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer NameQualifier=\"Name Qualifier\">da70f2.l7tech.com</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ieLzx8zmUR92xmY6N5wSvKX/T2U=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>EoHq6juiYIrUZp5mg0f5lH3SjSrfReiZJ78sPZ0lN/rcdbSs8X5utUb9hK4LF6RZXeU16MG8f5RWJoBRyFRL13Vmj5jbjCaWRCNMHdcZB50iCFK9lzBYyo1KaFa2hflMkOO5zg34aFvRlQB6UpxPUXUy1yjFlHFZpQ9Gi4LiDX2F+UuflFL4WK47bHanovv/79K4BMA541BNcEUqQn/GU87kRrCmRqdx0wTw4+mOkxyXQHbuWsH+tp4LMNh2ErxgkKW5tu+k8zr1+OrtniD7CL6I7DDXJdUs6BjHSjSbwasgM7z6ppnndEVioRlvtAo7wXtmeaRM17SPNJgDIMGx0Q==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=da70f2.l7tech.com</X509SubjectName><X509Certificate>MIIC/TCCAeWgAwIBAgIJAKjZJ8uQHN1oMA0GCSqGSIb3DQEBDAUAMBwxGjAYBgNVBAMTEWRhNzBmMi5sN3RlY2guY29tMB4XDTEyMDgxNDE3MjQ0MloXDTIyMDgxMjE3MjQ0MlowHDEaMBgGA1UEAxMRZGE3MGYyLmw3dGVjaC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDA6UstJqkyP+x+pT9N0pzDqF1n+hUM3OMWaYgjDfomjRJ8XmAz9RUCQ7gcaPhAfyiC7+D3WM37n+ygW8SboEsD/mQrpafUgBrkPofTDsvDfgdl41y6fvmAlRBl9Ybp6J9aiRC9D3CFCaqKdD2ABKYOWoe8tMP+oADWS6DwmlYG9Hg2FNGeotAvNkDgv6zNieL9cQ6lAR7oIOvQfHwa0oWL0kr7JfMJen0dE0vTJz/TpWkxvMaGhB5drwc0aWIrLxhYoFdtBri1wiC+2jtCp0paFLoWzbP/D6Wq5PO/CF5VPBeZjmhFep1vYmoQp9k3bU6n8aQSCLoZXYNIno6cFURVAgMBAAGjQjBAMB0GA1UdDgQWBBRmt5nXfvZwazTJitglJfQzjGDXlzAfBgNVHSMEGDAWgBRmt5nXfvZwazTJitglJfQzjGDXlzANBgkqhkiG9w0BAQwFAAOCAQEAfV5sN3w054GukVpCZB8dQvK17X3/a1CCuBHoJpEn8E5bFQJgMrOXBIGgjTbDk+xe7WfV6n75RKpoU1ttpqmXCbOVNQNL0ad4CS5x0nX08UjWLwocw1Z/+oGhjoq8Gsg+KTIkh8xBNInTi3NPUwrD4E6m9K6B7SLmZfJhtWxVauARgsIOHNCyHi0p6m2FQcq33/kIcp2OQEdTCApS0+ExmqtfmYaEbqSMF3TlMutLsPee2YikT0DINNOwylmXz0P2+pIOmGsXyMaPwlmSii7dhffRUILphdQP+XbXbNYfcwFr6p/Ncd6oLlc9dORZCCpQQolcn4a11RJE1apj7i6NRg==</X509Certificate></X509Data></KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"Name Identifier Qualifier\">Name Identifier Value</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Address=\"http://address\" InResponseTo=\"http://inresponseto\" Recipient=\"http://recipient\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2012-08-27T22:46:47.946Z\" NotOnOrAfter=\"2012-08-27T22:53:47.946Z\"/><saml2:AuthnStatement AuthnInstant=\"2012-08-27T22:48:47.946Z\"><saml2:SubjectLocality Address=\"10.7.48.234\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    /**
     * Digest has been modified.
     */
    private final String SAML_V2_BEARER_SIGNED_ASSERTION_INVALID_DIGEST = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\" IssueInstant=\"2012-08-27T22:48:47.946Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer NameQualifier=\"Name Qualifier\">da70f2.l7tech.com</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#SamlAssertion-37a2d4a79fc1225f34f6d8b6deec890b\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ieLzx8zmURHACKY6N5wSvKX/T2U=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>EoHq6juiYIrUZp5mg0f5lH3SjSrfReiZJ78sPZ0lN/rcdbSs8X5utUb9hK4LF6RZXeU16MG8f5RWJoBRyFRL13Vmj5jbjCaWRCNMHdcZB50iCFK9lzBYyo1KaFa2hflMkOO5zg34aFvRlQB6UpxPUXUy1yjFlHFZpQ9Gi4LiDX2F+UuflFL4WK47bHanovv/79K4BMA541BNcEUqQn/GU87kRrCmRqdx0wTw4+mOkxyXQHbuWsH+tp4LMNh2ErxgkKW5tu+k8zr1+OrtniD7CL6I7DDXJdUs6BjHSjSbwasgM7z6ppnndEVioRlvtAo7wXtmeaRM17SPNJgDIMGx0Q==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=da70f2.l7tech.com</X509SubjectName><X509Certificate>MIIC/TCCAeWgAwIBAgIJAKjZJ8uQHN1oMA0GCSqGSIb3DQEBDAUAMBwxGjAYBgNVBAMTEWRhNzBmMi5sN3RlY2guY29tMB4XDTEyMDgxNDE3MjQ0MloXDTIyMDgxMjE3MjQ0MlowHDEaMBgGA1UEAxMRZGE3MGYyLmw3dGVjaC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDA6UstJqkyP+x+pT9N0pzDqF1n+hUM3OMWaYgjDfomjRJ8XmAz9RUCQ7gcaPhAfyiC7+D3WM37n+ygW8SboEsD/mQrpafUgBrkPofTDsvDfgdl41y6fvmAlRBl9Ybp6J9aiRC9D3CFCaqKdD2ABKYOWoe8tMP+oADWS6DwmlYG9Hg2FNGeotAvNkDgv6zNieL9cQ6lAR7oIOvQfHwa0oWL0kr7JfMJen0dE0vTJz/TpWkxvMaGhB5drwc0aWIrLxhYoFdtBri1wiC+2jtCp0paFLoWzbP/D6Wq5PO/CF5VPBeZjmhFep1vYmoQp9k3bU6n8aQSCLoZXYNIno6cFURVAgMBAAGjQjBAMB0GA1UdDgQWBBRmt5nXfvZwazTJitglJfQzjGDXlzAfBgNVHSMEGDAWgBRmt5nXfvZwazTJitglJfQzjGDXlzANBgkqhkiG9w0BAQwFAAOCAQEAfV5sN3w054GukVpCZB8dQvK17X3/a1CCuBHoJpEn8E5bFQJgMrOXBIGgjTbDk+xe7WfV6n75RKpoU1ttpqmXCbOVNQNL0ad4CS5x0nX08UjWLwocw1Z/+oGhjoq8Gsg+KTIkh8xBNInTi3NPUwrD4E6m9K6B7SLmZfJhtWxVauARgsIOHNCyHi0p6m2FQcq33/kIcp2OQEdTCApS0+ExmqtfmYaEbqSMF3TlMutLsPee2YikT0DINNOwylmXz0P2+pIOmGsXyMaPwlmSii7dhffRUILphdQP+XbXbNYfcwFr6p/Ncd6oLlc9dORZCCpQQolcn4a11RJE1apj7i6NRg==</X509Certificate></X509Data></KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"Name Identifier Qualifier\">Name Identifier Value</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Address=\"http://address\" InResponseTo=\"http://inresponseto\" Recipient=\"http://recipient\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2012-08-27T22:46:47.946Z\" NotOnOrAfter=\"2012-08-27T22:53:47.946Z\"/><saml2:AuthnStatement AuthnInstant=\"2012-08-27T22:48:47.946Z\"><saml2:SubjectLocality Address=\"10.7.48.234\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";
    private final String SAML_V2_BEARER_NOT_SIGNED_ASSERTION = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-f738662ad68e4742a2d58218763e9c01\" IssueInstant=\"2012-08-27T23:53:37.991Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer NameQualifier=\"Name Qualifier\">da70f2.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"Name Identifier Qualifier\">Name Identifier Value</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Address=\"http://address\" InResponseTo=\"http://inresponseto\" Recipient=\"http://recipient\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2012-08-27T23:51:37.991Z\" NotOnOrAfter=\"2012-08-27T23:58:37.991Z\"/><saml2:AuthnStatement AuthnInstant=\"2012-08-27T23:53:37.990Z\"><saml2:SubjectLocality Address=\"10.7.48.234\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";
}
