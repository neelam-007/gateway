/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.security.cert.TestKeysLoader;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SignedSamlTest;
import com.l7tech.security.token.*;
import com.l7tech.security.wstrust.RstInfo;
import com.l7tech.security.wstrust.RstrInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecorationRequirements.SimpleSecureConversationSession;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathVersion;
import org.apache.jcp.xml.dsig.internal.dom.DOMReference;
import org.apache.jcp.xml.dsig.internal.dom.DOMSubTreeData;
import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dom.DOMURIReference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author mike
 */
public class WssProcessorTest {
    private static Logger log = Logger.getLogger(WssProcessorTest.class.getName());
    private static final Random random = new Random();

    private void doTest(TestDocument testDocument) throws Exception {
        doTest(testDocument, new WssProcessorImpl());
    }

    private void doTest(TestDocument testDocument, WssProcessor wssProcessor) throws Exception {
        doTest( testDocument, wssProcessor, null );
    }

    private void doTest( final TestDocument testDocument,
                         final WssProcessor wssProcessor,
                         final Functions.UnaryVoid<ProcessorResult> verificationCallback ) throws Exception {
        Document request = testDocument.document;
        X509Certificate recipientCertificate = testDocument.recipientCertificate;
        PrivateKey recipientPrivateKey = testDocument.recipientPrivateKey;
        SecurityTokenResolver securityTokenResolver = testDocument.securityTokenResolver;


        log.info("Testing document: " + testDocument.name);
        log.info("Original decorated message (reformatted): " + XmlUtil.nodeToFormattedString(request));
        ProcessorResult result = wssProcessor.undecorateMessage(new Message(request),
                testDocument.securityContextFinder,
                                                                new WrapSSTR(recipientCertificate,
                                                                             recipientPrivateKey,
                                                                             securityTokenResolver));
        if ( verificationCallback != null ) {
            assertTrue(result != null);

            verificationCallback.call( result );
        }

        checkProcessorResult(request, result);
    }

    private void checkProcessorResult(Document request, ProcessorResult result) throws IOException {
        assertTrue(result != null);

        ParsedElement[] encrypted = result.getElementsThatWereEncrypted();
        assertTrue(encrypted != null);
        if (encrypted.length > 0) {
            log.info("The following elements were encrypted:");
            for (ParsedElement anEncrypted : encrypted) {
                Element element = anEncrypted.asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were encrypted.");

        SignedElement[] signed = result.getElementsThatWereSigned();
        assertTrue(signed != null);
        if (signed.length > 0) {
            log.info("The following elements were signed:");
            for (SignedElement aSigned : signed) {
                Element element = aSigned.asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were signed.");


        XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
        assertTrue(tokens != null);
        if (tokens.length > 0) {
            log.info("The following security tokens were found:");
            for (XmlSecurityToken token : tokens) {
                if (token instanceof SamlSecurityToken) {
                    log.info("Possession proved: " + ((SamlSecurityToken)token).isPossessionProved());
                    log.info("  " + ((SamlSecurityToken)token).getSubjectCertificate());
                } else if (token instanceof X509SigningSecurityToken) {
                    log.info("Possession proved: " + ((X509SigningSecurityToken)token).isPossessionProved());
                    log.info("  " + token);
                } else {
                    log.info("  " + token);
                }
            }
        } else
            log.info("No security tokens were found.");

        WssTimestamp timestamp = result.getTimestamp();
        if (timestamp != null) {
            log.info("Timestamp created = " + new Date(timestamp.getCreated().asTime()));
            log.info("Timestamp expires = " + new Date(timestamp.getExpires().asTime()));
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

    @Before
    public void beforeEachTest() {
        SyspropUtil.clearProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS);
        SyspropUtil.clearProperty(XencUtil.PROP_ENCRYPT_EMPTY_ELEMENTS);
        ConfigFactory.clearCachedConfig();
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS
        );
    }

    @Test
    public void testDotnetEncryptedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet encrypted request", TestDocuments.DOTNET_ENCRYPTED_REQUEST));
    }

    @Test
    public void testDotnetSignedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request", TestDocuments.DOTNET_SIGNED_REQUEST));
    }

    @Test
    public void testDotnetRequestWithUsernameToken() throws Exception {
        doTest(makeDotNetTestDocument("dotnet request with username token", TestDocuments.DOTNET_USERNAME_TOKEN));
    }

    @Test
    @BugNumber(10786)
    public void testUsernameTokenWithTrailingWhitespace() throws Exception {
        doTest(makeUsernameTokenWithTrailingWhitespaceTestDocument(), new WssProcessorImpl(), new Functions.UnaryVoid<ProcessorResult>() {
            @Override
            public void call(ProcessorResult pr) {
                UsernameToken utok = (UsernameToken) pr.getXmlSecurityTokens()[0];
                assertEquals("joe", utok.getUsername());
                assertEquals(" spacebeforeandafter ", new String(utok.getPassword()));
            }
        });
    }

    public TestDocument makeUsernameTokenWithTrailingWhitespaceTestDocument() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.DIR + "bug10786UtokPasswdWhitespace.xml");
        return new TestDocument("testUsernameTokenWithTrailingWhitespace", doc, null, null, null, null, null);
    }

