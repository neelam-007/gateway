/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.message.Message;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.skunkworks.wsibsp.WsiBSPValidator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Decorate messages with WssDecorator and then send them through WssProcessor
 */
public class WssRoundTripTest extends TestCase {
    private static Logger log = Logger.getLogger(WssRoundTripTest.class.getName());
    private static final String SESSION_ID = "http://www.layer7tech.com/uuid/mike/myfunkytestsessionid";
    private static final WsiBSPValidator validator = new WsiBSPValidator();

    static {
        System.setProperty(WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS, "true");
    }

    public WssRoundTripTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssRoundTripTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
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

    public void testSimple() throws Exception {
        runRoundTripTest(new NamedTestDocument("Simple",
                                               wssDecoratorTest.getSimpleTestDocument()));
    }

    public void testSignedAndEncryptedUsernameToken() throws Exception {
        doTestSignedAndEncryptedUsernameToken(null);
    }

    public void testSignedAndEncryptedUsernameToken_withKeyCache() throws Exception {
        doTestSignedAndEncryptedUsernameToken(new SimpleSecurityTokenResolver());
    }

    public void doTestSignedAndEncryptedUsernameToken(SecurityTokenResolver securityTokenResolver) throws Exception {
        NamedTestDocument ntd = new NamedTestDocument("Signed and Encrypted UsernameToken",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());
        ntd.td.securityTokenResolver = securityTokenResolver;
        String undecoratedRequest = XmlUtil.nodeToString(ntd.td.c.message);
        EncryptedKey[] ekh = new EncryptedKey[1];
        runRoundTripTest(ntd, false, ekh);
        EncryptedKey encryptedKey = ekh[0];
        assertNotNull(encryptedKey);

        NamedTestDocument ntd2 = new NamedTestDocument("Signed and Encrypted UsernameToken (2)",
                                                      wssDecoratorTest.getSignedAndEncryptedUsernameTokenTestDocument());

        // Make a second request using the same encrypted key and token resolver
        ntd2.td.securityTokenResolver = ntd.td.securityTokenResolver;
        ntd2.td.encryptedKeySha1 = encryptedKey.getEncryptedKeySHA1();

        DecorationRequirements reqs = makeDecorationRequirements(ntd2.td);
        reqs.setEncryptedKey(encryptedKey.getSecretKey());
        reqs.setEncryptedKeySha1(encryptedKey.getEncryptedKeySHA1());

        Document doc = ntd2.td.c.message;
        new WssDecoratorImpl().decorateMessage(doc, reqs);

        // Now try processing it
        Document doc1 = XmlUtil.stringToDocument(XmlUtil.nodeToString(doc));
        Message req = new Message(doc1);

        log.info("SECOND decorated request *Pretty-Printed*: " + XmlUtil.nodeToFormattedString(doc));

        WrapSSTR strr = new WrapSSTR(ntd2.td.recipientCert, ntd2.td.recipientKey, ntd2.td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{ntd2.td.senderCert});
        ProcessorResult r = new WssProcessorImpl().undecorateMessage(req,
                                                                     ntd2.td.senderCert,
                                                                     null,
                                                                     strr);

        UsernameToken usernameToken = findUsernameToken(r);
        WssDecoratorTest.TestDocument td = ntd2.td;
        if (td.signUsernameToken) {
            Map ns = new HashMap();
            ns.put("wsse", SoapUtil.SECURITY_NAMESPACE);
            ProcessorResultUtil.SearchResult foo = ProcessorResultUtil.searchInResult(log, doc1, "//wsse:UsernameToken", ns, false, r.getElementsThatWereSigned(), "signed");
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
            assertFalse(r.getTimestamp().isSigned());
        } else {
            // If timestamp was supposed to be signed, make sure it actually was
            EncryptedKey[] ekOut2 = new EncryptedKey[1];
            SigningSecurityToken timestampSigner = checkTimestampSignature(td, r, ekOut2);
            checkEncryptedUsernameToken(td, usernameToken, r, timestampSigner);
        }
    }

