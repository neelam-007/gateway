/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.WsiBSPValidator;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * Decorate messages with WssDecorator and then send them through WssProcessor
 */
public class WssRoundTripTest {
    private static Logger log = Logger.getLogger(WssRoundTripTest.class.getName());
    private static final WsiBSPValidator validator = new WsiBSPValidator();

    static {
        System.setProperty(WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS, "true");
    }

    private static class NamedTestDocument {
        String name;
        WssDecoratorTest.TestDocument td;

        public NamedTestDocument(String name, WssDecoratorTest.TestDocument td) {
            this.name = name;
            this.td = td;
        }
    }

    WssDecoratorTest wssDecoratorTest = new WssDecoratorTest("WssDecoratorTest");

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
        runRoundTripTest(new NamedTestDocument("signedUsernameToken", wssDecoratorTest.getSignedUsernameTokenTestDocument()));
    }

    public void doTestSignedAndEncryptedUsernameToken( SecurityTokenResolver securityTokenResolver) throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("Signed and Encrypted UsernameToken",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());
        ntd.td.securityTokenResolver = securityTokenResolver;
        EncryptedKey[] ekh = new EncryptedKey[1];
        runRoundTripTest(ntd, true, ekh);
        EncryptedKey encryptedKey = ekh[0];
        assertNotNull(encryptedKey);

        NamedTestDocument ntd2 = new NamedTestDocument("Signed and Encrypted UsernameToken (2)",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());

        // Make a second request using the same encrypted key and token resolver
        ntd2.td.securityTokenResolver = ntd.td.securityTokenResolver;
        ntd2.td.req.setEncryptedKeySha1(encryptedKey.getEncryptedKeySHA1());

        DecorationRequirements reqs = ntd2.td.req;
        reqs.setEncryptedKey(encryptedKey.getSecretKey());
        reqs.setEncryptedKeySha1(encryptedKey.getEncryptedKeySHA1());

        Document doc = ntd2.td.c.message;
        new WssDecoratorImpl().decorateMessage(new Message(doc), reqs);

        // Now try processing it
        Document doc1 = XmlUtil.stringToDocument(XmlUtil.nodeToString(doc));
        Message req = new Message(doc1);

        log.info("SECOND decorated request *Pretty-Printed*: " + XmlUtil.nodeToFormattedString(doc));

        WrapSSTR strr = new WrapSSTR(ntd2.td.req.getRecipientCertificate(), ntd2.td.recipientKey, ntd2.td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{ntd2.td.req.getSenderMessageSigningCertificate()});
        ProcessorResult r = new WssProcessorImpl().undecorateMessage(req,
                                                                     ntd2.td.req.getSenderMessageSigningCertificate(),
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
                assertEquals(foo.getResultCode(), ProcessorResultUtil.FALSIFIED);
            else
                assertEquals(foo.getResultCode(), ProcessorResultUtil.NO_ERROR);
        }

        if (securityTokenResolver == null) {
            // Second request should have included no EncryptedKey, and hence no signed timestamp
            for (XmlSecurityToken toke : r.getXmlSecurityTokens())
                if (toke instanceof EncryptedKey)
                    fail("Second request included an EncryptedKey");
            assertTrue(null==ProcessorResultUtil.getParsedElementForNode(r.getTimestamp()==null ? null : r.getTimestamp().asElement(), r.getElementsThatWereSigned()));
        } else {
            // If timestamp was supposed to be signed, make sure it actually was
            EncryptedKey[] ekOut2 = new EncryptedKey[1];
            SigningSecurityToken timestampSigner = checkTimestampSignature(td, r, ekOut2);
            checkEncryptedUsernameToken(td, usernameToken, r, timestampSigner);
        }
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
                         false);
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
    public void testSigningOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly",
                                               wssDecoratorTest.getSigningOnlyTestDocument()));
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
                         false);
    }

    @Test
    public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument()),
                         false);
    }

    @Test
    public void testSigningAndEncryptionWithSecureConversationWss11() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument());
        ntd.td.req.addSignatureConfirmation("abc11SignatureConfirmationValue11blahblahblah11==");
        runRoundTripTest(ntd,
                         false);
    }

    @Test
    public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlHolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument(1)),
                         false);
    }

    @Test
    public void testSignedSamlSenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlSenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument(1)),
                         false);
    }

    @Test
    public void testSignedSaml2HolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSaml2HolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument(2)),
                         false);
    }

    @Test
    public void testSignedSaml2SenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSaml2SenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument(2)),
                         false);
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

        String result = runRoundTripTest(ntd, false);

        assertTrue("Use of correct transform", result.contains(SoapUtil.TRANSFORM_ATTACHMENT_CONTENT));
    }

    @Test
    public void testSoapWithSignedAttachmentComplete() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("SoapWithSignedAttachmentContentAndMIMEHeaders",
                                               wssDecoratorTest.getSoapWithSignedAttachmentTestDocument());

        ntd.td.req.setSignPartHeaders(true);
        ntd.td.req.getPartsToSign().add("-76392836.13454");

        String result = runRoundTripTest(ntd, false);

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
                                               false);
    }

    @Test
    public void testEncryptedUsernameTokenWithDerivedKeys() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedUsernameTokenWithDerivedKeys",
                                               wssDecoratorTest.getEncryptedUsernameTokenWithDerivedKeysTestDocument()),
                                               false);
    }

    @Test
    public void testOaepEncryptedKey() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedKeyAlgorithm",
                                               wssDecoratorTest.getOaepKeyEncryptionTestDocument()));
    }

    @Test
    public void testConfigSecHdrAttributesTestDocument() throws Exception {
        runRoundTripTest(new NamedTestDocument("ConfigSecHdrAttributesTestDocument", wssDecoratorTest.getConfigSecHdrAttributesTestDocument()));
    }

    @Test
    public void testExplicitSignatureConfirmation() throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("ExplicitSignatureConfirmation",
                                                      wssDecoratorTest.getExplicitSignatureConfirmationsTestDocument());
        runRoundTripTest(ntd, false);
    }

    private void runRoundTripTest(NamedTestDocument ntd) throws Exception {
        runRoundTripTest(ntd, true);
    }

    // @return  the decorated request, in case you want to test replaying it
    private String runRoundTripTest(NamedTestDocument ntd, boolean checkBSP1Compliance) throws Exception {
        return runRoundTripTest(ntd, checkBSP1Compliance, null);
    }

    // @return  the decorated request, in case you want to test replaying it
    private String runRoundTripTest(NamedTestDocument ntd, boolean checkBSP1Compliance, EncryptedKey[] ekOut) throws Exception {
        log.info("Running round-trip test on test document: " + ntd.name);
        final WssDecoratorTest.TestDocument td = ntd.td;
        WssDecoratorTest.Context c = td.c;
        Message message = c.messageMessage;
        Document soapMessage = message.getXmlKnob().getDocumentReadOnly();

        WssDecorator martha = new WssDecoratorImpl();
        WssProcessor trogdor = new WssProcessorImpl();

        // save a record of the literal encrypted elements before they get messed with
        final Set<Element> elementSet = td.req.getElementsToEncrypt();
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

        // ... pretend HTTP goes here ...
        final String networkRequestString = new String(decoratedMessageDocument);

        // Ooh, an incoming message has just arrived!
        Message incomingMessage = new Message();
        incomingMessage.initialize(c.messageMessage.getMimeKnob().getOuterContentType(), decoratedMessage);
        Document incomingSoapDocument = incomingMessage.getXmlKnob().getDocumentReadOnly();

        boolean isValid = !checkBSP1Compliance || validator.isValid(incomingSoapDocument);

        assertTrue("Serialization did not affect the integrity of the XML message",
                   XmlUtil.nodeToString(soapMessage).equals(XmlUtil.nodeToString(XmlUtil.stringToDocument(networkRequestString))));

        WrapSSTR strr = new WrapSSTR(td.req.getRecipientCertificate(), td.recipientKey, td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{td.req.getSenderMessageSigningCertificate()});
        ProcessorResult r = trogdor.undecorateMessage(incomingMessage,
                                                      td.req.getSenderMessageSigningCertificate(),
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
            assertEquals(foo.getResultCode(), ProcessorResultUtil.NO_ERROR);
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
        if (!td.req.getSignatureConfirmations().isEmpty()) {
            List<SignatureConfirmation> gotConfimationElements = r.getSignatureConfirmationValues();
            for (SignatureConfirmation element : gotConfimationElements)
                assertTrue(signedDomElements.contains(element.asElement()));
            List<String> gotConfirmationValues = Functions.map(gotConfimationElements,
                    Functions.<String, SignatureConfirmation>getterTransform(SignatureConfirmation.class.getMethod("getConfirmationValue")));
            for (String expectedConfValue : td.req.getSignatureConfirmations())
                assertTrue("Expect SignatureConfirmation for: " + expectedConfValue, gotConfirmationValues.contains(expectedConfValue));
        }

        if (td.req.isProtectTokens()) {
            for (SignedElement signedElement : signed) {
                final Element signingTokenEl = signedElement.getSigningSecurityToken().asElement();
                final boolean signingTokenElWasSigned = signedDomElements.contains(signingTokenEl);
                assertTrue("ProtectTokens implies that all signing tokens were signed", signingTokenElWasSigned);
            }
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

        assertTrue("WS-I BSP check.", isValid);

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
            assertNotNull("Timestamp was supposed to have been signed", ProcessorResultUtil.getParsedElementForNode(r.getTimestamp().asElement(), r.getElementsThatWereSigned()));
            SigningSecurityToken[] signers = r.getSigningTokens(rts.asElement());
            assertNotNull(signers);
            // Martha can currently only produce messages with a single timestamp-covering signature in the default sec header
            assertTrue(signers.length == 1);
            SigningSecurityToken signer = timestampSigner = signers[0];
            assertTrue(signer.asElement().getOwnerDocument() == rts.asElement().getOwnerDocument());
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
                assertEquals("WS-Security session ID was supposed to match", sct.getContextIdentifier(), WssDecoratorTest.TEST_WSSC_SESSION_ID);
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
}