    @Test
    public void testEttkSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed request", TestDocuments.ETTK_SIGNED_REQUEST));
    }

    @Test
    public void testEttkEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk encrypted request", TestDocuments.ETTK_ENCRYPTED_REQUEST));
    }

    @Test
    public void testEttkSignedEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed encrypted request", TestDocuments.ETTK_SIGNED_ENCRYPTED_REQUEST));
    }

    @Test
    public void testEttkSignedRequestIssuerSerial() throws Exception {
        doTest(makeEttkTestDocument("ettk signed request (with X509IssuerSerial)", TestDocuments.GOOGLESPELLREQUEST_SIGNED_ISSUERSERIAL));
    }

    /*
    @Test
    public void testRequestWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request wrapped l7 actor", TestDocuments.WRAPED_L7ACTOR));
    }

    @Test
    public void testRequestMultipleWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request multiple wrapped l7 actor", TestDocuments.MULTIPLE_WRAPED_L7ACTOR));
    }*/

    @Test
    public void testDotnetSignedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed SecureConversation request", TestDocuments.DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN));
    }

    @Test
    public void testDotnetSignedEncryptedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed encrypted SecureConversation request", TestDocuments.DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN));
    }

    @Test
    public void testDotnetSignedRequest2() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request 2", TestDocuments.DOTNET_SIGNED_REQUEST2));
    }

    @Test
    public void testWebsphereSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("websphere signed request", TestDocuments.WEBSPHERE_SIGNED_REQUEST));
    }

    @Test
    public void testSampleSignedSamlHolderOfKeyRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest();
        sst.setUp();
        doTest(makeEttkTestDocument("sample signed SAML holder-of-key request",
                                    /*TestDocuments.SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST*/sst.getRequestSignedWithSamlToken(false, false, false, 1)));
    }

    @Test
    public void testSignedSamlSenderVouchesRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest();
        sst.setUp();
        doTest(makeEttkTestDocument("Signed SAML sender-vouches request",
                                    sst.getSignedRequestWithSenderVouchesToken()));
    }

    @Test
    public void testSaml11DerivedKeys() throws Exception {
        doDerivedSamlTest( null, makeAliceTestDocument(
                "Signed SAMLv1.1 with derived keys request",
                TestDocuments.WAREHOUSE_REQUEST_SAML11_DERIVED_KEYS), true );
    }

    @Test
    public void testSaml11DerivedKeysTokenNotPresent() throws Exception {
        final TestDocument document1 = makeAliceTestDocument( "Signed SAMLv1.1 with derived keys request", TestDocuments.WAREHOUSE_REQUEST_SAML11_DERIVED_KEYS );
        final TestDocument document2 = makeTestDocument( "Signed SAMLv1.1 (not present) with derived keys request", TestDocuments.WAREHOUSE_REQUEST_SAML11_DERIVED_KEYS_NO_TOKEN, document1.securityTokenResolver );
        doDerivedSamlTest( document1, document2, false );
    }

    @Test
    public void testSaml20DerivedKeys() throws Exception {
        doDerivedSamlTest( null, makeAliceTestDocument(
                "Signed SAMLv2.0 with derived keys request",
                TestDocuments.WAREHOUSE_REQUEST_SAML20_DERIVED_KEYS), true );
    }

    @Test
    public void testSaml20DerivedKeysTokenNotPresent() throws Exception {
        final TestDocument document1 = makeAliceTestDocument( "Signed SAMLv2.0 with derived keys request", TestDocuments.WAREHOUSE_REQUEST_SAML20_DERIVED_KEYS );
        final TestDocument document2 = makeTestDocument( "Signed SAMLv2.0 (not present) with derived keys request", TestDocuments.WAREHOUSE_REQUEST_SAML20_DERIVED_KEYS_NO_TOKEN, document1.securityTokenResolver );
        doDerivedSamlTest( document1, document2, false );
    }

    private void doDerivedSamlTest( final TestDocument document1,
                                    final TestDocument document2,
                                    final boolean expectToken ) throws Exception {
        if ( document1!=null ) doTest( document1 );
        doTest( document2,
                new WssProcessorImpl(),
                new Functions.UnaryVoid<ProcessorResult>(){
                    @Override
                    public void call( final ProcessorResult processorResult ) {
                        boolean foundSamlToken = false;
                        for ( final XmlSecurityToken token : processorResult.getXmlSecurityTokens() ) {
                            if ( token instanceof SamlAssertion ) {
                                foundSamlToken = true;
                                break;
                            }
                        }
                        assertEquals( "Found SAML token", expectToken, foundSamlToken );
                        assertNotNull( "Timestamp", processorResult.getTimestamp() );
                        assertTrue( "Timestamp signed", Functions.map( Arrays.asList(processorResult.getElementsThatWereSigned()), new Functions.Unary<Element,SignedElement>(){
                            @Override
                            public Element call( final SignedElement signedElement ) {
                                return signedElement.asElement();
                            }
                        }).contains( processorResult.getTimestamp().asElement() ) );
                    }
                }
        );
    }

    @Test
    public void testNonSoapRequest() throws Exception {
        try {
            doTest(makeEttkTestDocument("non-SOAP request",
                                        TestDocuments.NON_SOAP_REQUEST));
            fail("Expected MessageNotSoapException was not thrown");
        } catch (MessageNotSoapException e) {
            // Ok
        }
    }

    @Test
    public void testLayer7Interop2008Response222() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "wssInterop/interop_2008_layer7_222_response.xml");
        TestDocument td = new TestDocument("Layer7Interop2008Response222", d,
                                            TestDocuments.getWssInteropAliceKey(),
                                            TestDocuments.getWssInteropAliceCert(),
                                            null,
                                            TestDocuments.getWssInteropBobCert(),
                                            new SimpleSecurityTokenResolver(TestDocuments.getWssInteropBobCert()));
        doTest(td);
    }

    @Test
    public void testBug3736StrTransform() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3736_STR_TRANSFORM_REQUEST);

        result = new TestDocument("Bug3736StrTransform", d,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null);
        doTest(result);
    }

    @Test
    public void testBug7170StrTransformX509DataIsserSerial() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.BUG_7170_STR_TRANSFORM_X509DATA_REQUEST);

        result = new TestDocument("Bug7170StrTransform", d,
                                  TestDocuments.getWssInteropAliceKey(),
                                  TestDocuments.getWssInteropAliceCert(),
                                  null,
                                  null,
                                  null);
        doTest(result);
    }

    @Test
    public void testBug3611SignatureInclusiveNamespaces() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3611_SIGNATURE_INCLUSIVE_NAMESPACES);
        result = new TestDocument("Bug3611SignatureInclusiveNamespaces", d, null, null, null, null, null);
        doTest(result);
    }

    /**
     * Test that use of unsupported XPath transform causes reasonable error (not java.lang.NoSuchMethodError).
     */
    @Test
    public void testBug3747DsigXpath() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3747_DSIG_XPATH);
        result = new TestDocument("Bug3747DsigXpath", d, null, null, null, null, null);

        try {
            doTest(result);
        } catch (InvalidDocumentFormatException idfe) {
            // Expected failure
        }
    }

    @Test
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

    @Test
    public void testKeyInfoThumbprintRequest() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "keyinfothumbreq.xml");

            SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getWssInteropAliceCert());
            result = new TestDocument("KeyInfoThumbprintRequest", d, null, null, null, null, securityTokenResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        doTest(result);
    }

    @Test
    @BugNumber(11320)
    public void testAes128GcmDecryption() throws Exception {
        doTest(new TestDocument("testAes128GcmDecryption", TestDocuments.getTestDocument(TestDocuments.DIR + "placeOrder_encrypted_aes128gcm.xml"), null, null, null, null,
                new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey())),  new WssProcessorImpl(), new Functions.UnaryVoid<ProcessorResult>() {
            @Override
            public void call(ProcessorResult processorResult) {
                final EncryptedElement[] encryptedElements = processorResult.getElementsThatWereEncrypted();
                assertEquals(1, encryptedElements.length);

                final EncryptedElement encryptedElement = encryptedElements[0];
                assertEquals(XencUtil.AES_128_GCM, encryptedElement.getAlgorithm());

                final Element element = encryptedElement.asElement();
                assertEquals("Body", element.getLocalName());
                assertTrue(element == findSoapBody(element));
                assertTrue(!nodeToString(element).contains("DecryptionFault"));
            }
        });
    }

    @Test
    @BugNumber(11320)
    public void testAes256GcmDecryption() throws Exception {
        doTest(new TestDocument("testAes256GcmDecryption", TestDocuments.getTestDocument(TestDocuments.DIR + "placeOrder_encrypted_aes256gcm.xml"), null, null, null, null,
                new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey())),  new WssProcessorImpl(), new Functions.UnaryVoid<ProcessorResult>() {
            @Override
            public void call(ProcessorResult processorResult) {
                final EncryptedElement[] encryptedElements = processorResult.getElementsThatWereEncrypted();
                assertEquals(1, encryptedElements.length);

                final EncryptedElement encryptedElement = encryptedElements[0];
                assertEquals(XencUtil.AES_256_GCM, encryptedElement.getAlgorithm());

                final Element element = encryptedElement.asElement();
                assertEquals("Body", element.getLocalName());
                assertTrue(element == findSoapBody(element));
                assertTrue(!nodeToString(element).contains("DecryptionFault"));
            }
        });
    }

    @Test(expected = ProcessorException.class)
    @BugNumber(11320)
    public void testAes256GcmDecryptionFailure() throws Exception {
        SyspropUtil.setProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS, "false");
        ConfigFactory.clearCachedConfig();

        doTest(new TestDocument("testAes256GcmDecryptionFailure", TestDocuments.getTestDocument(TestDocuments.DIR + "placeOrder_encrypted_aes256gcm_corrupted.xml"), null, null, null, null,
                new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey())));
    }

    private static Element findSoapBody(Node someNodeFromDoc) {
        try {
            Element body = SoapUtil.getBodyElement(someNodeFromDoc.getOwnerDocument());
            if (body == null)
                throw new RuntimeException("no SOAP body found");
            return body;
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException("bad SOAP env", e);
        }
    }

    private static String nodeToString(Node node) {
        try {
            return XmlUtil.nodeToString(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @BugNumber(3754)
    public void testBug3754PingReqVordelSigned() throws Exception {
        doTest(makeBug3754PingReqVordelSignedTestDocument());
    }

    public TestDocument makeBug3754PingReqVordelSignedTestDocument() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.BUG_3754_PING_REQ_VORDEL_SIGNED);
        return new TestDocument("Bug3754PingReqVordelSigned", d, null, null, null, null, null);
    }

    // TODO recreate test messsage using another cert
    @Ignore("disabled because the cert that signed the test message has since expired")
    @Test
    public void testSignedSvAssertionWithThumbprintSha1() throws Exception {
        TestDocument r;
        Document ass = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedSvThumbAssertion.xml");
        Document d = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element security = SoapUtil.getOrMakeSecurityElement(d);
        security.appendChild(d.importNode(ass.getDocumentElement(), true));
        SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate());
        r = new TestDocument("SignedSvAssertionWithThumbprintSha1", d, null, null, null, null, securityTokenResolver);
        doTest(r);
    }

    // TODO recreate test messsage using another cert
    @Ignore("Disabled because the cert that signed the test request has since expired")
    @Test
    public void testCompleteEggRequest() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/ValidBlueCardRequest.xml");

        Element sec = SoapUtil.getSecurityElement(d);
        Element ass = DomUtils.findFirstChildElementByName(sec, SamlConstants.NS_SAML, "Assertion");

        Document assDoc = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedAttrThumbAssertion.xml");
        sec.replaceChild(d.importNode(assDoc.getDocumentElement(), true), ass);

        //XmlUtil.nodeToOutputStream(d, new FileOutputStream("c:/eggerequest.xml"));

        SecurityTokenResolver securityTokenResolver = new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate());
        TestDocument td = new TestDocument("CompleteEggRequest", d, null, null, null, null, securityTokenResolver);
        doTest(td);
    }

    @Test
    public void testWssInterop2005JulyResponse() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_RESPONSE);

        result = new TestDocument("WssInterop2005JulyResponse", d,
                                  TestDocuments.getWssInteropAliceKey(),
                                  TestDocuments.getWssInteropAliceCert(), null,
                                  TestDocuments.getWssInteropBobCert(), null );
        doTest(result);
    }

    @Test
    public void testUnknownBinarySecurityTokenTypeRejection() throws Exception {
        try {
            TestDocument result;
            Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_BST);
            result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_BST, d, null, null, null, null, null );
            doTest(result);
            Assert.fail("Expected  ProcessorException for unknown BST");
        } catch ( ProcessorException pe ) {
            Assert.assertTrue("Exception is unknown bst", ExceptionUtils.getMessage(pe).contains("BinarySecurityToken"));
        }
    }

    @Test
    public void testUnknownBinarySecurityTokenTypeSuccess() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_BST);
        result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_BST, d, null, null, null, null, null );

        WssProcessorImpl wssProcessor = new WssProcessorImpl();
        wssProcessor.setPermitUnknownBinarySecurityTokens(true);

        doTest(result, wssProcessor);
    }

    @Test
    public void testUnknownMustUnderstandHeaderRejection() throws Exception {
        try {
            TestDocument result;
            Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_MUSTUNDERSTAND);
            result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_MUSTUNDERSTAND, d, null, null, null, null, null );
            doTest(result);
            Assert.fail("Expected  ProcessorException for unknown header with MustUnderstand=1");
        } catch ( ProcessorValidationException pve ) {
            Assert.assertTrue("Exception is MustUnderstand", ExceptionUtils.getMessage(pve).contains("mustUnderstand"));
        }
    }

    @Test
    public void testUnknownMustUnderstandHeaderSuccess() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_MUSTUNDERSTAND);
        result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_UNKNOWN_MUSTUNDERSTAND, d, null, null, null, null, null );

        WssProcessorImpl wssProcessor = new WssProcessorImpl();
        wssProcessor.setRejectOnMustUnderstand(false);

        doTest(result, wssProcessor);
    }

    @Test
    public void testMultipleTimestampSignatureRejection() throws Exception {
        try {
            TestDocument result;
            Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_MULTIPLE_TIMESTAMP_SIGS);
            result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_MULTIPLE_TIMESTAMP_SIGS, d, null, null, null, null, null );
            doTest(result);
            Assert.fail("Expected  ProcessorException for multiple signatures for timestamp");
        } catch ( ProcessorValidationException pve ) {
            Assert.assertTrue("Exception is timestamp signature", ExceptionUtils.getMessage(pve).contains("Timestamp"));
        }
    }

    @Test
    public void testMultipleTimestampSignatureSuccess() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_MULTIPLE_TIMESTAMP_SIGS);
        result = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_MULTIPLE_TIMESTAMP_SIGS, d, null, null, null, null, null );

        WssProcessorImpl wssProcessor = new WssProcessorImpl();
        wssProcessor.setPermitMultipleTimestampSignatures(true);

        doTest(result, wssProcessor);
    }

    @BugNumber(7199)
    @Test
    public void testOutOfOrderSecurityHeader() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_OUT_OF_ORDER_HEADER);
        TestDocument testDoc = new TestDocument(TestDocuments.WAREHOUSE_REQUEST_OUT_OF_ORDER_HEADER, d, null, null, null, null, null );
        doTest(testDoc);
    }

    private TestDocument makeAliceTestDocument( final String testname, final String docname ) {
        try {
            return makeTestDocument( testname, docname, new SimpleSecurityTokenResolver(TestDocuments.getWssInteropAliceCert(), TestDocuments.getWssInteropAliceKey()) );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestDocument makeTestDocument( final String testname, final String docname, final SecurityTokenResolver securityTokenResolver ) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d, null, null, null, null, securityTokenResolver );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(), null, null, null );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestDocument makeEttkTestDocument(String testname, Document doc) {
        try {
            return new TestDocument(testname, doc,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(), null, null, null );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDocument makeDotNetTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            //final SecureConversationSession session = new SecureConversationSession();

            // Set up a fake ws-sc session, in case this example will be needing it
            final byte[] sharedSecret = TestDocuments.getDotNetSecureConversationSharedSecret();
            SecurityContextFinder dotNetSecurityContextFinder = new SecurityContextFinder() {
                @Override
                public SecurityContext getSecurityContext(String securityContextIdentifier) {
                    return new SecurityContext(){
                        @Override
                        public byte[] getSharedSecret() {
                            return sharedSecret;
                        }
                        @Override
                        public SecurityToken getSecurityToken() {
                            return null;
                        }
                    };
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

    @Test
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

        ProcessorResult got = p.undecorateMessage(new Message(d), null,
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
        dreq.setSignTimestamp(true);
        new WssDecoratorImpl().decorateMessage(msg, dreq);
        return new Pair<Message, SimpleSecureConversationSession>(msg, session);
    }

    @Test
    public void testPartInfoCausingPrematureDomCommit() throws Exception {
        Pair<Message, SimpleSecureConversationSession> req = makeWsscSignedMessage();
        final SimpleSecureConversationSession session = req.right;

        byte[] decoratedBytes = IOUtils.slurpStream(req.left.getMimeKnob().getEntireMessageBodyAsInputStream());

        Message msg = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(decoratedBytes));

        // Undecorate the signed documnet
        SecurityContextFinder scf = new SecurityContextFinder() {
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return session;
            }
        };
        Document doc = msg.getXmlKnob().getDocumentWritable();
        ProcessorResult pr = new WssProcessorImpl().undecorateMessage(msg, scf, null);
        assertTrue(pr.getElementsThatWereSigned().length > 0);
        assertTrue(pr.getSigningTokens(pr.getTimestamp().asElement())[0] instanceof SecurityContextToken);

        if (pr.getProcessedActor() != null &&
            pr.getProcessedActor() == SecurityActor.L7ACTOR) {
            Element eltodelete = SoapUtil.getSecurityElement(doc, pr.getProcessedActorUri());
            eltodelete.getParentNode().removeChild(eltodelete);
        }

        // Stream out the message
        byte[] undecoratedBytes = IOUtils.slurpStream(msg.getMimeKnob().getEntireMessageBodyAsInputStream());

        // Security header should have been removed
        //noinspection IOResourceOpenedButNotSafelyClosed
        Document gotDoc = XmlUtil.parse(new ByteArrayInputStream(undecoratedBytes));
        log.info("Undecorated message (pretty-printed): " + XmlUtil.nodeToFormattedString(gotDoc));
        Element sec = SoapUtil.getSecurityElement(gotDoc);
        assertNull(sec);

        Element l7sec = SoapUtil.getSecurityElementForL7(gotDoc);
        assertNull(l7sec);
    }

    private static interface Configurer {
        boolean configure(Document placeOrder, Element blarg) throws IOException;
    }

    @Test
    public void testBug2157RejectSoapHeaders() throws Exception {
        final String blargns = "http://example.com/ns/blargle";

        List<Configurer> tests = new ArrayList<Configurer>(Arrays.asList(
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Simple attrs with no namespace prefixes are recognized (apparently allowed by SOAP 1.1)
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        blarg.setAttributeNS( null, "mustUnderstand", "1" );
                        blarg.setAttributeNS( null, "actor", SoapUtil.ACTOR_VALUE_NEXT );
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=1, actor="+SoapUtil.ACTOR_VALUE_NEXT;
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // An actor with a SOAP namepsace URI is recognized and rejected
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "1");
                        blarg.setAttributeNS(soapns, "soapenv:actor", SoapUtil.ACTOR_VALUE_NEXT);
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=1, soapenv:actor="+SoapUtil.ACTOR_VALUE_NEXT;
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // A role (SOAP 1.2) is recognized and rejected
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "true");
                        blarg.setAttributeNS(soapns, "soapenv:role", SoapUtil.ROLE_VALUE_NEXT);
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=true, soapenv:role="+SoapUtil.ROLE_VALUE_NEXT;
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Same as #1 but with SecureSpan actor
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        blarg.setAttributeNS( null, "mustUnderstand", "1" );
                        blarg.setAttributeNS( null, "actor", "secure_span" );
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=1, actor=secure_span";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Same as #2 but with SecureSpan actor
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "1");
                        blarg.setAttributeNS(soapns, "soapenv:actor", "secure_span");
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=1, soapenv:actor=secure_span";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Same as #3 but with SecureSpan role
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "true");
                        blarg.setAttributeNS(soapns, "soapenv:role", "secure_span");
                        return true;
                    }
                    public String toString() {
                        return "mustUnderstand=true, soapenv:role=secure_span";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Not rejected for mustUndestand=false
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        blarg.setAttributeNS( null, "mustUnderstand", "false" );
                        blarg.setAttributeNS( null, "actor", "secure_span" );
                        return false;
                    }
                    public String toString() {
                        return "mustUnderstand=false, soapenv:actor=secure_span";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Not rejected for no mustUnderstand
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        blarg.setAttributeNS( null, "actor", "secure_span" );
                        return false;
                    }
                    public String toString() {
                        return "soapenv:actor=secure_span";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Not rejected for no actor
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "1");
                        return false;
                    }
                    public String toString() {
                        return "soapenv:mustUnderstand=1";
                    }
                },
                new Configurer() {
                    public boolean configure(Document placeOrder, Element blarg) throws IOException {
                        // Not rejected for empty role
                        blarg.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:blarg", blargns);
                        final String soapns = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;
                        blarg.getOwnerDocument().getDocumentElement().setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soapenv", soapns );
                        blarg.setAttributeNS(soapns, "soapenv:mustUnderstand", "true");
                        blarg.setAttributeNS(soapns, "soapenv:role", "");
                        return false;
                    }
                    public String toString() {
                        return "soapenv:mustUnderstand=true, soapenv:role=";
                    }
                }
        ));

        for (Configurer test : tests) {
            Document placeOrder = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
            Element security = SoapUtil.getOrMakeSecurityElement(placeOrder);
            Element header = (Element)security.getParentNode();
            header.removeChild(security);
            final Element blarg = header.getOwnerDocument().createElementNS(blargns, "blarg:blargle");
            header.appendChild(blarg);
            boolean shouldReject = test.configure(placeOrder, blarg);

            // Round trip doc to fix modified namespaces
            placeOrder = XmlUtil.parse(XmlUtil.nodeToString(placeOrder));

            {
                WssProcessorImpl processor = new WssProcessorImpl(new Message(placeOrder));
                processor.setRejectOnMustUnderstand(false);
                processor.processMessage();
            }

            try {
                WssProcessorImpl processor = new WssProcessorImpl(new Message(placeOrder));
                processor.setRejectOnMustUnderstand(shouldReject);
                processor.processMessage();
                if (shouldReject)
                    fail("failed to reject message with mustUnderstand=1 : " + test);
            } catch (ProcessorValidationException e) {
                if (!shouldReject)
                    fail("rejected a document that should have passed : " + test);
                assertTrue(e.getMessage().contains("mustUnderstand"));
            }
        }
    }

    @Test
    @BugNumber(4667)
    public void testBug4667SignatureCacheBug() throws Exception {
        WssProcessor proc = new WssProcessorImpl();

        try {
            Message signed2 = makeMessage(HACKED);
            ProcessorResult pr2 = proc.undecorateMessage(signed2, null, null);
            assertTrue(pr2.getElementsThatWereSigned().length < 1);
        } catch (InvalidDocumentSignatureException e) {
            // Ok
        }

        try {
            Message signed1 = makeMessage(SIGNED);
            ProcessorResult pr1 = proc.undecorateMessage(signed1, null, null);
            assertTrue(pr1.getElementsThatWereSigned().length > 0);
        } catch (InvalidDocumentSignatureException e) {
            fail("Signature should have validated");
        }

        try {
            Message signed3 = makeMessage(HACKED);
            ProcessorResult pr3 = proc.undecorateMessage(signed3, null, null);
            assertTrue("Signature should NOT have validated", pr3.getElementsThatWereSigned().length < 1);
        } catch (InvalidDocumentSignatureException e) {
            // Ok
        }
    }

    @Test
    @BugNumber(6002)
    public void testCanonicalizeWithCanceledDefaultNamespace() throws Exception {
        Document doc = XmlUtil.stringToDocument("<a xmlns=\"urn:a\"><b xmlns=\"\"><c/></b></a>");
        Element element = doc.getDocumentElement();

        // Expected failure due to XSS4J excl c11r bug; see Bug #6002.  If bug is fixed, swap the next two tests
        //assertEquals(canonicalizeWithIbm(element), canonicalizeWithSun(element));
        assertTrue(!canonicalizeWithIbm(element).equals(canonicalizeWithSun(element)));
    }


    @Test
    @BugNumber(6002)
	public void testCanonicalizeNcesMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.NCES_REQ_WITH_CANCELED_XMLNS);
        Element element = SoapUtil.getBodyElement(doc);

        String canonResultIbm = canonicalizeWithIbm(element);

        final String ibmFormatted = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(canonResultIbm));
        final String sunFormatted = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(canonicalizeWithSun(element)));

        // Expected failure due to XSS4J excl c11r bug; see Bug #6002.  If bug is fixed, swap the next two tests
        //assertEquals(ibmFormatted, sunFormatted);
        assertTrue(!ibmFormatted.equals(sunFormatted));
    }

    private String canonicalizeWithIbm(Element element) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ExclusiveC11r().canonicalize(element, baos);
        return baos.toString();
    }

    private String canonicalizeWithSun(Element element) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ApacheExclusiveC14nAdaptor().canonicalize(element, baos);
        return baos.toString();
    }

    private Document getNcesSignedRequest() throws IOException, SAXException {
        String docXml = TestDocuments.getTestDocumentAsXml(TestDocuments.NCES_REQ_WITH_CANCELED_XMLNS);
        return XmlUtil.stringToDocument(docXml);
    }

    @Test
    @BugNumber(6002)
	public void testProcessNcesSignedMessageWithTrogdor() throws Exception {
        Document doc = getNcesSignedRequest();
        WssProcessorImpl proc = new WssProcessorImpl(new Message(doc));
        ProcessorResult result = proc.processMessage();
        final SignedElement[] signed = result.getElementsThatWereSigned();
        System.out.println("Saw " + signed.length + " signed elements:");
        for (SignedElement signedElement : signed) {
            System.out.println(signedElement.asElement().getNodeName());
        }
        assertTrue(signed.length == 4); // message ID, saml assertion, timestamp, and body
    }

    @Test
    @BugNumber(6002)
	public void testVerifyNcesSignatureWithSun() throws Exception {
        final Document doc = getNcesSignedRequest();
        Element sechdr = SoapUtil.getSecurityElement(doc);
        Element signature = XmlUtil.findOnlyOneChildElementByName(sechdr, "http://www.w3.org/2000/09/xmldsig#", "Signature");

        XMLSignatureFactory sigfac = XMLSignatureFactory.getInstance( "DOM", new XMLDSigRI() );
        X509Certificate ncesCert = CertUtils.decodeCert( HexUtils.decodeBase64( SIGNED_WITH_NCES_CERT, true ) );
        XMLSignature sig = sigfac.unmarshalXMLSignature(new DOMStructure(signature));
        DOMValidateContext vc = new DOMValidateContext(ncesCert.getPublicKey(), doc);

        vc.setURIDereferencer(new URIDereferencer() {
            public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException {
                DOMURIReference ref = (DOMURIReference)uriReference;
                final String targetUri = ref.getURI();
                if (targetUri == null)
                    throw new URIReferenceException("Reference lacks target URI: " + ref.getHere().getNodeName());
                Element found = null;
                try {
                    found = SoapUtil.getElementByWsuId(doc, targetUri);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (found == null)
                    throw new URIReferenceException("Reference target element not found: " + ref.getHere().getNodeName() + ": URI=" + targetUri);
                return new DOMSubTreeData(found, false);
            }
        });

        final boolean validity = sig.validate(vc);
        List refs = sig.getSignedInfo().getReferences();
        for (Object refobj : refs) {
            DOMReference ref = (DOMReference) refobj;
            System.out.printf("Ref: %s  validity:%b\n", ref.getURI(), ref.validate(vc));
        }
        assertTrue(validity);
    }

    @Ignore("Test signature needs to be regenerated after fix for Bug #7859")
    @Test
	public void testEcdsaSha256ignedRequest() throws Exception {
        doTest(makeEttkTestDocument("ECDSA with SHA-256 signed request", TestDocuments.ECDSA_SHA256_REQUEST));
    }

    @Ignore("Temporarily disabled while RSA Jsafe problem is investigated")
    @Test
	public void testEcdsaSha384ignedRequest() throws Exception {
        doTest(makeEttkTestDocument("ECDSA with SHA-384 signed request", TestDocuments.ECDSA_SHA384_REQUEST));
    }

    @Ignore("Test signature needs to be regenerated after fix for Bug #7859")
    @Test
    public void testEcdsaSha284RequestSignedUsingLuna() throws Exception {
        doTest(makeEttkTestDocument("ECDSA with SHA-284 signed using Luna", XmlUtil.stringAsDocument(SIGNED_USING_SHA384_WITH_ECDSA)));
    }

    @Test
    @BugNumber(7580)
    public void testBug7580ProcessWsscRequest() throws Exception {
        SecurityContextFinder scf = new SecurityContextFinder() {
            @Override
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return new SimpleSecureConversationSession(B7580_WSSC_IDENT, B7580_WSSC_SHARED_SECRET, "http://schemas.xmlsoap.org/ws/2004/04/sc");
            }
        };
        TestDocument testDocument = new TestDocument("Bug7580ProcessWsscRequest", XmlUtil.stringAsDocument(B7580_WSSC_REQ), null, null, scf, null, null);
        doTest(testDocument);
    }

    @Test(expected=InvalidDocumentFormatException.class)
    @BugNumber(7758)
    public void testXpointerReferenceRejected() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.WAREHOUSE_REQUEST_XPOINTER_REFERENCE);
        TestDocument td = new TestDocument("Warehouse Request XPointer", d, null, null, null, null, null);
        doTest( td );
    }

    @Ignore("Test certificate has expired")
    @Test
    public void testEcdsaSignedFromMiltonAtGd() throws Exception {
        doTest(makeEttkTestDocument("ECDSA signed by Milton", XmlUtil.stringAsDocument(ECDSA_EXAMPLE_FROM_MILTON_GD)));
    }

    @Test
    @BugNumber(7157)
    public void testMultipleSignatures() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "bug7157_signatureCombination.xml");
        TestDocument td = new TestDocument("Multiple signatures", d, null, null, null, null, null);
        doTest(td);

        // Test proposed XPath for detecting this issue
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("ds", SoapUtil.DIGSIG_URI);
        assertTrue(XpathUtil.testXpathExpression(d, "count(/*/*[local-name()=\"Header\"]/*[local-name()=\"Security\"]/ds:Signature)=2", XpathVersion.XPATH_1_0, nsmap, null).matches());
        assertFalse(XpathUtil.testXpathExpression(d, "count(/*/*[local-name()=\"Header\"]/*[local-name()=\"Security\"]/ds:Signature)=1", XpathVersion.XPATH_1_0, nsmap, null).matches());
    }

    @Test
    @BugNumber(7157)
    public void testMultipleSignaturesXP20() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "bug7157_signatureCombination.xml");
        TestDocument td = new TestDocument("Multiple signatures", d, null, null, null, null, null);
        doTest(td);

        // Test proposed XPath for detecting this issue
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("ds", SoapUtil.DIGSIG_URI);
        assertTrue(XpathUtil.testXpathExpression(d, "count(/*/*[local-name()=\"Header\"]/*[local-name()=\"Security\"]/ds:Signature)=2", XpathVersion.XPATH_2_0, nsmap, null).matches());
        assertFalse(XpathUtil.testXpathExpression(d, "count(/*/*[local-name()=\"Header\"]/*[local-name()=\"Security\"]/ds:Signature)=1", XpathVersion.XPATH_2_0, nsmap, null).matches());
    }

    /**
     * It is not important how this test fails, this is to ensure we don't loop
     */
    @Test(expected=InvalidDocumentFormatException.class)
    public void testRecursiveEncryptedKeys() throws Exception {
        Document d = XmlUtil.parse( RECURSIVE_ENCRYPTED_KEYS );
        TestDocument td = new TestDocument("Recursive Encrypted Keys", d, null, null, null, null, null);
        doTest(td);
    }

    private static Message makeMessage(String xml) throws SAXException {
        Message message = new Message();
        message.initialize(XmlUtil.stringToDocument(xml));
        return message;
    }

    @Test(expected = ProcessorException.class)
    @BugNumber(9781)
    public void testBug9781WrongDecryptionKey() throws Exception {
        System.setProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS, "false");
        ConfigFactory.clearCachedConfig();
        try {
            Document d = TestDocuments.getTestDocument("com/l7tech/policy/resources/bug9781_request.xml");
            TestDocument td = new TestDocument("testBug9781WrongDecryptionKey", d, TestDocuments.getWssInteropAliceKey(), TestDocuments.getWssInteropAliceCert(), null, null, null);
            doTest(td);
        } finally {
            System.clearProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS);
            ConfigFactory.clearCachedConfig();
        }
    }

    @Test
    @BugNumber(9298)
    public void testPkiPathBinarySecurityToken() throws Exception {
        Document d = TestDocuments.getTestDocument("com/l7tech/policy/resources/bug_9298_signed_with_pkipath.xml");
        TestDocument td = new TestDocument("testPkiPathBinarySecurityToken", d, null, null, null, null, null);
        doTest(td, new WssProcessorImpl(), new Functions.UnaryVoid<com.l7tech.security.xml.processor.ProcessorResult>() {
            @Override
            public void call(ProcessorResult pr) {
                SignedElement[] signed = pr.getElementsThatWereSigned();
                assertNotNull(signed);
                assertTrue(signed.length > 0);

                SignedElement elm = signed[0];
                assertNotNull(elm);

                SigningSecurityToken tok = elm.getSigningSecurityToken();
                assertNotNull(tok);
                assertTrue(tok.isPossessionProved());

                if (!(tok instanceof X509SigningSecurityToken)) {
                    fail("must be reported as an X.509 token");
                }

                X509Certificate cert = ((X509SigningSecurityToken) tok).getMessageSigningCertificate();
                assertNotNull(cert);
                assertEquals("CN=andygray", cert.getSubjectDN().toString());
            }
        });
    }

    @Test
    @BugNumber(9298)
    public void testSignedWithCertEmbeddedInKeyIdentifier() throws Exception {
        Document d = TestDocuments.getTestDocument("com/l7tech/policy/resources/bug_9298_signed_with_cert_keyid.xml");
        TestDocument td = new TestDocument("testSignedWithCertEmbeddedInKeyIdentifier", d, null, null, null, null, null);
        doTest(td, new WssProcessorImpl(), new Functions.UnaryVoid<com.l7tech.security.xml.processor.ProcessorResult>() {
            @Override
            public void call(ProcessorResult pr) {
                SignedElement[] signed = pr.getElementsThatWereSigned();
                assertNotNull(signed);
                assertTrue(signed.length > 0);

                SignedElement elm = signed[0];
                assertNotNull(elm);

                SigningSecurityToken tok = elm.getSigningSecurityToken();
                assertNotNull(tok);
                assertTrue(tok.isPossessionProved());

                if (!(tok instanceof X509SigningSecurityToken)) {
                    fail("must be reported as an X.509 token");
                }

                X509Certificate cert = ((X509SigningSecurityToken) tok).getMessageSigningCertificate();
                assertNotNull(cert);
                assertEquals("CN=andygray", cert.getSubjectDN().toString());
            }
        });
    }

    @Test
    public void testWcfTrace() throws Exception {
        // Load private keys we'll need to process the trace messages
        SignerInfo[] privateKeys = TestKeysLoader.loadPrivateKeys(TestDocuments.DIR + "wcf_unum/", ".p12", "password".toCharArray(),
                "bookstoreservice_com", "bookstorests_com");
        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver(null, privateKeys);


        // 00 Initial client RST
        Message rst;
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/00_cl_rst_sct.xml");
        WssProcessorImpl processor = new WssProcessorImpl(rst = new Message(d));
        processor.setSecurityTokenResolver(resolver);
        ProcessorResult pr = processor.processMessage();
        assertBodyWasSigned(pr, d);
        RstInfo rstInfo = RstInfo.parseRstElement(SoapUtil.getPayloadElement(rst.getXmlKnob().getDocumentReadOnly()));


        // 01 Server RSTR
        Message rstr;
        d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/01_sv_rstr_sct.xml");
        processor = new WssProcessorImpl(rstr = new Message(d));
        processor.setSecurityTokenResolver(resolver);
        pr = processor.processMessage();
        assertBodyWasSigned(pr, d);
        RstrInfo rstrInfo = RstrInfo.parseRstrElement(SoapUtil.getPayloadElement(rstr.getXmlKnob().getDocumentReadOnly()), null);


        // Reconstruct WS-SC session using parsed RST and RSTR messages along with knowledge of private key
        final byte[] sharedKey = SecureConversationKeyDeriver.pSHA1( rstInfo.decodedNonce, rstrInfo.getEntropy(), rstrInfo.isKeySizePresent() ? rstrInfo.getKeySize()/8 : 32 );
        // get the sct Identifier
        String identifier = null;
        if ( rstrInfo.getToken() != null ) {
            Element tokenIdentifier = XmlUtil.findOnlyOneChildElementByName(rstrInfo.getToken(), SoapConstants.WSSC_NAMESPACE_ARRAY, "Identifier");
            if (tokenIdentifier != null) {
                identifier = XmlUtil.getTextValue(tokenIdentifier);
            }
        }
        final Pair<String, byte[]> session = new Pair<String,byte[]>( identifier, sharedKey );
        final SecurityContextFinder scFinder = new SecurityContextFinder() {
            @Override
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                if (session.left.equals(securityContextIdentifier)) {
                    return new SecurityContext() {
                        @Override
                        public byte[] getSharedSecret() {
                            return session.right;
                        }

                        @Override
                        public SecurityToken getSecurityToken() {
                            throw new UnsupportedOperationException("getSecurityToken");
                        }
                    };
                }
                return null;
            }
        };


        // 02 Client BuyBook
        d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/02_cl_buybook.xml");
        processor = new WssProcessorImpl(new Message(d));
        processor.setSecurityTokenResolver(resolver);
        processor.setSecurityContextFinder(scFinder);
        pr = processor.processMessage();
        assertBodyWasSigned(pr, d);


        // 03 Server BuyBookResponse
        d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/03_sv_buybookresponse.xml");
        processor = new WssProcessorImpl(new Message(d));
        processor.setSecurityTokenResolver(resolver);
        processor.setSecurityContextFinder(scFinder);
        pr = processor.processMessage();
        assertBodyWasSigned(pr, d);


        // 04 Client Cancel Context
        d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/04_cl_rst_sct_cancel.xml");
        processor = new WssProcessorImpl(new Message(d));
        processor.setSecurityTokenResolver(resolver);
        processor.setSecurityContextFinder(scFinder);
        pr = processor.processMessage();
        assertBodyWasSigned(pr, d);


        // 04 Client Cancel Context
        d = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/05_sv_rstr_sct_cancel.xml");
        processor = new WssProcessorImpl(new Message(d));
        processor.setSecurityTokenResolver(resolver);
        processor.setSecurityContextFinder(scFinder);
        pr = processor.processMessage();
        assertBodyWasSigned(pr, d);
    }

    private void assertBodyWasSigned(ProcessorResult pr, Document soapenv) throws InvalidDocumentFormatException {
        boolean sawSignedBody = false;
        
        Element body = SoapUtil.getBodyElement(soapenv);
        for (SignedElement signedElement : pr.getElementsThatWereSigned()) {
            if (signedElement.asElement() == body)
                sawSignedBody = true;
        }

        assertTrue("Body was not recorded as signed and successfully verified", sawSignedBody);
    }

    public static final String SIGNED =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" actor=\"secure_span\" soapenv:mustUnderstand=\"1\"><wsu:Timestamp wsu:Id=\"Timestamp-1-bb20ffed092c39938d5776ddb3a52009\"><wsu:Created>2008-01-24T22:18:43.582Z</wsu:Created><wsu:Expires>2008-01-24T22:23:43.582Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-f0a1e27c6fff3697f437d09435cf387f\">MIICazCCAdSgAwIBAgIEPb7rVDANBgkqhkiG9w0BAQQFADB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwHhcNMDUwMTAxMDAwMDAwWhcNMjUxMjMxMjM1OTU5WjB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK2K2KzkU42+/bfpfDUIo68oA5DQ1iW9F38UrC5/5PVcIVp0cyu28eGr/5n8OVyfZhBg4Kn1q5L5aQFwvQBSskk9RvBkgHYLIFkmOdLv6N1vftEphBSw1E2WB0hyhkzxu8JmV0FJ+dq3jEM/JA4kHsTEOsyYj20/Q1j0Y3Sel+fDAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAiA+65PCTbLfkB7OLz5OEQUwySoK16nTY3cXKGrq1rWdHAYmr+FfVF+1ePicihDMVqfzZHeHMlNAvjVRliwP4HuU58OMz3Jn+8iJ0exKH9EKgfFZ7csX7cyXtZfvaMTxlAca04muonxJS0FFqxSFgJNScQELaA6R82wse0hksr7o=</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#Timestamp-1-bb20ffed092c39938d5776ddb3a52009\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>ZjUwdUoLkXCAaaDV0Fxxl4Xz+L0=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>a+6a1V+IULo5DsO2E507O22Me6OH198ZrL2aYTD0ktI6bfX+66H0asqFFCc35WWCtkmqSjBvWEvIX+OBmfWW4jIEgCMVSd+/TBUzs0dFj9z69/yx66ALFt6kOr/IUCxXdW2M2XhC9Bw1kJASfRTD0WhJaF9z415QxkOLq3yUhxY=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#BinarySecurityToken-0-f0a1e27c6fff3697f437d09435cf387f\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"></wsse:Reference></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soapenv:Header><soapenv:Body>\n" +
            "        <ns1:placeOrder xmlns:ns1=\"http://warehouse.acme.com/ws\" soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "            <productid xsi:type=\"xsd:long\">-9206260647417300294</productid>\n" +
            "            <amount xsi:type=\"xsd:long\">1</amount>\n" +
            "            <price xsi:type=\"xsd:float\">5.0</price>\n" +
            "            <accountid xsi:type=\"xsd:long\">228</accountid>\n" +
            "        </ns1:placeOrder>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    public static final String HACKED =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" actor=\"secure_span\" soapenv:mustUnderstand=\"1\"><wsu:Timestamp wsu:Id=\"Timestamp-1-bb20ffed092c39938d5776ddb3a52009\"><wsu:Created>2008-01-24T22:18:43.582Z</wsu:Created><wsu:Expires>2008-01-24T22:23:43.582Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-f0a1e27c6fff3697f437d09435cf387f\">MIICazCCAdSgAwIBAgIEPb7rVDANBgkqhkiG9w0BAQQFADB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwHhcNMDUwMTAxMDAwMDAwWhcNMjUxMjMxMjM1OTU5WjB6MRMwEQYDVQQDDApKb2huIFNtaXRoMR8wHQYDVQQLDBZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMQwwCgYDVQQKDANJQk0xEjAQBgNVBAcMCUN1cGVydGlubzETMBEGA1UECAwKQ2FsaWZvcm5pYTELMAkGA1UEBhMCVVMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK2K2KzkU42+/bfpfDUIo68oA5DQ1iW9F38UrC5/5PVcIVp0cyu28eGr/5n8OVyfZhBg4Kn1q5L5aQFwvQBSskk9RvBkgHYLIFkmOdLv6N1vftEphBSw1E2WB0hyhkzxu8JmV0FJ+dq3jEM/JA4kHsTEOsyYj20/Q1j0Y3Sel+fDAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAiA+65PCTbLfkB7OLz5OEQUwySoK16nTY3cXKGrq1rWdHAYmr+FfVF+1ePicihDMVqfzZHeHMlNAvjVRliwP4HuU58OMz3Jn+8iJ0exKH9EKgfFZ7csX7cyXtZfvaMTxlAca04muonxJS0FFqxSFgJNScQELaA6R82wse0hksr7o=</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#Timestamp-1-bb20ffed092c39938d5776ddb3a52009\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>ZjUwdUoLkXCAaaDV0Fxxl4Xz+L0=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>a+6a1V+IULo5DE50s027O22Me6OH198ZrL2aYTD0ktI6bfX+66H0asqFFCc35WWCtkmqSjBvWEvIX+OBmfWW4jIEgCMVSd+/TBUzs0dFj9z69/yx66ALFt6kOr/IUCxXdW2M2XhC9Bw1kJASfRTD0WhJaF9z415QxkOLq3yUhxY=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#BinarySecurityToken-0-f0a1e27c6fff3697f437d09435cf387f\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"></wsse:Reference></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soapenv:Header><soapenv:Body>\n" +
            "        <ns1:placeOrder xmlns:ns1=\"http://warehouse.acme.com/ws\" soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "            <productid xsi:type=\"xsd:long\">-9206260647417300294</productid>\n" +
            "            <amount xsi:type=\"xsd:long\">1</amount>\n" +
            "            <price xsi:type=\"xsd:float\">5.0</price>\n" +
            "            <accountid xsi:type=\"xsd:long\">228</accountid>\n" +
            "        </ns1:placeOrder>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String SIGNED_WITH_NCES_CERT =
            "MIID4DCCA0mgAwIBAgIDANPtMA0GCSqGSIb3DQEBBQUAMFcxCzAJBgNVBAYTAlVT\n" +
            "MRgwFgYDVQQKEw9VLlMuIEdvdmVybm1lbnQxDDAKBgNVBAsTA0RvRDEMMAoGA1UE\n" +
            "CxMDUEtJMRIwEAYDVQQDEwlET0QgQ0EtMTMwHhcNMDgwNTA5MjEzNTI5WhcNMTEw\n" +
            "NTEwMjEzNTI5WjByMQswCQYDVQQGEwJVUzEYMBYGA1UEChMPVS5TLiBHb3Zlcm5t\n" +
            "ZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsTA1BLSTENMAsGA1UECxMEVVNBRjEe\n" +
            "MBwGA1UEAxMVc2VjdXJpdHkubmNlcy5kb2QubWlsMIGfMA0GCSqGSIb3DQEBAQUA\n" +
            "A4GNADCBiQKBgQCp/c/SufZ7aAfdiz4Kw0kx4JejzddFAMWQXjRib8DGTAeJQNib\n" +
            "0cGXaS1d/gc1YPnmmlS+mPHo1r9bGMXqlpoMGqmni+0ifeFW9Xk8ZDPdtWpVYjJK\n" +
            "OHtmRERL+8gwQtBlDmgCJVViSlsG5ZSZ8T6sSoUe0RVm/tWhalCuSUXYpQIDAQAB\n" +
            "o4IBnTCCAZkwHwYDVR0jBBgwFoAUZGRDJaRs5w0iHWWswOR1N8wE2towHQYDVR0O\n" +
            "BBYEFHaYFXcU4XaTudFQbmi7YnlLyzHeMA4GA1UdDwEB/wQEAwIFoDCBxwYDVR0f\n" +
            "BIG/MIG8MC2gK6AphicgaHR0cDovL2NybC5kaXNhLm1pbC9nZXRjcmw/RE9EJTIw\n" +
            "Q0EtMTMwgYqggYeggYSGgYEgbGRhcDovL2NybC5nZHMuZGlzYS5taWwvY24lM2RE\n" +
            "T0QlMjBDQS0xMyUyY291JTNkUEtJJTJjb3UlM2REb0QlMmNvJTNkVS5TLiUyMEdv\n" +
            "dmVybm1lbnQlMmNjJTNkVVM/Y2VydGlmaWNhdGVyZXZvY2F0aW9ubGlzdDtiaW5h\n" +
            "cnkwFgYDVR0gBA8wDTALBglghkgBZQIBCwUwZQYIKwYBBQUHAQEEWTBXMDMGCCsG\n" +
            "AQUFBzAChidodHRwOi8vY3JsLmRpc2EubWlsL2dldHNpZ24/RE9EJTIwQ0EtMTMw\n" +
            "IAYIKwYBBQUHMAGGFGh0dHA6Ly9vY3NwLmRpc2EubWlsMA0GCSqGSIb3DQEBBQUA\n" +
            "A4GBAKBixlrEeYq2Av8LqtFu6aC7yMINhHw5ICV+r5rsIRnbwBTeXbYAQ9HI3xZZ\n" +
            "gL//fxSkNnWKQK1JdMRmzVSq9kPvkyOF56YHNW5t6YAmOrEkNBEcl2x8mtRUl9Wy\n" +
            "EQTDNN63uJSIPSQnDkbOuBBywbxmJtgcPfOliMn/FnrD8NwO";

    /** @deprecated this message was generated before the fix for bug #7859 and so encodes the ECDSA SignatureValue incorrectly (as ASN.1, rather than as raw r,s pair) */
    @Deprecated
    private static final String SIGNED_USING_SHA384_WITH_ECDSA =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/" +
            "wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-ws" +
            "security-utility-1.0.xsd\" actor=\"secure_span\" soapenv:mustUnderstand=\"1\"><wsu:Timestamp wsu:Id=\"Timestamp-2-47386e8ba38ee" +
            "990fff2b55c6eb65b8c\"><wsu:Created>2009-04-29T18:38:49.359Z</wsu:Created><wsu:Expires>2009-04-29T18:43:49.359Z</wsu:Expires></w" +
            "su:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-secu" +
            "rity-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu" +
            ":Id=\"BinarySecurityToken-0-1cb7b9c6383e08e83467f25de2c233ab\">MIIB2TCCAV6gAwIBAgIJAJCW+P9mQT2aMAoGCCqGSM49BAMDMBgxFjAUBgNVBAMM" +
            "DWVjY3Rlc3RjbGllbnQwHhcNMDkwNDI5MTgyODQ5WhcNMjkwNDI0MTgyODQ5WjAYMRYwFAYDVQQDDA1lY2N0ZXN0Y2xpZW50MHYwEAYHKoZIzj0CAQYFK4EEACIDYgA" +
            "EMl9nqqnkh4KdCMuVesojNkrJ4d0X+jvfyz3sKVvNm1jS1XmCH5TeZkzjeHGsIOstiS0yZdhFRdwgfuB1FaONHW31Vw+OxZdH0/wj4qs5RdF2MLBn8GfZWAd7LEps5s" +
            "INo3QwcjAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUBAf8ECDAGBgRVHSUAMB0GA1UdDgQWBBRn+VqVeSh2MgmUHblUzjkdOHG2CDAfBgNVHSMEG" +
            "DAWgBRn+VqVeSh2MgmUHblUzjkdOHG2CDAKBggqhkjOPQQDAwNpADBmAjEA9RBr66e/Ygzjd5GBpljgsYpi3Ht4hJcj79x7NNpJyL+b9mj6zy3NP4/RqS2yLpVpAjEA" +
            "+Rv3Vj8FFsIq/qS82Qi+98FZc6U9rZVBpDk3qJ9yZmQ9QotQbRncZayK72efFzQK</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www." +
            "w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:" +
            "CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384\"></ds:SignatureMeth" +
            "od><ds:Reference URI=\"#Body-1-70c6bfe2ac05e370eac521410ec6e054\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/20" +
            "01/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#sha384" +
            "\"></ds:DigestMethod><ds:DigestValue>BBbmAdQLiiyHnDEi12C659CYVN2q+wlcERpePTzP+N8OT6n9s41YnekZB3KzHIj2</ds:DigestValue></ds:Refe" +
            "rence><ds:Reference URI=\"#Timestamp-2-47386e8ba38ee990fff2b55c6eb65b8c\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w" +
            "3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-mor" +
            "e#sha384\"></ds:DigestMethod><ds:DigestValue>danM1MmilxAzDjAW1aV7ZXt2FmYa0JyhTMK8rVaft9yIHnPQqH6xUNxuJwt2jy/u</ds:DigestValue><" +
            "/ds:Reference></ds:SignedInfo><ds:SignatureValue>MGYCMQDC3K80d8XRHD30BpPGVuEm+ZlXg1HZw70LZtQgRqm+QP6oRpEgzWyEhsKk2RRuPIoCMQCCDm" +
            "SqELyqSDStFAG9A+idVK9jQjrWXw4Sl19S2Yhq4qWhDgSrWp8GtIlN4o4q6w4=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wss" +
            "e:Reference URI=\"#BinarySecurityToken-0-1cb7b9c6383e08e83467f25de2c233ab\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-x509-token-profile-1.0#X509v3\"></wsse:Reference></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></w" +
            "sse:Security></soapenv:Header><soapenv:Body xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-util" +
            "ity-1.0.xsd\" wsu:Id=\"Body-1-70c6bfe2ac05e370eac521410ec6e054\"><ns1:placeOrder xmlns:ns1=\"http://warehouse.acme.com/ws\" soa" +
            "penv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><productid xsi:type=\"xsd:long\">-9206260647417300294</product" +
            "id><amount xsi:type=\"xsd:long\">1</amount><price xsi:type=\"xsd:float\">5.0</price><accountid xsi:type=\"xsd:long\">228</accou" +
            "ntid></ns1:placeOrder></soapenv:Body></soapenv:Envelope>";

    private static final String B7580_WSSC_IDENT = "http://www.layer7tech.com/uuid/ed0cc365c1d4f1b06d82e2db7872e65cadca160a";
    private static final byte[] B7580_WSSC_SHARED_SECRET = { -87,32,-79,-40,108,-31,-31,-66,-80,103,114,4,104,-43,54,-94 };
    private static final byte[] B7580_WSSC_DERIVED_WITH_LUNA = { 59,8,20,97,-128,-28,68,-5,-48,90,-81,98,82,-90,-64,75 };
    private static final String B7580_WSSC_REQ =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:" +
            "xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Header><wsse:Security actor=\"secure_span\" soap:mustUnderstand=\"1\" xmlns:" +
            "wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-wssecurity-utility-1.0.xsd\"><wsu:Timestamp wsu:Id=\"Timestamp-1-d793dfd7097cc37a5883b5a0dd76867e\"><wsu:Created>" +
            "2009-08-11T20:16:52.924341377Z</wsu:Created><wsu:Expires>2009-08-11T20:21:52.924Z</wsu:Expires></wsu:Timestamp><wssc:SecurityContextToken xmlns:" +
            "wssc=\"http://schemas.xmlsoap.org/ws/2004/04/sc\"><wssc:Identifier>http://www.layer7tech.com/uuid/ed0cc365c1d4f1b06d82e2db7872e65cadca160a</wssc:" +
            "Identifier></wssc:SecurityContextToken><wssc:DerivedKeyToken wssc:Algorithm=\"http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk/p_sha1\" wsu:" +
            "Id=\"DerivedKey-Sig-0-ccac801e6ea98d5d30d8e88bd96e5ac5\" xmlns:wssc=\"http://schemas.xmlsoap.org/ws/2004/04/sc\"><wsse:SecurityTokenReference><wsse:" +
            "Reference URI=\"http://www.layer7tech.com/uuid/ed0cc365c1d4f1b06d82e2db7872e65cadca160a\" ValueType=\"http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct\"/>" +
            "</wsse:SecurityTokenReference><wssc:Generation>0</wssc:Generation><wssc:Length>16</wssc:Length><wssc:Label>WS-SecureConversation</wssc:Label><wsse:" +
            "Nonce>Ok+XTDkuVaBxWKwGiV3uBQ==</wsse:Nonce></wssc:DerivedKeyToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algor" +
            "ithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"/><ds:Reference URI" +
            "=\"#Timestamp-1-d793dfd7097cc37a5883b5a0dd76867e\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:" +
            "DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>8vmhbtctYxxRx8V0lfrLYS2wR4M=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:" +
            "SignatureValue>c0xOexnaLgiErN565l1j42rulw4=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-" +
            "200401-wss-wssecurity-secext-1.0.xsd\"><wsse:Reference URI=\"#DerivedKey-Sig-0-ccac801e6ea98d5d30d8e88bd96e5ac5\" ValueType=\"http://schemas.xmlsoap.org/ws/2004/04" +
            "/security/sc/dk\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soap:Header><soap:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"><" +
            "delay>0</delay></listProducts></soap:Body></soap:Envelope>";

    private static final String ECDSA_EXAMPLE_FROM_MILTON_GD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:BinarySecurityToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"CertId-1BD8AC45F3134AF165125555972903310\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">MIIBVDCB26ADAgECAgIE0jAKBggqhkjOPQQDAzAUMRIwEAYDVQQDEwlUZXN0IHRlc3QwHhcNMDkxMDE0MjIzNDM4WhcNMDkxMTEzMjIzNTI4WjAWMRQwEgYDVQQDEwtNaWx0b24gdGVzdDB2MBAGByqGSM49AgEGBSuBBAAiA2IABCUrgO1fN22cSMKU0B0wfY179NelsKPozAJSwjkgjhEEDSNWB+7MuLnXgu+oJ45V/4+eNH0HxqeoXJs+KER2pRkE08ALv1vKeFUkwWie6b7zM2Lc7MnO7Wk+hfccbTBovDAKBggqhkjOPQQDAwNoADBlAjBVLFIv45yIBkrYXbvelm/nou0Zeha89ps+G4ytwlV5bFIi6oxNML281URBz5auejQCMQDYGIuGvaOmGZZAV/klAVs+pWnOYKnGfffHYf/O5uRd9sQWUZrilT/UssjNT0Dvbx4=</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Signature-5\">\n" +
            "<ds:SignedInfo>\n" +
            "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384\"/>\n" +
            "<ds:Reference URI=\"#id-6\">\n" +
            "<ds:Transforms>\n" +
            "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
            "</ds:Transforms>\n" +
            "<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#sha384\"/>\n" +
            "<ds:DigestValue>NLlgN3qzbmWAKbqX+++kVnEmPn4C5/iKkCcn5iJ7yKUaszqhom2sbOzeweAadafO</ds:DigestValue>\n" +
            "</ds:Reference>\n" +
            "</ds:SignedInfo>\n" +
            "<ds:SignatureValue>\n" +
            "L/O5rFENm/vLGC5OtubBBvPJEMJuXefB/kJC8FE/8oHzlSzR7C8jpL8SCLS+U5u+kqOHh6yqmcLU\n" +
            "Tgp+rPo/vYdAKl+02CU9zPDXRfeMNb3dH/p0YujzgOwbbIEQZfaj\n" +
            "</ds:SignatureValue>\n" +
            "<ds:KeyInfo Id=\"KeyId-1BD8AC45F3134AF165125555972903311\">\n" +
            "<wsse:SecurityTokenReference xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"STRId-1BD8AC45F3134AF165125555972903312\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:Reference URI=\"#CertId-1BD8AC45F3134AF165125555972903310\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"/></wsse:SecurityTokenReference>\n" +
            "</ds:KeyInfo>\n" +
            "</ds:Signature></wsse:Security></S:Header><S:Body xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"id-6\"><ns2:buyHatResponse xmlns:ns2=\"http://hats/\"><return>test</return></ns2:buyHatResponse></S:Body></S:Envelope>";

    private static final String RECURSIVE_ENCRYPTED_KEYS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
            "    xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
            "    xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"\n" +
            "    xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
            ">\n" +
            "    <soapenv:Header>\n" +
            "        <wsse:Security soapenv:mustUnderstand=\"1\">\n" +
            "            <xenc:EncryptedKey Id=\"EncryptedKey-1\">\n" +
            "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "                <dsig:KeyInfo>\n" +
            "                    <wsse:SecurityTokenReference>\n" +
            "                        <wsse:Reference URI=\"EncryptedKey-2\" ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey\"/>\n" +
            "                    </wsse:SecurityTokenReference>\n" +
            "                </dsig:KeyInfo>\n" +
            "                <xenc:CipherData>\n" +
            "                    <xenc:CipherValue>SWVtMUz5+92Scmwtc28IUo2nZG14dyitl0CGiiIiHUIt1wTHSb0Opi/jD73hEr88rCMnOiNd+H8FfN+ivn2xK4tJvSPzktKZw0jpX3iaOfCVB+m9BjdB3Mkdesc2vfb0B0l5x6bFN7QihCZI89ViEAFHQizhnar/iPAiNKixlvQ=</xenc:CipherValue>\n" +
            "                </xenc:CipherData>\n" +
            "            </xenc:EncryptedKey>\n" +
            "            <xenc:EncryptedKey Id=\"EncryptedKey-2\">\n" +
            "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "                <dsig:KeyInfo>\n" +
            "                    <wsse:SecurityTokenReference>\n" +
            "                        <wsse:Reference URI=\"EncryptedKey-1\" ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey\"/>\n" +
            "                    </wsse:SecurityTokenReference>\n" +
            "                </dsig:KeyInfo>\n" +
            "                <xenc:CipherData>\n" +
            "                    <xenc:CipherValue>SWVtMUz5+92Scmwtc28IUo2nZG14dyitl0CGiiIiHUIt1wTHSb0Opi/jD73hEr88rCMnOiNd+H8FfN+ivn2xK4tJvSPzktKZw0jpX3iaOfCVB+m9BjdB3Mkdesc2vfb0B0l5x6bFN7QihCZI89ViEAFHQizhnar/iPAiNKixlvQ=</xenc:CipherValue>\n" +
            "                </xenc:CipherData>\n" +
            "            </xenc:EncryptedKey>\n" +
            "        </wsse:Security>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
