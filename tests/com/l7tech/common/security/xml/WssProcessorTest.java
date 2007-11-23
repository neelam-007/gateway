/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SignedSamlTest;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecorationRequirements.SimpleSecureConversationSession;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.server.secureconversation.SecureConversationSession;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.activeio.util.ByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssProcessorTest extends TestCase {
    private static Logger log = Logger.getLogger(WssProcessorTest.class.getName());
    private static final Random random = new Random();

    public WssProcessorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssProcessorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private void doTest(TestDocument testDocument) throws Exception {
        WssProcessor wssProcessor = new WssProcessorImpl();
        Document request = testDocument.document;
        X509Certificate recipientCertificate = testDocument.recipientCertificate;
        PrivateKey recipientPrivateKey = testDocument.recipientPrivateKey;
        SecurityTokenResolver securityTokenResolver = testDocument.securityTokenResolver;


        log.info("Testing document: " + testDocument.name);
        log.info("Original decorated message (reformatted): " + XmlUtil.nodeToFormattedString(request));
        ProcessorResult result = wssProcessor.undecorateMessage(new Message(request),
                                                                testDocument.senderCeritifcate,
                                                                testDocument.securityContextFinder,
                                                                new WrapSSTR(recipientCertificate,
                                                                             recipientPrivateKey,
                                                                             securityTokenResolver));
        checkProcessorResult(request, result);
    }

    private void checkProcessorResult(Document request, ProcessorResult result) throws IOException {
        assertTrue(result != null);

        ParsedElement[] encrypted = result.getElementsThatWereEncrypted();
        assertTrue(encrypted != null);
        if (encrypted.length > 0) {
            log.info("The following elements were encrypted:");
            for (int j = 0; j < encrypted.length; j++) {
                Element element = encrypted[j].asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were encrypted.");

        SignedElement[] signed = result.getElementsThatWereSigned();
        assertTrue(signed != null);
        if (signed.length > 0) {
            log.info("The following elements were signed:");
            for (int j = 0; j < signed.length; j++) {
                Element element = signed[j].asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were signed.");


        XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
        assertTrue(tokens != null);
        if (tokens.length > 0) {
            log.info("The following security tokens were found:");
            for (int j = 0; j < tokens.length; j++) {
                SecurityToken token = tokens[j];
                if (token instanceof SamlSecurityToken) {
                    log.info("Possession proved: " + ((SamlSecurityToken)token).isPossessionProved());
                    log.info("  " + ((SamlSecurityToken)token).getSubjectCertificate());
                } else if (token instanceof X509SecurityToken) {
                    log.info("Possession proved: " + ((X509SecurityToken)token).isPossessionProved());
                    log.info("  " + token);
                } else {
                    log.info("  " + token);
                }
            }
        } else
            log.info("No security tokens were found.");

        WssTimestamp timestamp = result.getTimestamp();
        if (timestamp != null) {
            log.info("Timestamp created = " + timestamp.getCreated().asDate());
            log.info("Timestamp expires = " + timestamp.getExpires().asDate());
        } else
            log.info("No timestamp was found.");

        log.info("Undecorated document:\n" + XmlUtil.nodeToFormattedString(request));
        log.info("Security namespace observed:\n" + result.getSecurityNS());
        log.info("WSU namespace observed:\n" + result.getWSUNS());
    }

    public static class TestDocument {
        public String name;
        public Document document;
        public X509Certificate senderCeritifcate;
        public PrivateKey recipientPrivateKey;
        public X509Certificate recipientCertificate;
        public SecurityContextFinder securityContextFinder = null;
        public SecurityTokenResolver securityTokenResolver;

        TestDocument(String n, Document d, PrivateKey rpk, X509Certificate rc,
                     SecurityContextFinder securityContextFinder, X509Certificate senderCert,
                     SecurityTokenResolver securityTokenResolver)
        {
            this.name = n;
            this.document = d;
            this.recipientPrivateKey = rpk;
            this.recipientCertificate = rc;
            this.securityContextFinder = securityContextFinder;
            this.senderCeritifcate = senderCert;
            this.securityTokenResolver = securityTokenResolver;
        }
    }

    public void testDotnetEncryptedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet encrypted request", TestDocuments.DOTNET_ENCRYPTED_REQUEST));
    }

    public void testDotnetSignedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request", TestDocuments.DOTNET_SIGNED_REQUEST));
    }

    public void testDotnetRequestWithUsernameToken() throws Exception {
        doTest(makeDotNetTestDocument("dotnet request with username token", TestDocuments.DOTNET_USERNAME_TOKEN));
    }

    public void testEttkSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed request", TestDocuments.ETTK_SIGNED_REQUEST));
    }

    public void testEttkEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk encrypted request", TestDocuments.ETTK_ENCRYPTED_REQUEST));
    }

    public void testEttkSignedEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed encrypted request", TestDocuments.ETTK_SIGNED_ENCRYPTED_REQUEST));
    }

    /*public void testRequestWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request wrapped l7 actor", TestDocuments.WRAPED_L7ACTOR));
    }

    public void testRequestMultipleWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request multiple wrapped l7 actor", TestDocuments.MULTIPLE_WRAPED_L7ACTOR));
    }*/

    public void testDotnetSignedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed SecureConversation request", TestDocuments.DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN));
    }

    public void testDotnetSignedEncryptedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed encrypted SecureConversation request", TestDocuments.DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN));
    }

    public void testDotnetSignedRequest2() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request 2", TestDocuments.DOTNET_SIGNED_REQUEST2));
    }

    public void testWebsphereSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("websphere signed request", TestDocuments.WEBSPHERE_SIGNED_REQUEST));
    }

    public void testSampleSignedSamlHolderOfKeyRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest("blah");
        sst.setUp();
        doTest(makeEttkTestDocument("sample signed SAML holder-of-key request",
                                    /*TestDocuments.SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST*/sst.getRequestSignedWithSamlToken(false, false, false, 1)));
    }

    public void testSignedSamlSenderVouchesRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest("blah");
        sst.setUp();
        doTest(makeEttkTestDocument("Signed SAML sender-vouches request",
                                    sst.getSignedRequestWithSenderVouchesToken()));
    }

    public void testNonSoapRequest() throws Exception {
        try {
            doTest(makeEttkTestDocument("non-SOAP request",
                                        TestDocuments.NON_SOAP_REQUEST));
            fail("Expected MessageNotSoapException was not thrown");
        } catch (MessageNotSoapException e) {
            // Ok
        }
    }

    public void testBug3736StrTransform() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3736_STR_TRANSFORM_REQUEST);

            result = new TestDocument("Bug3736StrTransform", d,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
        } catch (Exception e) {
            throw e;
        }

        doTest(result);
    }

    public void testBug3611SignatureInclusiveNamespaces() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3611_SIGNATURE_INCLUSIVE_NAMESPACES);

            result = new TestDocument("Bug3611SignatureInclusiveNamespaces", d,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
        } catch (Exception e) {
            throw e;
        }

        doTest(result);
    }

    /**
     * Test that use of unsupported XPath transform causes reasonable error (not java.lang.NoSuchMethodError). 
     */
    public void testBug3747DsigXpath() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3747_DSIG_XPATH);

            result = new TestDocument("Bug3747DsigXpath", d,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
        } catch (Exception e) {
            throw e;
        }

        try {
            doTest(result);
        } catch (InvalidDocumentFormatException idfe) {
            // Expected failure
        }
    }

    public void testWssInterop2005JulyRequest() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_REQUEST);

            result = new TestDocument("WssInterop2005JulyRequest", d,
                                                TestDocuments.getWssInteropBobKey(),
                                                TestDocuments.getWssInteropBobCert(),
                                                null,
                                                TestDocuments.getWssInteropAliceCert(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        doTest(result);
    }

    public void testKeyInfoThumbprintRequest() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "/keyinfothumbreq.xml");

            SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getWssInteropAliceCert());
            result = new TestDocument("KeyInfoThumbprintRequest",
                                      d,
                                      null,
                                      null,
                                      null,
                                      null,
                                      securityTokenResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        doTest(result);
    }

    // disabled because the cert that signed the test message has since expired
    // TODO recreate test messsage using another cert
    public void DISABLED_testSignedSvAssertionWithThumbprintSha1() throws Exception {
        TestDocument r;
        Document ass = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedSvThumbAssertion.xml");
        Document d = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element security = SoapUtil.getOrMakeSecurityElement(d);
        security.appendChild(d.importNode(ass.getDocumentElement(), true));
        SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate());
        r = new TestDocument("SignedSvAssertionWithThumbprintSha1",
                             d,
                             null,
                             null,
                             null,
                             null,
                             securityTokenResolver);
        doTest(r);
    }

    // Disabled because the cert that signed the test request has since expired
    // TODO recreate test messsage using another cert
    public void DISABLED_testCompleteEggRequest() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/ValidBlueCardRequest.xml");

        Element sec = SoapUtil.getSecurityElement(d);
        Element ass = XmlUtil.findFirstChildElementByName(sec, SamlConstants.NS_SAML, "Assertion");

        Document assDoc = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedAttrThumbAssertion.xml");
        sec.replaceChild(d.importNode(assDoc.getDocumentElement(), true), ass);

        //XmlUtil.nodeToOutputStream(d, new FileOutputStream("c:/eggerequest.xml"));

        SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate());
        TestDocument td = new TestDocument("CompleteEggRequest",
                                           d,
                                           null,
                                           null,
                                           null,
                                           null,
                                           securityTokenResolver);
        doTest(td);
    }

    public void testWssInterop2005JulyResponse() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_RESPONSE);

        result = new TestDocument("WssInterop2005JulyResponse", d,
                                  TestDocuments.getWssInteropAliceKey(),
                                  TestDocuments.getWssInteropAliceCert(),
                                  null,
                                  TestDocuments.getWssInteropBobCert(),
                                  null);
        doTest(result);
    }

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(),
                                    null,
                                    null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestDocument makeEttkTestDocument(String testname, Document doc) {
        try {
            return new TestDocument(testname, doc,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(),
                                    null,
                                    null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDocument makeDotNetTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            final SecureConversationSession session = new SecureConversationSession();

            // Set up a fake ws-sc session, in case this example will be needing it
            session.setSharedSecret(TestDocuments.getDotNetSecureConversationSharedSecret());
            SecurityContextFinder dotNetSecurityContextFinder = new SecurityContextFinder() {
                public SecurityContext getSecurityContext(String securityContextIdentifier) {
                    return session;
                }
            };

            return new TestDocument(testname, d,
                                    TestDocuments.getDotNetServerPrivateKey(),
                                    TestDocuments.getDotNetServerCertificate(),
                                    dotNetSecurityContextFinder,
                                    null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static final String INDIGO_PING_RESPONSE = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><s:Header><ActivityId xmlns=\"http://schemas.microsoft.com/2004/09/ServiceModel/Diagnostics\">78f13fbd-8484-4ae4-b3e3-185b52f9608b</ActivityId><o:Security xmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" s:mustUnderstand=\"1\"><u:Timestamp u:Id=\"uuid-f7c80253-34a1-4b59-acf2-27240cc2fb4e-12\"><u:Created>2005-11-08T17:34:37.508Z</u:Created><u:Expires>2005-11-08T17:39:37.508Z</u:Expires></u:Timestamp><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></CanonicalizationMethod><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></SignatureMethod><Reference URI=\"#_0\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></Transform></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod><DigestValue>gVc0sw3s6TrTsaGKKLQxYp/X+jY=</DigestValue></Reference><Reference URI=\"#uuid-f7c80253-34a1-4b59-acf2-27240cc2fb4e-12\"><Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></Transform></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></DigestMethod><DigestValue>HBFdY75N1LYFI301jYhlAAoEUSU=</DigestValue></Reference></SignedInfo><SignatureValue>pW/nCS3BchGqMSwQA/0VzyhjeAZem9AqlgjW3xMuHiwr3ZP0GxmXiMV5g/S6kd1itPs1d5OV6alqQGKa1SN1vh23SRenpdaUWrUg0oQgpIOrHuiP08DqNwXegJC2xIzRoXLW+wSKbiBC6zsRSNYVveo0+yE/+FzagbTzz9iN/0E=</SignatureValue><KeyInfo><o:SecurityTokenReference><o:KeyIdentifier EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">Xeg55vRyK3ZhAEhEf+YT0z986L0=</o:KeyIdentifier></o:SecurityTokenReference></KeyInfo></Signature></o:Security></s:Header><s:Body u:Id=\"_0\"><PingResponse xmlns=\"http://xmlsoap.org/Ping\">Ping</PingResponse></s:Body></s:Envelope>";
    public static final String INDIGO_CERT = "MIIDDDCCAfSgAwIBAgIQb6U6bec4ZHW96T5N2A/NdTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQK\n" +
            "DAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MTAyNzAwMDAwMFoX\n" +
            "DTE4MTAyNzIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsTF09BU0lTIEludGVyb3Ag\n" +
            "VGVzdCBDZXJ0MQ4wDAYDVQQDDAVXc3NJUDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA2X9Z\n" +
            "Wiek/59vvg+l/lmzWjBYiqoOuSI+ms3ief7RyhPNh/IrGE3VwU67HsygNeavE06S6xNfcNWUNLqE\n" +
            "dRmd/29WnubNH7hWJsqp7rn8g/mxNVkynCkJ1saKuD8ILiKfNg0e8UUE9QzwEz1fxw81OR0SbDit\n" +
            "fTrDj8Q/ouCgEaUCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8v\n" +
            "aW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQb\n" +
            "1AYE+P8ue/8qbgUJOKoyDXFqaTAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkq\n" +
            "hkiG9w0BAQUFAAOCAQEAeltzyUHj+/0i3Hsj5XvWrJ7mF+zBFwp7E6CPLP/urfMdl1VFaBttOCcd\n" +
            "WRrm8GI3KsGQMV6dpzAykl1JDO7T6IMSMYA1/YTsSH9S8xoubL/7IGYj3izKZ9LrV7fJJOHOerKL\n" +
            "gIk/0X8DzH15jwel271s6Nh6DiXqU2Hf0YUmauLAH+rbiuNLlUKM5UkP4BtGqPw+6tvyaUOa3fzJ\n" +
            "s92WB+j5x91/xmvNg+ZTp+TEfyINM3wZAHwoIzXtEViopCRsXkmLr+IBGszmUpZnPd2QuqDSSkQh\n" +
            "lZmUAuNVPCTBoNuWBX/tvvAw3a3jl+DXB+Fn2JbRpoUdvkgAWCAJ6hrKgA==";

    public void testIndigoPingResponse() throws Exception {

        X509Certificate aliceCert = TestDocuments.getWssInteropAliceCert();
        X509Certificate bobCert = TestDocuments.getWssInteropBobCert();
        byte[] icbytes = HexUtils.decodeBase64(INDIGO_CERT);
        X509Certificate indigoCert = CertUtils.decodeCert(icbytes);
        log.info("Indigo cert DN: " + indigoCert.getSubjectDN());
        log.info("Indigo cert issuer: " + indigoCert.getIssuerDN());
        log.info("Indigo cert SKI: " + CertUtils.getSki(indigoCert));
        log.info("Alice cert SKI: " + CertUtils.getSki(aliceCert));
        log.info("Bob cert SKI: " + CertUtils.getSki(bobCert));

        Document d = XmlUtil.stringToDocument(INDIGO_PING_RESPONSE);
        log.info("Input decorated message (reformatted): \n" + XmlUtil.nodeToFormattedString(d));
        WssProcessor p = new WssProcessorImpl();

        ProcessorResult got = p.undecorateMessage(new Message(d),
                                                  null,
                                                  null,
                                                  new WrapSSTR(aliceCert,
                                                               TestDocuments.getWssInteropAliceKey(),
                                                               new SimpleSecurityTokenResolver(bobCert)));

        checkProcessorResult(d, got);
    }

    private Pair<Message, SimpleSecureConversationSession> makeWsscSignedMessage() throws Exception {
        Message msg = new Message();
        msg.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, TestDocuments.getTestDocumentURL(TestDocuments.PLACEORDER_CLEARTEXT).openStream());

        // Sign this message with WS-SecureConversation
        final byte[] sessionKey = new byte[256];
        random.nextBytes(sessionKey);
        SimpleSecureConversationSession session =
                new SimpleSecureConversationSession("blah", sessionKey, SoapUtil.WSSC_NAMESPACE);

        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecureConversationSession(session);
        dreq.setIncludeTimestamp(true);
        dreq.setSignTimestamp();
        new WssDecoratorImpl().decorateMessage(msg, dreq);
        return new Pair<Message, SimpleSecureConversationSession>(msg, session);
    }

    public void testPartInfoCausingPrematureDomCommit() throws Exception {
        Pair<Message, SimpleSecureConversationSession> req = makeWsscSignedMessage();
        final SimpleSecureConversationSession session = req.right;

        byte[] decoratedBytes = HexUtils.slurpStream(req.left.getMimeKnob().getEntireMessageBodyAsInputStream());

        Message msg = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(decoratedBytes));

        // Undecorate the signed documnet
        SecurityContextFinder scf = new SecurityContextFinder() {
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return session;
            }
        };
        Document doc = msg.getXmlKnob().getDocumentWritable();
        ProcessorResult pr = new WssProcessorImpl().undecorateMessage(msg, null, scf, null);
        assertTrue(pr.getElementsThatWereSigned().length > 0);
        assertTrue(pr.getSigningTokens(pr.getTimestamp().asElement())[0] instanceof SecurityContextToken);

        if (pr.getProcessedActor() != null &&
            pr.getProcessedActor() == SecurityActor.L7ACTOR) {
            Element eltodelete = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
            eltodelete.getParentNode().removeChild(eltodelete);
        }

        // Stream out the message
        byte[] undecoratedBytes = HexUtils.slurpStream(msg.getMimeKnob().getEntireMessageBodyAsInputStream());

        // Security header should have been removed
        //noinspection IOResourceOpenedButNotSafelyClosed
        Document gotDoc = XmlUtil.parse(new ByteArrayInputStream(undecoratedBytes));
        log.info("Undecorated message (pretty-printed): " + XmlUtil.nodeToFormattedString(gotDoc));
        Element sec = SoapUtil.getSecurityElement(gotDoc);
        assertNull(sec);

        Element l7sec = SoapUtil.getSecurityElement(gotDoc, SecurityActor.L7ACTOR.getValue());
        assertNull(l7sec);
    }
}