    public void testEncryptedBodySignedEnvelope() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedBodySignedEnvelope",
                                               wssDecoratorTest.getEncryptedBodySignedEnvelopeTestDocument()));
    }

    public void testSignedEnvelope() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedEnvelope",
                                               wssDecoratorTest.getSignedEnvelopeTestDocument()));
    }

    public void testEncryptionOnlyAES128() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES128",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_128_CBC.getXEncName())));
    }

    public void testEncryptionOnlyAES192() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES192",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_192_CBC.getXEncName())),
                         false);
    }

    public void testEncryptionOnlyAES256() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyAES256",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_256_CBC.getXEncName())));
    }

    public void testEncryptionOnlyTripleDES() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnlyTripleDES",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.TRIPLE_DES_CBC.getXEncName())));
    }

    public void testSigningOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly",
                                               wssDecoratorTest.getSigningOnlyTestDocument()));
    }

    public void testSingleSignatureMultipleEncryption() throws Exception {
        runRoundTripTest(new NamedTestDocument("SingleSignatureMultipleEncryption",
                                               wssDecoratorTest.getSingleSignatureMultipleEncryptionTestDocument()));
    }

    /* we no longer support this
    public void testWrappedSecurityHeader() throws Exception {
        runRoundTripTest(new NamedTestDocument("WrappedSecurityHeader",
                                               wssDecoratorTest.getWrappedSecurityHeaderTestDocument()));
    }*/

    public void testSkilessRecipientCert() throws Exception {
        runRoundTripTest(new NamedTestDocument("SkilessRecipientCert",
                                               wssDecoratorTest.getSkilessRecipientCertTestDocument()));
    }

    public void testSigningOnlyWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnlyWithSecureConversation",
                                               wssDecoratorTest.getSigningOnlyWithSecureConversationTestDocument()),
                         false);
    }

    public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument()),
                         false);
    }

    public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlHolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument()),
                         false);
    }

    public void testSignedSamlSenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlSenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument()),
                         false);
    }

    public void testSignedEmptyElement() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedEmptyElement",
                                               wssDecoratorTest.getSignedEmptyElementTestDocument()));
    }

    public void testEncryptedEmptyElement() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedEmptyElement",
                                               wssDecoratorTest.getEncryptedEmptyElementTestDocument()));
    }

    public void testSoapWithUnsignedAttachment() throws Exception {
        runRoundTripTest(new NamedTestDocument("SoapWithUnsignedAttachment",
                                               wssDecoratorTest.getSoapWithUnsignedAttachmentTestDocument()));
    }

    public void testSoapWithSignedAttachment() throws Exception {
        runRoundTripTest(new NamedTestDocument("SoapWithSignedAttachment",
                                               wssDecoratorTest.getSoapWithSignedAttachmentTestDocument()));
    }

    public void testSoapWithSignedEncryptedAttachment() throws Exception {
        runRoundTripTest(new NamedTestDocument("SoapWithSignedEncryptedAttachment",
                                               wssDecoratorTest.getSoapWithSignedEncryptedAttachmentTestDocument()));
    }

    public void testSignedAndEncryptedBodyWithNoBst() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedAndEncryptedBodyWithNoBst",
                                               wssDecoratorTest.getSignedAndEncryptedBodyWithNoBstTestDocument()));
    }

    public void testEncryptedUsernameToken() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedUsernameToken",
                                               wssDecoratorTest.getEncryptedUsernameTokenTestDocument()),
                         false);
    }

    public void testEncryptedUsernameTokenWithDerivedKeys() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedUsernameTokenWithDerivedKeys",
                                               wssDecoratorTest.getEncryptedUsernameTokenWithDerivedKeysTestDocument()),
                                               false);
    }

    public void testOaepEncryptedKey() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptedKeyAlgorithm",
                                               wssDecoratorTest.getOaepKeyEncryptionTestDocument()));
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
        Document message = c.message;

        WssDecorator martha = new WssDecoratorImpl();
        WssProcessor trogdor = new WssProcessorImpl();

        // save a record of the literal encrypted elements before they get messed with
        Element[] elementsBeforeEncryption = new Element[td.elementsToEncrypt.length];
        String[] canonicalizedElementsBeforeEncryption = new String[td.elementsToEncrypt.length];
        for (int i = 0; i < td.elementsToEncrypt.length; i++) {
            Element element = td.elementsToEncrypt[i];
            log.info("Saving canonicalizing copy of " + element.getLocalName() + " before it gets encrypted");
            canonicalizedElementsBeforeEncryption[i] = canonicalize(element);
            elementsBeforeEncryption[i] = (Element) element.cloneNode(true);
        }

        log.info("Message before decoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(message));
        DecorationRequirements reqs = makeDecorationRequirements(td);

        martha.decorateMessage(message, reqs);

        log.info("Decorated message (*note: pretty-printed):\n\n" + XmlUtil.nodeToFormattedString(message));

        // Serialize to string to simulate network transport
        byte[] decoratedMessage = XmlUtil.nodeToString(message).getBytes();

        // ... pretend HTTP goes here ...
        final String networkRequestString = new String(decoratedMessage);

        // Ooh, an incoming message has just arrived!
        Document incomingMessage = XmlUtil.stringToDocument(networkRequestString);

        boolean isValid = !checkBSP1Compliance || validator.isValid(incomingMessage);

        assertTrue("Serialization did not affect the integrity of the XML message",
                   XmlUtil.nodeToString(message).equals(XmlUtil.nodeToString(incomingMessage)));

        WrapSSTR strr = new WrapSSTR(td.recipientCert, td.recipientKey, td.securityTokenResolver);
        strr.addCerts(new X509Certificate[]{td.senderCert});
        ProcessorResult r = trogdor.undecorateMessage(new Message(incomingMessage),
                                                      td.senderCert,
                                                      makeSecurityContextFinder(td.secureConversationKey),
                                                      strr);

        log.info("After undecoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(incomingMessage));

        ParsedElement[] encrypted = r.getElementsThatWereEncrypted();
        assertNotNull(encrypted);
        SignedElement[] signed = r.getElementsThatWereSigned();
        assertNotNull(signed);

        // Output security tokens
        UsernameToken usernameToken = findUsernameToken(r);

        if (td.signUsernameToken) {
            Map ns = new HashMap();
            ns.put("wsse", SoapUtil.SECURITY_NAMESPACE);
            ProcessorResultUtil.SearchResult foo = ProcessorResultUtil.searchInResult(log, incomingMessage, "//wsse:UsernameToken", ns, false, r.getElementsThatWereSigned(), "signed");
            assertEquals(foo.getResultCode(), ProcessorResultUtil.NO_ERROR);
        }

        // If timestamp was supposed to be signed, make sure it actually was
        SigningSecurityToken timestampSigner = checkTimestampSignature(td, r, ekOut);
        checkEncryptedUsernameToken(td, usernameToken, r, timestampSigner);

        // Make sure all requested elements were signed
        for (int i = 0; i < td.elementsToSign.length; ++i) {
            Element elementToSign = td.elementsToSign[i];

            boolean wasSigned = false;
            for (int j = 0; j < signed.length; ++j) {
                Element signedElement = signed[j].asElement();
                if (localNamePathMatches(elementToSign, signedElement)) {
                    wasSigned = true;
                    break;
                }
            }
            assertTrue("Element " + elementToSign.getLocalName() + " must be signed", wasSigned);
            log.info("Element " + elementToSign.getLocalName() + " verified as signed successfully.");
        }

        // Make sure all requested elements were encrypted
        for (int i = 0; i < elementsBeforeEncryption.length; ++i) {
            Element elementToEncrypt = elementsBeforeEncryption[i];
            String canonicalized = canonicalizedElementsBeforeEncryption[i];
            log.info("Looking to ensure element was encrypted: " + XmlUtil.nodeToString(elementToEncrypt));

            boolean wasEncrypted = false;
            for (int j = 0; j < encrypted.length; ++j) {
                Element encryptedElement = encrypted[j].asElement();

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
                       XmlUtil.elementIsEmpty(elementToEncrypt) || wasEncrypted);
            log.info("Element " + elementToEncrypt.getLocalName() + " succesfully verified as either empty or encrypted.");
        }

        assertTrue("WS-I BSP check.", isValid);

        return networkRequestString;
    }

    private UsernameToken findUsernameToken(ProcessorResult r) {
        XmlSecurityToken[] tokens = r.getXmlSecurityTokens();
        UsernameToken usernameToken = null;
        for (int i = 0; i < tokens.length; i++) {
            XmlSecurityToken token = tokens[i];
            log.info("Got security token: " + token.getClass() + " type=" + token.getType());
            if (token instanceof UsernameToken) {
                if (usernameToken != null)
                    fail("More than one UsernameToken found in processor result");
                usernameToken = (UsernameToken)token;
            }
            if (token instanceof SigningSecurityToken) {
                SigningSecurityToken signingSecurityToken = (SigningSecurityToken)token;
                log.info("It's a signing security token.  possessionProved=" + signingSecurityToken.isPossessionProved());
                SignedElement[] signedElements = signingSecurityToken.getSignedElements();
                for (int j = 0; j < signedElements.length; j++) {
                    SignedElement signedElement = signedElements[j];
                    log.info("It was used to sign: " + signedElement.asElement().getLocalName());
                }
            }
        }
        return usernameToken;
    }

    private void checkEncryptedUsernameToken(WssDecoratorTest.TestDocument td, UsernameToken usernameToken, ProcessorResult r, SigningSecurityToken timestampSigner) {
        if (td.encryptUsernameToken) {
            assertNotNull(usernameToken);
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
        if (td.signTimestamp) {
            WssTimestamp rts = r.getTimestamp();
            assertNotNull(rts);
            assertTrue("Timestamp was supposed to have been signed", rts.isSigned());
            SigningSecurityToken[] signers = r.getSigningTokens(rts.asElement());
            assertNotNull(signers);
            // Martha can currently only produce messages with a single timestamp-covering signature in the default sec header
            assertTrue(signers.length == 1);
            SigningSecurityToken signer = timestampSigner = signers[0];
            assertTrue(signer.asElement().getOwnerDocument() == rts.asElement().getOwnerDocument());
            assertTrue(signer.isPossessionProved());

            if (signer instanceof X509SecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((X509SecurityToken)signer).getCertificate().equals(td.senderCert));
            } else if (signer instanceof SamlSecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((SamlSecurityToken)signer).getSubjectCertificate().equals(SamlAssertion.newInstance(td.senderSamlAssertion).getSubjectCertificate()));
            } else if (signer instanceof SecurityContextToken) {
                SecurityContextToken sct = (SecurityContextToken)signer;
                assertTrue("SecurityContextToken was supposed to have proven possession", sct.isPossessionProved());
                assertEquals("WS-Security session ID was supposed to match", sct.getContextIdentifier(), SESSION_ID);
                assertTrue(Arrays.equals(sct.getSecurityContext().getSharedSecret(),
                                         td.secureConversationKey));
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

    private DecorationRequirements makeDecorationRequirements(final WssDecoratorTest.TestDocument td) {
        DecorationRequirements reqs = new DecorationRequirements();
        reqs.setSenderSamlToken(td.senderSamlAssertion, td.signSamlToken);
        reqs.setSenderMessageSigningCertificate(td.senderCert);
        reqs.setRecipientCertificate(td.recipientCert);
        reqs.setSenderMessageSigningPrivateKey(td.senderKey);
        reqs.setSignTimestamp();
        reqs.setUsernameTokenCredentials(td.usernameToken);
        reqs.setEncryptUsernameToken(td.encryptUsernameToken);
        reqs.setSignUsernameToken(td.signUsernameToken);
        reqs.setSuppressBst(td.suppressBst);
        reqs.setUseDerivedKeys(td.useDerivedKeys);
        reqs.setKeyEncryptionAlgorithm(td.keyEncryptionAlgoritm);
        if (td.secureConversationKey != null) {
            reqs.setSecureConversationSession(new DecorationRequirements.SecureConversationSession() {
                public String getId() { return SESSION_ID; }
                public byte[] getSecretKey() { return td.secureConversationKey; }
                public String getSCNamespace() {
                        return SoapUtil.WSSC_NAMESPACE;
                    }
            });
        }
        if (td.elementsToEncrypt != null) {
            for (int i = 0; i < td.elementsToEncrypt.length; i++) {
                reqs.getElementsToEncrypt().add(td.elementsToEncrypt[i]);
            }
        }
        if (td.encryptionAlgorithm !=null) {
            reqs.setEncryptionAlgorithm(td.encryptionAlgorithm);
        }
        if (td.elementsToSign != null) {
            for (int i = 0; i < td.elementsToSign.length; i++) {
                reqs.getElementsToSign().add(td.elementsToSign[i]);
            }
        }
        return reqs;
    }

    private SecurityContextFinder makeSecurityContextFinder(final byte[] secureConversationKey) {
        if (secureConversationKey == null) return null;
        return new SecurityContextFinder() {
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return new SecurityContext() {
                    public byte[] getSharedSecret() {
                        return secureConversationKey;
                    }
                };
            }
        };
    }

    private String canonicalize(Node node) throws IOException {
        return XmlUtil.nodeToString(node);
    }

    /**
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
            if (a == aroot || b == broot)
                return true;
            a = (Element) a.getParentNode();
            b = (Element) b.getParentNode();
        }
        return true;
    }
}
