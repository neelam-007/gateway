package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.message.SecurityKnob;
import com.l7tech.security.WsiBSPValidator;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.decorator.WssDecoratorUtils;
import com.l7tech.security.xml.processor.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.xpath.*;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.security.xml.KeyInfoInclusionType.*;
import static org.junit.Assert.*;

/**
 * Decorate messages with WssDecorator and then send them through WssProcessor
 */
public class WssRoundTripTest {
    private static Logger log = Logger.getLogger(WssRoundTripTest.class.getName());
    private static final WsiBSPValidator validator = new WsiBSPValidator();

    static {
        SyspropUtil.setProperty(WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS, "true");
        SyspropUtil.setProperty(SoapUtil.PROPERTY_DISCLOSE_ELEMENT_NAME_IN_WSU_ID, "true");
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS,
            SoapUtil.PROPERTY_DISCLOSE_ELEMENT_NAME_IN_WSU_ID
        );
    }

    private static class NamedTestDocument {
        String name;
        WssDecoratorTest.TestDocument td;

        private NamedTestDocument(String name, WssDecoratorTest.TestDocument td) {
            this.name = name;
            this.td = td;
        }
    }

    WssDecoratorTest wssDecoratorTest = new WssDecoratorTest();

    @Test
    public void testSimple() throws Exception {
        runRoundTripTest(new NamedTestDocument("Simple",
                                               wssDecoratorTest.getSimpleTestDocument()));
    }

    @Test
    public void testSignedAndEncryptedUsernameToken() throws Exception {
        doTestSignedAndEncryptedUsernameToken(null);
    }

    @Test
    public void testSignedAndEncryptedUsernameToken_withKeyCache() throws Exception {
        doTestSignedAndEncryptedUsernameToken(new SimpleSecurityTokenResolver());
    }

    @Test
    public void testSignedUsernameToken() throws Exception {
        runRoundTripTest(new NamedTestDocument("signedUsernameToken", wssDecoratorTest.getSignedUsernameTokenTestDocument()), null);
    }

    public void doTestSignedAndEncryptedUsernameToken( SecurityTokenResolver securityTokenResolver) throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("Signed and Encrypted UsernameToken",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());
        ntd.td.securityTokenResolver = securityTokenResolver;
        EncryptedKey[] ekh = new EncryptedKey[1];
        runRoundTripTest(ntd, null, ekh);
        EncryptedKey encryptedKey = ekh[0];
        assertNotNull(encryptedKey);

        NamedTestDocument ntd2 = new NamedTestDocument("Signed and Encrypted UsernameToken (2)",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());

        // Make a second request using the same encrypted key and token resolver
        ntd2.td.securityTokenResolver = ntd.td.securityTokenResolver;
        ntd2.td.req.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref(encryptedKey.getEncryptedKeySHA1()));

        DecorationRequirements reqs = ntd2.td.req;
        reqs.setEncryptedKey(encryptedKey.getSecretKey());
        reqs.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref(encryptedKey.getEncryptedKeySHA1()));

        Document doc = ntd2.td.c.message;
        new WssDecoratorImpl().decorateMessage(new Message(doc), reqs);

        // Now try processing it
        Document doc1 = XmlUtil.stringToDocument(XmlUtil.nodeToString(doc));
        Message req = new Message(doc1);

        log.info("SECOND decorated request *Pretty-Printed*: " + XmlUtil.nodeToFormattedString(doc));

        WrapSSTR strr = new WrapSSTR(ntd2.td.req.getRecipientCertificate(), ntd2.td.recipientKey, ntd2.td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{ntd2.td.req.getSenderMessageSigningCertificate()});
        ProcessorResult r = new WssProcessorImpl().undecorateMessage(req,
                null,
                                                                     strr);

        UsernameToken usernameToken = findUsernameToken(r);
        WssDecoratorTest.TestDocument td = ntd2.td;
        if (td.req.isSignUsernameToken()) {
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("wsse", SoapUtil.SECURITY_NAMESPACE);
            ProcessorResultUtil.SearchResult foo = ProcessorResultUtil.searchInResult(log, doc1,
                    new DomCompiledXpath(new XpathExpression("//wsse:UsernameToken", ns)), null, false, r.getElementsThatWereSigned(), "signed");
            if (securityTokenResolver == null)
                assertEquals( (long) foo.getResultCode(), (long) ProcessorResultUtil.FALSIFIED );
            else
                assertEquals( (long) foo.getResultCode(), (long) ProcessorResultUtil.NO_ERROR );
        }

        if (securityTokenResolver == null) {
            // Second request should have included no EncryptedKey, and hence no signed timestamp
            for (XmlSecurityToken toke : r.getXmlSecurityTokens())
                if (toke instanceof EncryptedKey)
                    fail("Second request included an EncryptedKey");
            assertTrue(ProcessorResultUtil.getParsedElementsForNode(r.getTimestamp()==null ? null : r.getTimestamp().asElement(), r.getElementsThatWereSigned()).isEmpty());
        } else {
            // If timestamp was supposed to be signed, make sure it actually was
            EncryptedKey[] ekOut2 = new EncryptedKey[1];
            SigningSecurityToken timestampSigner = checkTimestampSignature(td, r, ekOut2);
            checkEncryptedUsernameToken(td, usernameToken, r, timestampSigner);
        }
    }


    @Test
    @BugNumber(9802)
    public void testSignedAndEncryptedUsernameTokenWithEncryptedSignature() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedAndEncryptedUsernameTokenWithEncryptedSignature",
                wssDecoratorTest.getSignedAndEncryptedUsernameTokenWithEncryptedSignatureTestDocument()), null);
    }

    @Test
    public void testEncryptedBodySignedEnvelope() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedBodySignedEnvelope",
                                               wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument()));
    }

    @Test
    public void testSignedEnvelope() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedEnvelope",
                                               wssDecoratorTest.getSignedEnvelopeTestDocument()));
    }

    @Test
    public void testEncryptionOnlyAES128() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES128",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument( XencAlgorithm.AES_128_CBC.getXEncName())));
    }

    @Test
    public void testEncryptionOnlyAES192() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES192",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_192_CBC.getXEncName())),
                         null);
    }

    @Test
    public void testEncryptionOnlyAES256() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES256",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_256_CBC.getXEncName())));
    }

    @Test
    public void testEncryptionOnlyTripleDES() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyTripleDES",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.TRIPLE_DES_CBC.getXEncName())));
    }

    @Test
    public void testEncryptionKeyReferences() throws Exception {
        final Collection<KeyInfoInclusionType> keyReferenceTypes = EnumSet.of( CERT, STR_SKI, ISSUER_SERIAL, KEY_NAME );
        CollectionUtils.foreach( keyReferenceTypes, false, new Functions.UnaryVoidThrows<KeyInfoInclusionType, Exception>() {
            @Override
            public void call( final KeyInfoInclusionType keyInfoInclusionType ) throws Exception {
                final NamedTestDocument testDocument = new NamedTestDocument( "EncryptionOnlyAES128",
                        wssDecoratorTest.getEncryptionOnlyTestDocument( XencAlgorithm.AES_128_CBC.getXEncName() ) );
                testDocument.td.req.setEncryptionKeyInfoInclusionType( keyInfoInclusionType );
                final String decoratedRequest = runRoundTripTest( testDocument, null );
                final Document document = XmlUtil.parse( decoratedRequest );
                final Element header = SoapUtil.getSecurityElementForL7( document );
                assertNotNull( "No security header found", header );

                final Element encryptedKeyElement = DomUtils.findExactlyOneChildElementByName( header, SoapUtil.XMLENC_NS, SoapUtil.ENCRYPTEDKEY_EL_NAME );
                final Element keyInfoElement = DomUtils.findOnlyOneChildElementByName( encryptedKeyElement, SoapUtil.DIGSIG_URI, SoapUtil.KINFO_EL_NAME );

                final IdAttributeConfig idConfig = IdAttributeConfig.fromString( "{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Id" );
                final Map<String,Element> identifierMap = DomUtils.getElementByIdMap( document, idConfig );

                final X509Certificate bobCert = TestDocuments.getWssInteropBobCert();
                final ContextualSecurityTokenResolver resolver = new ContextualSecurityTokenResolver.Support.DelegatingContextualSecurityTokenResolver(
                        new SimpleSecurityTokenResolver( bobCert )){
                    @Override
                    public X509Certificate lookupByIdentifier( final String identifier ) {
                        assertTrue(  "BST element present", identifierMap.keySet().contains( identifier ) );
                        return bobCert;
                    }
                };
                final KeyInfoElement element = KeyInfoElement.parse( keyInfoElement, resolver, EnumSet.allOf(KeyInfoInclusionType.class) );
                assertEquals( "Reference type", keyInfoInclusionType, element.getKeyInfoInclusionType() );
            }
        } );
    }

    @Test
    public void testSigningOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly",
                                               wssDecoratorTest.getSigningOnlyTestDocument()));
    }

    @Test
    public void testSigningOnly_dsa_sha1() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_dsa_sha1",
                                               wssDecoratorTest.getSigningOnly_dsa_sha1_TestDocument()));
    }

    @Test
    public void testSigningOnly_dsa_sha1_sha256References() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_dsa_sha1_sha256References",
                                               wssDecoratorTest.getSigningOnly_dsa_sha1_sha256References_TestDocument()),
                         xpathVerifier(
                                 "1=count(/soapenv:Envelope/soapenv:Header/wsse:Security/ds:Signature/ds:SignedInfo/ds:SignatureMethod[@Algorithm = 'http://www.w3.org/2000/09/xmldsig#dsa-sha1'])",
                                 "2=count(/soapenv:Envelope/soapenv:Header/wsse:Security/ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod[@Algorithm = 'http://www.w3.org/2001/04/xmlenc#sha256'])"
                                 ));
    }

    @Ignore("Fails because we currently do not support sha256 with DSA and fall back to sha1 instead (which signs and verifies ok, but fails the post-check for SHA-256)")
    @Test
    public void testSigningOnly_dsa_sha256() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_dsa_sha256",
                                               wssDecoratorTest.getSigningOnly_dsa_sha256_TestDocument()));
    }

    @Test
    public void testSigningOnly_rsa_sha1() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_rsa_sha1",
                                               wssDecoratorTest.getSigningOnly_rsa_sha1_TestDocument()));
    }

    @Test
    public void testSigningOnly_rsa_sha256() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_rsa_sha256",
                                               wssDecoratorTest.getSigningOnly_rsa_sha256_TestDocument()));
    }

    @Test
    public void testSigningOnly_rsa_sha384() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_rsa_sha384",
                                               wssDecoratorTest.getSigningOnly_rsa_sha384_TestDocument()));
    }

    @Test
    public void testSigningOnly_rsa_sha512() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_rsa_sha512",
                                               wssDecoratorTest.getSigningOnly_rsa_sha512_TestDocument()));
    }

    @Test
    public void testSigningOnly_ec_sha1() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_ec_sha1",
                                               wssDecoratorTest.getSigningOnly_ec_sha1_TestDocument()));
    }

    @Test
    public void testSigningOnly_ec_sha256() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_ec_sha256",
                                               wssDecoratorTest.getSigningOnly_ec_sha256_TestDocument()));
    }

    @Test
    public void testSigningOnly_ec_sha384() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_ec_sha384",
                                               wssDecoratorTest.getSigningOnly_ec_sha384_TestDocument()));
    }

    @Test
    public void testSigningOnly_ec_sha512() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly_ec_sha512",
                                               wssDecoratorTest.getSigningOnly_ec_sha512_TestDocument()));
    }

    @Test
    public void testSigningOnlyWithProtectTokens() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnlyWithProtectTokens",
                                               wssDecoratorTest.getSigningOnlyWithProtectTokensTestDocument()));
    }

    @Test
    public void testSigningProtectTokenNoBst() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningProtectTokenNoBst",
                wssDecoratorTest.getSigningProtectTokenNoBstTestDocument()));
    }

    @Test
    @BugNumber(11320)
    public void testAes128GcmEncryptionOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("Aes128GcmEncryptionOnly", wssDecoratorTest.getAes128GcmEncryptionOnlyTestDocument()), null);
    }

    @Test
    @BugNumber(11320)
    public void testAes256GcmEncryptionOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("Aes256GcmEncryptionOnly", wssDecoratorTest.getAes256GcmEncryptionOnlyTestDocument()), null);
    }

    @Test
    public void testSingleSignatureMultipleEncryption() throws Exception {
        runRoundTripTest(new NamedTestDocument("SingleSignatureMultipleEncryption",
                                               wssDecoratorTest.getSingleSignatureMultipleEncryptionTestDocument()));
    }

    @Test
    @Ignore("we no longer support this")
    public void testWrappedSecurityHeader() throws Exception {
        runRoundTripTest(new NamedTestDocument("WrappedSecurityHeader",
                                               wssDecoratorTest.getWrappedSecurityHeaderTestDocument()));
    }

    @Test
    public void testSkilessRecipientCert() throws Exception {
        runRoundTripTest(new NamedTestDocument("SkilessRecipientCert",
                                               wssDecoratorTest.getSkilessRecipientCertTestDocument()));
    }

    @Test
    public void testSigningOnlyWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnlyWithSecureConversation",
                                               wssDecoratorTest.getSigningOnlyWithSecureConversationTestDocument()),
                         null);
    }

    @Test
    public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument()),
                         null);
    }

    @Test
    @BugNumber(9687)
    public void testSigningWithSecureConversation_protectTokens_emptySignList() throws Exception {
        final NamedTestDocument ntd = new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument());
        // As of Bug #9687 fix, protectTokens should force a signature even if elementsToSign is empty.
        ntd.td.req.setProtectTokens(true);
        ntd.td.req.setSignTimestamp(false);
        ntd.td.req.getElementsToEncrypt().clear();
        ntd.td.req.getElementsToSign().clear();
        runRoundTripTest(ntd, null);
    }

    @Test
    public void testSigningWithSecureConversation2005WithSct() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSigningWithSecureConversation2005WithSct",
                wssDecoratorTest.getSigningWithSecureConversation2005WithSctTestDocument()),
                null);
    }

    @Test
    public void testSigningWithSecureConversation2004WithoutSct() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSigningWithSecureConversation2004WithoutSct",
                wssDecoratorTest.getSigningWithSecureConversation2004WithoutSctTestDocument()),
                null);
    }

    @Test
    public void testSigningWithSecureConversation2005WithoutSct() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSigningWithSecureConversation2005WithoutSct",
                wssDecoratorTest.getSigningWithSecureConversation2005WithoutSctTestDocument()),
                null);
    }

    @Test
    public void testSigningAndEncryptionWithSecureConversationWss11() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument());
        ntd.td.req.addSignatureConfirmation("abc11SignatureConfirmationValue11blahblahblah11==");
        runRoundTripTest(ntd,
                         null);
    }

    @Test
    public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlHolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument(1)),
                         null);
    }

    @Test
    public void testSignedSamlSenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlSenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument(1)),
                         null);
    }

    @Test
    public void testSignedSaml2HolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSaml2HolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument(2)),
                         null);
    }

    @Test
    public void testSignedSaml2SenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSaml2SenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument(2)),
                         null);
    }

    @Test
    public void testSignedEmptyElement() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedEmptyElement",
                                               wssDecoratorTest.getSignedEmptyElementTestDocument()));
    }

    @Test
    public void testEncryptedEmptyElement() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedEmptyElement",
                                               wssDecoratorTest.getEncryptedEmptyElementTestDocument()));
    }

    @Test
    @BugNumber(11191)
    public void testSignedEncryptedEmptyElement() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedEncryptedEmptyElement",
                                               wssDecoratorTest.getSignedEncryptedEmptyElementTestDocument()));
    }

    @Test
    public void testSoapWithUnsignedAttachment() throws Exception {
        runRoundTripTest(new NamedTestDocument("SoapWithUnsignedAttachment",
                                               wssDecoratorTest.getSoapWithUnsignedAttachmentTestDocument()));
    }

    @Test
    public void testSoapWithSignedAttachmentContent() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("SoapWithSignedAttachmentContent",
                                               wssDecoratorTest.getSoapWithSignedAttachmentTestDocument());

        ntd.td.req.setSignPartHeaders(false);
        final Set<String> partsToSign = ntd.td.req.getPartsToSign();
        partsToSign.clear();
        partsToSign.add("-76392836.13454");

        String result = runRoundTripTest(ntd, null);

        assertTrue("Use of correct transform", result.contains(SoapUtil.TRANSFORM_ATTACHMENT_CONTENT));
    }

    @Test
    public void testSoapWithSignedAttachmentComplete() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("SoapWithSignedAttachmentContentAndMIMEHeaders",
                                               wssDecoratorTest.getSoapWithSignedAttachmentTestDocument());

        ntd.td.req.setSignPartHeaders(true);
        ntd.td.req.getPartsToSign().add("-76392836.13454");

        String result = runRoundTripTest(ntd, null);

        assertTrue("Use of correct transform", result.contains(SoapUtil.TRANSFORM_ATTACHMENT_COMPLETE));
    }

    @Test
    public void testSoapWithSignedEncryptedAttachment() throws Exception {
        runRoundTripTest(new NamedTestDocument("SoapWithSignedEncryptedAttachment",
                                               wssDecoratorTest.getSoapWithSignedEncryptedAttachmentTestDocument()));
    }

    @Test
    public void testSignedAndEncryptedBodyWithSki() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedAndEncryptedBodyWithNoBst",
                                               wssDecoratorTest.getSignedAndEncryptedBodySkiTestDocument()));
    }

    @Test
    public void testEncryptedUsernameToken() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedUsernameToken",
                                               wssDecoratorTest.getEncryptedUsernameTokenTestDocument()),
                                               null);
    }

    @Test
    public void testEncryptedUsernameTokenWithDerivedKeys() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedUsernameTokenWithDerivedKeys",
                                               wssDecoratorTest.getEncryptedUsernameTokenWithDerivedKeysTestDocument()),
                                               null);
    }

    @Test
    public void testOaepEncryptedKey() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedKeyAlgorithm",
                                               wssDecoratorTest.getOaepKeyEncryptionTestDocument()),
                         null);
    }

    @Test
    public void testConfigSecHdrAttributesTestDocument() throws Exception {
        runRoundTripTest(new NamedTestDocument("ConfigSecHdrAttributesTestDocument", wssDecoratorTest.getConfigSecHdrAttributesTestDocument()));
    }

    @Test
    public void testExplicitSignatureConfirmation() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("ExplicitSignatureConfirmation",
            wssDecoratorTest.getExplicitSignatureConfirmationsTestDocument());
        runRoundTripTest(ntd, null);
    }

    @Test
    public void testSuiteBCryptoSignatureSha1() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSuiteBCryptoSignature(ECDSA-SHA384", wssDecoratorTest.getSuiteBSigningTestDocument("SHA-1")));
    }

    @Test
    public void testSuiteBCryptoSignatureSha256() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSuiteBCryptoSignature(ECDSA-SHA384", wssDecoratorTest.getSuiteBSigningTestDocument("SHA-384")));
    }

    @Test
    public void testSuiteBCryptoSignatureSha384() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSuiteBCryptoSignature(ECDSA-SHA256", wssDecoratorTest.getSuiteBSigningTestDocument("SHA-256")));
    }

    @Test
    public void testSuiteBCryptoSignatureSha384_2() throws Exception {
        runRoundTripTest(new NamedTestDocument("testSuiteBCryptoSignature(ECDSA-SHA384", wssDecoratorTest.getSuiteBSigningTestDocument("SHA-384")));
    }

    private Functions.Unary<Boolean,Document> wsiBspVerifier() {
        return new Functions.Unary<Boolean,Document>(){
            @Override
            public Boolean call( final Document document ) {
                return validator.isValid( document );
            }
        };
    }

    /**
     * Create a verifier for the given XPath expressions.
     *
     * The given expressions should evaluate to true or false to indicate
     * success or failure.
     */
    private Functions.Unary<Boolean,Document> xpathVerifier( final String... xpaths ) {
        return new Functions.Unary<Boolean,Document>(){
            @Override
            public Boolean call( final Document document ) {
                try {
                    final XPathFactory factory = XPathFactory.newInstance( XPathFactory.DEFAULT_OBJECT_MODEL_URI );
                    final NamespaceContextImpl namespaces = new NamespaceContextImpl(XmlUtil.findAllNamespaces( document.getDocumentElement() ));
                    final XPath xpath = factory.newXPath();
                    xpath.setNamespaceContext( namespaces );

                    boolean verified = true;
                    for ( final String xpathExpr : xpaths ) {
                        final boolean xpathVerified =
                                (Boolean) xpath.evaluate( xpathExpr, document, XPathConstants.BOOLEAN );
                        if ( !xpathVerified ) {
                            System.out.println( "Verification xpath failed: " + xpathExpr );
                        }
                        verified = verified && xpathVerified;
                    }

                    return verified;
                } catch ( XPathFactoryConfigurationException e ) {
                    e.printStackTrace();
                } catch ( XPathExpressionException e ) {
                    e.printStackTrace();
                }
                return false;
            }
        };
    }


    private void runRoundTripTest( final NamedTestDocument ntd ) throws Exception {
        runRoundTripTest(ntd, wsiBspVerifier());
    }

    // @return  the decorated request, in case you want to test replaying it
    private String runRoundTripTest( final NamedTestDocument ntd,
                                     final Functions.Unary<Boolean,Document> verifier ) throws Exception {
        return runRoundTripTest(ntd, verifier, null);
    }

    /**
     *
     * @param ntd The test document to use
     * @param verifier A verifier to run against the decorated message
     * @param ekOut holder for any encrypted key
     * @return the decorated request, in case you want to test replaying it
     * @throws Exception
     */
    private String runRoundTripTest( final NamedTestDocument ntd,
                                     final Functions.Unary<Boolean,Document> verifier,
                                     final EncryptedKey[] ekOut) throws Exception {
        log.info("Running round-trip test on test document: " + ntd.name);
        final WssDecoratorTest.TestDocument td = ntd.td;
        WssDecoratorTest.Context c = td.c;
        Message message = c.messageMessage;
        Document soapMessage = message.getXmlKnob().getDocumentReadOnly();

        WssDecorator martha = new WssDecoratorImpl();
        WssProcessor trogdor = new WssProcessorImpl();

        // save a record of the literal encrypted elements before they get messed with
        final Set<Element> elementSet = td.req.getElementsToEncrypt().keySet();
        final Element[] elementsToEncrypt = elementSet.toArray(new Element[elementSet.size()]);
        Element[] elementsBeforeEncryption = new Element[elementsToEncrypt.length];
        for (int i = 0; i < elementsToEncrypt.length; i++) {
            Element element = elementsToEncrypt[i];
            log.info("Saving canonicalizing copy of " + element.getLocalName() + " before it gets encrypted");
            elementsBeforeEncryption[i] = (Element) element.cloneNode(true);
        }

        log.info("Message before decoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(soapMessage));
        DecorationRequirements reqs = td.req;

        martha.decorateMessage(c.messageMessage, reqs);

        log.info("Decorated message (reformatted):\n\n" + XmlUtil.nodeToFormattedString(c.messageMessage.getXmlKnob().getDocumentReadOnly()));
        schemaValidateSamlAssertions(soapMessage);

        // Serialize to string to simulate network transport
        byte[] decoratedMessageDocument = XmlUtil.nodeToString(soapMessage).getBytes();
        byte[] decoratedMessage = IOUtils.slurpStream(c.messageMessage.getMimeKnob().getEntireMessageBodyAsInputStream());


        // prepare fake request and wss result, to accomodate the signature confirmation expectations
        final Message fakeRequest = new Message();
        SecurityKnob fakeSK = fakeRequest.getSecurityKnob();
        fakeSK.addDecorationResult(new WssDecorator.DecorationResult() {
            @Override
            public String getEncryptedKeySha1() { return null; }

            @Override
            public SecretKey getEncryptedKeySecretKey() { return null; }

            @Override
            public String getWsscSecurityContextId() { return null; }

            @Override
            public SecurityContext getWsscSecurityContext() { return null; }

            @Override
            public Map<String, Boolean> getSignatures() {
                Map<String, Boolean> signatures = new HashMap<String, Boolean>();
                for(String fakeSignature : td.req.getSignatureConfirmations())
                    if (fakeSignature != null) // null means "no signatures confirmed"
                        signatures.put(fakeSignature, false);
                return signatures;
            }

            @Override
            public Set<String> getEncryptedSignatureValues() {
                return new HashSet<String>();
            }

            @Override
            public String getSecurityHeaderActor() {
                return td.req.getSecurityHeaderActor();
            }

            @Override
            public void setSecurityHeaderActor(String newActor) {
                throw new UnsupportedOperationException(); 
            }
        });

        // ... pretend HTTP goes here ...
        final String networkRequestString = new String(decoratedMessageDocument);

        // Ooh, an incoming message has just arrived!
        Message incomingMessage = new Message();
        incomingMessage.initialize(c.messageMessage.getMimeKnob().getOuterContentType(), decoratedMessage);
        incomingMessage.notifyMessage(fakeRequest, MessageRole.REQUEST);
        Document incomingSoapDocument = incomingMessage.getXmlKnob().getDocumentReadOnly();

        boolean isValid = verifier==null || verifier.call(incomingSoapDocument);

        assertTrue("Serialization did not affect the integrity of the XML message",
                   XmlUtil.nodeToString(soapMessage).equals(XmlUtil.nodeToString(XmlUtil.stringToDocument(networkRequestString))));

        WrapSSTR strr = new WrapSSTR(td.req.getRecipientCertificate(), td.recipientKey, td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{td.req.getSenderMessageSigningCertificate()});
        ProcessorResult r = trogdor.undecorateMessage(incomingMessage,
                makeSecurityContextFinder(td.req.getSecureConversationSession()),
                                                      strr);

        final Element processedSecurityHeader = SoapUtil.getSecurityElement(incomingSoapDocument, r.getProcessedActorUri());

        log.info("After undecoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(incomingSoapDocument));

        ParsedElement[] encrypted = r.getElementsThatWereEncrypted();
        assertNotNull(encrypted);
        SignedElement[] signed = r.getElementsThatWereSigned();
        assertNotNull(signed);

        List<Element> signedDomElements = Functions.map(Arrays.asList(r.getElementsThatWereSigned()),
                Functions.<Element, SignedElement>getterTransform(SignedElement.class.getMethod("asElement")));

        // Output security tokens
        UsernameToken usernameToken = findUsernameToken(r);

        if (td.req.isSignUsernameToken()) {
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("wsse", SoapUtil.SECURITY_NAMESPACE);
            ProcessorResultUtil.SearchResult foo = ProcessorResultUtil.searchInResult(log, incomingSoapDocument,
                    new DomCompiledXpath(new XpathExpression("//wsse:UsernameToken", ns)), null, false, r.getElementsThatWereSigned(), "signed");
            assertEquals( (long) foo.getResultCode(), (long) ProcessorResultUtil.NO_ERROR );
        }

        // If timestamp was supposed to be signed, make sure it actually was
        SigningSecurityToken timestampSigner = checkTimestampSignature(td, r, ekOut);
        checkEncryptedUsernameToken(td, usernameToken, r, timestampSigner);

        // Make sure all requested elements were signed
        Element[] elementsToSign = td.req.getElementsToSign().toArray(new Element[td.req.getElementsToSign().size()]);
        for (Element elementToSign : elementsToSign) {
            boolean wasSigned = false;

            for (SignedElement aSigned : signed) {
                if (localNamePathMatches(elementToSign, aSigned.asElement())) {
                    wasSigned = true;
                    break;
                }
            }
            assertTrue("Element " + elementToSign.getLocalName() + " must be signed", wasSigned);
            log.info("Element " + elementToSign.getLocalName() + " verified as signed successfully.");
        }

        // Make sure all requested elements were encrypted
        for (Element elementToEncrypt : elementsBeforeEncryption) {
            log.info("Looking to ensure element was encrypted: " + XmlUtil.nodeToString(elementToEncrypt));

            boolean wasEncrypted = false;
            for (ParsedElement anEncrypted : encrypted) {
                Element encryptedElement = anEncrypted.asElement();

                log.info("Checking if element matches: " + XmlUtil.nodeToString(encryptedElement));
                if (localNamePathMatches(elementToEncrypt, encryptedElement)) {
                    /* You cant test this because there is a wsu:Id element added (maybe by the signature process)
                    assertEquals("Canonicalized original element " + encryptedElement.getLocalName() +
                                 " content must match canonicalized decrypted element content",
                                 canonicalized,
                                 canonicalize(encryptedElement));*/
                    wasEncrypted = true;
                    break;
                }
            }
            assertTrue("Element " + elementToEncrypt.getLocalName() + " must be encrypted or empty",
                       DomUtils.elementIsEmpty(elementToEncrypt) || wasEncrypted);
            log.info("Element " + elementToEncrypt.getLocalName() + " succesfully verified as either empty or encrypted.");
        }

        // If there were supposed to be SignatureConfirmation elements, make sure they are there
        SignatureConfirmation confirmation = r.getSignatureConfirmation();
        SignatureConfirmation.Status confirmationStatus = confirmation.getStatus();

        assertNotSame( Arrays.toString(confirmation.getErrors().toArray()), SignatureConfirmation.Status.INVALID, confirmationStatus);

        Set<String> signaturesInRequest = WssDecoratorUtils.getSignaturesDecorated(fakeRequest.getSecurityKnob(), td.req.getSecurityHeaderActor()).keySet();
        if (confirmationStatus == SignatureConfirmation.Status.CONFIRMED) {
            Set<String> gotConfirmationValues = confirmation.getConfirmedValues().keySet();
            for (String confirmedValue : gotConfirmationValues)
                assertTrue(signedDomElements.contains(confirmation.getElement(confirmedValue)));
            for (String expectedConfValue : signaturesInRequest)
                assertTrue("Expect SignatureConfirmation for: " + expectedConfValue, gotConfirmationValues.contains(expectedConfValue));
        } else  {
            assertTrue("No signatures from the request were confirmed: " + signaturesInRequest, signaturesInRequest.isEmpty());
        }

        if (td.req.isProtectTokens()) {
            for (SignedElement signedElement : signed) {
                final Element signingTokenEl = signedElement.getSigningSecurityToken().asElement();
                final boolean signingTokenElWasSigned = signedDomElements.contains(signingTokenEl);
                assertTrue("ProtectTokens implies that all signing tokens were signed", signingTokenElWasSigned);
            }

            // If a WS-SC SCT is included and protectTokens is set, the SCT shall be signed (Bug #9687)
            SecurityContextToken sct = findSecurityContextToken(r);
            if (sct != null) {
                assertTrue("SecurityContextToken shall always be signed if protectTokens is asserted", isElementSigned(sct.asElement(), r));
            }
        }

        if (td.req.isOmitSecurityContextToken()) {
            // Ensure no SCT is physically present in message (virtual XmlSecurityToken is OK)
            for (String wsscns : SoapConstants.WSSC_NAMESPACE_ARRAY) {
                assertTrue("No SCT shall be included if omitSecurityContextToken is asserted",
                        incomingSoapDocument.getElementsByTagNameNS(wsscns, "SecurityContextToken").getLength() == 0);
            }

            if (td.req.getSecureConversationSession() != null) {
                // Ensure virtual SCT is present
                boolean sawVirtualSct = false;
                for (XmlSecurityToken token : r.getXmlSecurityTokens()) {
                    if (token instanceof SecurityContextToken) {
                        SecurityContextToken sct = (SecurityContextToken) token;
                        Element elm = sct.asElement();
                        assertNull("A virtual SCT must not be present in the document", elm.getParentNode());
                        assertTrue("A virtual SCT should use a marker NS", elm.getNamespaceURI().contains("virtual"));
                        sawVirtualSct = true;
                    }
                }

                assertTrue("A virtual SCT must be present when a message was decorated with WSSC but with omitSecurityContextToken",
                        sawVirtualSct);
            }
        }

        if (td.req.isEncryptSignature()) {
            Map<Node,Boolean> encryptedSigs = new HashMap<Node, Boolean>();
            for (ParsedElement enc : encrypted) {
                final Element element = enc.asElement();
                if ("Signature".equals(element.getLocalName()) && SoapUtil.DIGSIG_URI.equals(element.getNamespaceURI())) {
                    encryptedSigs.put(element, true);
                }
            }

            NodeList allSigs = incomingSoapDocument.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
            for (int i = 0; i < allSigs.getLength(); ++i) {
                Node element = allSigs.item(i);
                assertTrue("Signature shall have been encrypted", Boolean.TRUE.equals(encryptedSigs.get(element)));
            }
        }

        // Ensure signature method and hash algorithm are those requested
        if (!td.req.getElementsToSign().isEmpty() && td.req.getSenderMessageSigningPrivateKey() != null && td.req.getSignatureMessageDigest() != null) {
            SupportedSignatureMethods sigmeth = SupportedSignatureMethods.fromKeyAndMessageDigest(td.req.getSenderMessageSigningPrivateKey().getAlgorithm(), td.req.getSignatureMessageDigest());
            String signatureUri = sigmeth.getAlgorithmIdentifier();
            String digestUri = td.req.getSignatureReferenceMessageDigest() == null
                ? SupportedDigestMethods.fromAlias(sigmeth.getDigestAlgorithmName()).getIdentifier()
                : SupportedDigestMethods.fromAlias(td.req.getSignatureReferenceMessageDigest()).getIdentifier();

            // We'll just do an extremely crude sanity check to rule out obvious failure modes (ignoring configured digest/falling back to SHA-1/not signing at all/etc)
            String decoratedMessageString = new String(decoratedMessage);
            assertTrue(decoratedMessageString.contains(signatureUri));
            assertTrue(decoratedMessageString.contains(digestUri));
        }

        // Check Security header actor and mustUnderstand
        if (reqs.getSecurityHeaderMustUnderstand() != null) {
            final String mustUnderstandValue = SoapUtil.getMustUnderstandAttributeValue(processedSecurityHeader);
            Boolean mustUnderstand = "1".equals(mustUnderstandValue) || Boolean.valueOf(mustUnderstandValue);
            assertEquals(mustUnderstand, reqs.getSecurityHeaderMustUnderstand());
        }

        if (reqs.getSecurityHeaderActor() != null) {
            assertEquals(reqs.getSecurityHeaderActor(), SoapUtil.getActorValue(processedSecurityHeader));
        }

        assertTrue("Verifier check failed.", isValid);

        return networkRequestString;
    }

    private UsernameToken findUsernameToken(ProcessorResult r) {
        XmlSecurityToken[] tokens = r.getXmlSecurityTokens();
        UsernameToken usernameToken = null;
        for (XmlSecurityToken token : tokens) {
            log.info("Got security token: " + token.getClass() + " type=" + token.getType());
            if (token instanceof UsernameToken) {
                if (usernameToken != null)
                    fail("More than one UsernameToken found in processor result");
                usernameToken = (UsernameToken) token;
            }
            if (token instanceof SigningSecurityToken) {
                SigningSecurityToken signingSecurityToken = (SigningSecurityToken) token;
                log.info("It's a signing security token.  possessionProved=" + signingSecurityToken.isPossessionProved());
                SignedElement[] signedElements = signingSecurityToken.getSignedElements();
                for (SignedElement signedElement : signedElements) {
                    log.info("It was used to sign: " + signedElement.asElement().getLocalName());
                }
            }
        }
        return usernameToken;
    }

    private SecurityContextToken findSecurityContextToken(ProcessorResult r) {
        XmlSecurityToken[] tokens = r.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof SecurityContextToken) {
                return (SecurityContextToken) token;
            }
        }
        return null;
    }

    private boolean isElementSigned(Element e, ProcessorResult r) {
        SignedElement[] signed = r.getElementsThatWereSigned();
        for (SignedElement signedElement : signed) {
            if (signedElement.asElement() == e)
                return true;
        }
        return false;
    }

    private void checkEncryptedUsernameToken(WssDecoratorTest.TestDocument td, UsernameToken usernameToken, ProcessorResult r, SigningSecurityToken timestampSigner) {
        if (td.req.isEncryptUsernameToken()) {
            assertNotNull(usernameToken);
            if (td.req.isSignUsernameToken())
                assertTrue(ProcessorResultUtil.nodeIsPresent(usernameToken.asElement(), r.getElementsThatWereSigned()));
            assertTrue(ProcessorResultUtil.nodeIsPresent(usernameToken.asElement(), r.getElementsThatWereEncrypted()));
            SigningSecurityToken[] utSigners = r.getSigningTokens(usernameToken.asElement());
            assertNotNull(utSigners);
            assertTrue(utSigners.length == 1);
            if (timestampSigner != null)
                assertTrue("Timestamp must have been signed by same token as signed the UsernameToken", utSigners[0] == timestampSigner);

            log.info("UsernameToken was verified as having been encrypted, and signed by the same token as was used to sign the timestamp");
        }
    }

    private SigningSecurityToken checkTimestampSignature(WssDecoratorTest.TestDocument td, ProcessorResult r, EncryptedKey[] ekOut) throws SAXException, InvalidDocumentFormatException, GeneralSecurityException {
        SigningSecurityToken timestampSigner = null;
        if (td.req.isSignTimestamp()) {
            WssTimestamp rts = r.getTimestamp();
            assertNotNull(rts);
            assertFalse("Timestamp was supposed to have been signed", ProcessorResultUtil.getParsedElementsForNode(r.getTimestamp().asElement(), r.getElementsThatWereSigned()).isEmpty());
            SigningSecurityToken[] signers = r.getSigningTokens(rts.asElement());
            assertNotNull(signers);
            // Martha can currently only produce messages with a single timestamp-covering signature in the default sec header
            assertTrue(signers.length == 1);
            SigningSecurityToken signer = timestampSigner = signers[0];
            assertTrue(signer.asElement() == null || signer.asElement().getOwnerDocument() == rts.asElement().getOwnerDocument());
            assertTrue(signer.isPossessionProved());

            if (signer instanceof SamlSecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((SamlSecurityToken)signer).getSubjectCertificate().equals(td.req.getSenderSamlToken().getSubjectCertificate()));
            } else if (signer instanceof X509SigningSecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((X509SigningSecurityToken)signer).getCertificate().equals(td.req.getSenderMessageSigningCertificate()));
            } else if (signer instanceof SecurityContextToken) {
                SecurityContextToken sct = (SecurityContextToken)signer;
                assertTrue("SecurityContextToken was supposed to have proven possession", sct.isPossessionProved());
                assertEquals("WS-Security session ID was supposed to match", sct.getContextIdentifier(), td.req.getSecureConversationSession().getId());
                assertTrue(Arrays.equals(sct.getSecurityContext().getSharedSecret(),
                                         td.req.getSecureConversationSession().getSecretKey()));
            } else if (signer instanceof EncryptedKey) {
                EncryptedKey ek = (EncryptedKey)signer;
                if (ekOut != null && ekOut.length > 0) ekOut[0] = ek;
                assertTrue("EncryptedKey signature source should always (trivially) have proven possession", ek.isPossessionProved());
                String eksha1 = ek.getEncryptedKeySHA1();
                log.info("EncryptedKey sha-1: " + eksha1);
                assertNotNull(eksha1);
                assertTrue(eksha1.trim().length() > 0);
                assertNotNull(ek.getSecretKey());
                assertEquals(SecurityTokenType.WSS_ENCRYPTEDKEY, ek.getType());
                assertTrue(Arrays.equals(ek.getSignedElements(), r.getElementsThatWereSigned()));
            } else
                fail("Timestamp was signed with unrecognized security token of type " + signer.getClass() + ": " + signer);
        }
        return timestampSigner;
    }

    private SecurityContextFinder makeSecurityContextFinder(final DecorationRequirements.SecureConversationSession secureConversationSession) {
        if (secureConversationSession == null)
            return null;
        return new SecurityContextFinder() {
            @Override
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return new SecurityContext() {
                    @Override
                    public byte[] getSharedSecret() {
                        return secureConversationSession.getSecretKey();
                    }
                    @Override
                    public SecurityToken getSecurityToken() {
                        return null;
                    }
                };
            }
        };
    }

    private void schemaValidateSamlAssertions(Document document) {
        List<Element> elements = new ArrayList<Element>();
        NodeList nl1 = document.getElementsByTagNameNS(SamlConstants.NS_SAML, SamlConstants.ELEMENT_ASSERTION);
        NodeList nl2 = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, SamlConstants.ELEMENT_ASSERTION);
        for (int n=0; n<nl1.getLength(); n++) {
            elements.add((Element) nl1.item(n));
        }
        for (int n=0; n<nl2.getLength(); n++) {
            elements.add((Element) nl2.item(n));
        }

        for ( Element element : elements ) {
            org.apache.xmlbeans.XmlObject assertion;
            try {
                if ( SamlConstants.NS_SAML.equals(element.getNamespaceURI()) ) {
                    log.info("Validating SAML v1.1 Assertion");
                    assertion = x0Assertion.oasisNamesTcSAML1.AssertionDocument.Factory.parse(element).getAssertion();
                } else {
                    log.info("Validating SAML v2.0 Assertion");
                    assertion = x0Assertion.oasisNamesTcSAML2.AssertionDocument.Factory.parse(element).getAssertion();
                }
            }
            catch(org.apache.xmlbeans.XmlException xe) {
                throw new RuntimeException("Error processing SAML assertion.", xe);
            }

            List errors = new ArrayList();
            org.apache.xmlbeans.XmlOptions xo = new org.apache.xmlbeans.XmlOptions();
            xo.setErrorListener(errors);
            if (!assertion.validate(xo)) {
                // Bug 3935 is finally fixed
                throw new RuntimeException("Invalid assertion: " + errors);
            }
        }
    }

    /**
     * @param a one of the elements to compare.  may be null
     * @param b the other element to compare.  May be null
     * @return true iff. the two elements have the same namespace and localname and the same number of direct ancestor
     *         elements, and each pair of corresponding direct ancestor elements have the same namespace and localname.
     * TODO I think this is broken -- I think it short-circuits as soon as one element's parent is null
     */
    private boolean localNamePathMatches(Element a, Element b) {
        Element aroot = a.getOwnerDocument().getDocumentElement();
        Element broot = b.getOwnerDocument().getDocumentElement();
        while (a != null && b != null) {
            if (!a.getLocalName().equals(b.getLocalName()))
                return false;
            if ((a.getNamespaceURI() == null) != (b.getNamespaceURI() == null))
                return false;
            if (a.getNamespaceURI() != null && !a.getNamespaceURI().equals(b.getNamespaceURI()))
                return false;
            if ((a == aroot) != (b == broot))
                return false;
            if (a == aroot)
                return true;
            a = (Element) a.getParentNode();
            b = (Element) b.getParentNode();
        }
        return true;
    }

    private static final String OSD_XML =
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>\n" +
            "<a xml:a=\"a\"><a xml:a=\"a\"/></a>\n" +
            "</s:Body></s:Envelope>";

    @Test
    @BugNumber(6851)
    public void testOsdSignatureError_withDefault() throws Exception {
        doTestOsdSignatureError(null);
    }

    @Test
    @BugNumber(6851)
    @Ignore("Disabled because it fails with XSS4J")
    public void testOsdSignatureError_withXSS4J() throws Exception {
        doTestOsdSignatureError(true);
    }

    @Test
    @BugNumber(6851)
    public void testOsdSignatureError_withXMLSerializer() throws Exception {
        doTestOsdSignatureError(false);
    }

    @SuppressWarnings({"deprecation"})
    private void doTestOsdSignatureError(Boolean useXss4j) throws Exception {
        try {
            XmlUtil.setSerializeWithXss4j(useXss4j);
            reallyDoTestOsdSignatureError();
        } finally {
            XmlUtil.setSerializeWithXss4j(null);
        }
    }

    private void reallyDoTestOsdSignatureError() throws Exception {
        Document doc = XmlUtil.stringToDocument(OSD_XML);

        DecorationRequirements dreq = new DecorationRequirements();
        Pair<X509Certificate,PrivateKey> key = new TestCertificateGenerator().generateWithKey();
        dreq.setSenderMessageSigningCertificate(key.left);
        dreq.setSenderMessageSigningPrivateKey(key.right);
        dreq.getElementsToSign().add(SoapUtil.getBodyElement(doc));
        final Message before = new Message(doc);
        new WssDecoratorImpl().decorateMessage(before, dreq);

        final Document beforeDoc = before.getXmlKnob().getDocumentReadOnly();
        assertNotNull(beforeDoc);
        final String beforeDocXml = XmlUtil.nodeToString(beforeDoc);
        Message req = new Message(XmlUtil.stringToDocument(beforeDocXml));

        ProcessorResult pr = new WssProcessorImpl(req).processMessage();

        Element reqBody = SoapUtil.getBodyElement(req.getXmlKnob().getDocumentReadOnly());
        SignedElement[] signed = pr.getElementsThatWereSigned();
        assertTrue(isSigned(signed, reqBody));
    }

    private static boolean isSigned(SignedElement[] signed, Element wut) {
        for (SignedElement se : signed) {
            if (se.asElement() == wut)
                return true;
        }
        return false;
    }
    
    @Test
    public void testSamlSecretKeyHokSubjectConfirmation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SamlSecretKeyHokSubjectConfirmation",
                wssDecoratorTest.getSignWithSamlHokSecretKeyTestDocument()), null);
    }

    @Test
    @BugNumber(9965)
    public void testSamlSecretKeyHokSubjectConfirmationWithSamlPreferred() throws Exception {
        runRoundTripTest(new NamedTestDocument("SamlSecretKeyHokSubjectConfirmationwWithSamlPreferred",
                wssDecoratorTest.getSignWithSamlHokSecretKeyWithSamlPreferredTestDocument()), null);
    }

    @Test
    @BugNumber(9749)
    public void testEncryptedSignature() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedSignature", wssDecoratorTest.getEncryptedSignatureTestDocument()), null);
    }

    @Test
    @BugNumber(9749)
    public void testWholeElementEncryption() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedSignature", wssDecoratorTest.getTestWholeElementEncryptionTestDocument()), null);
    }

}
