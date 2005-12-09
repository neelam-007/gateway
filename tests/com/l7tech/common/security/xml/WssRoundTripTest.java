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
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.saml.SamlAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorate messages with WssDecorator and then send them through WssProcessor
 */
public class WssRoundTripTest extends TestCase {
    private static Logger log = Logger.getLogger(WssRoundTripTest.class.getName());
    private static final String SESSION_ID = "http://www.layer7tech.com/uuid/mike/myfunkytestsessionid";

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
    private NamedTestDocument[] getAllTestDocuments() throws Exception {
        // Find all getFooTestDocument() methods in WssDecoratorTest
        List testDocuments = new ArrayList();
        Method[] methods = WssDecoratorTest.class.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            if (name.startsWith("get") && name.endsWith("TestDocument") &&
                    method.getReturnType().equals(WssDecoratorTest.TestDocument.class))
            {
                testDocuments.add(new NamedTestDocument(name,
                                                        (WssDecoratorTest.TestDocument)method.invoke(
                                                                wssDecoratorTest, new Object[0])));
            }
        }

        log.info("Found " + testDocuments.size() + " TestDocuments in WssDecoratorTest");
        return (NamedTestDocument[])testDocuments.toArray(new NamedTestDocument[0]);
    }

    public void DISABLED_testAllRoundTrips() throws Exception {
        NamedTestDocument[] testDocs = getAllTestDocuments();
        for (int i = 0; i < testDocs.length; i++) {
            NamedTestDocument testDoc = testDocs[i];
            try {
                runRoundTripTest(testDoc);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Test \"" + testDoc.name + "\"failed: " + e.getMessage(), e);
            }
        }
    }

    public void testSimple() throws Exception {
        runRoundTripTest(new NamedTestDocument("Simple",
                                               wssDecoratorTest.getSimpleTestDocument()));
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
                                               wssDecoratorTest.getEncryptionOnlyTestDocument(XencAlgorithm.AES_192_CBC.getXEncName())));
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
                                               wssDecoratorTest.getSigningOnlyWithSecureConversationTestDocument()));
    }

    public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument()));
    }

    public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlHolderOfKeyRequest",
                                               wssDecoratorTest.getSignedSamlHolderOfKeyRequestTestDocument()));
    }

    public void testSignedSamlSenderVouchesRequest() throws Exception {
        runRoundTripTest(new NamedTestDocument("SignedSamlSenderVouchesRequest",
                                               wssDecoratorTest.getSignedSamlSenderVouchesRequestTestDocument()));
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

    private void runRoundTripTest(NamedTestDocument ntd) throws Exception {
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
        DecorationRequirements reqs = new DecorationRequirements();
        reqs.setSenderSamlToken(td.senderSamlAssertion, td.signSamlToken);
        reqs.setSenderMessageSigningCertificate(td.senderCert);
        reqs.setRecipientCertificate(td.recipientCert);
        reqs.setSenderMessageSigningPrivateKey(td.senderKey);
        reqs.setSignTimestamp();
        reqs.setUsernameTokenCredentials(null);
        reqs.setSuppressBst(td.suppressBst);
        if (td.secureConversationKey != null) {
            reqs.setSecureConversationSession(new DecorationRequirements.SecureConversationSession() {
                public String getId() { return SESSION_ID; }
                public byte[] getSecretKey() { return td.secureConversationKey.getEncoded(); }
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

        martha.decorateMessage(message, reqs);

        log.info("Decorated message (*note: pretty-printed):\n\n" + XmlUtil.nodeToFormattedString(message));

        // Serialize to string to simulate network transport
        byte[] decoratedMessage = XmlUtil.nodeToString(message).getBytes();

        // ... pretend HTTP goes here ...

        // Ooh, an incoming message has just arrived!
        Document incomingMessage = XmlUtil.stringToDocument(new String(decoratedMessage));

        assertTrue("Serialization did not affect the integrity of the XML message",
                   XmlUtil.nodeToString(message).equals(XmlUtil.nodeToString(incomingMessage)));

        ProcessorResult r = trogdor.undecorateMessage(new Message(incomingMessage),
                                                      td.senderCert,
                                                      td.recipientCert,
                                                      td.recipientKey,
                                                      makeSecurityContextFinder(td.secureConversationKey),
                                                      null);

        log.info("After undecoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(incomingMessage));

        ParsedElement[] encrypted = r.getElementsThatWereEncrypted();
        assertNotNull(encrypted);
        SignedElement[] signed = r.getElementsThatWereSigned();
        assertNotNull(signed);

        // Output security tokens
        XmlSecurityToken[] tokens = r.getXmlSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            XmlSecurityToken token = tokens[i];
            log.info("Got security token: " + token.getClass() + " type=" + token.getType());
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

        // If timestamp was supposed to be signed, make sure it actually was
        if (td.signTimestamp) {
            assertTrue("Timestamp was supposed to have been signed", r.getTimestamp().isSigned());
            XmlSecurityToken[] signers = r.getTimestamp().getSigningSecurityTokens();
            assertNotNull(signers);
            // Martha can currently only produce messages with a single timestamp-covering signature in the default sec header
            assertTrue(signers.length == 1);
            XmlSecurityToken signer = signers[0];
            if (signer instanceof X509SecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((X509SecurityToken)signer).getCertificate().equals(td.senderCert));
            } else if (signer instanceof SamlSecurityToken) {
                assertTrue("Timestamp signing security token must match sender cert",
                           ((SamlSecurityToken)signer).getSubjectCertificate().equals(new SamlAssertion(td.senderSamlAssertion).getSubjectCertificate()));
            } else if (signer instanceof SecurityContextToken) {
                SecurityContextToken sct = (SecurityContextToken)signer;
                assertTrue("SecurityContextToken was supposed to have proven possession", sct.isPossessionProved());
                assertEquals("WS-Security session ID was supposed to match", sct.getContextIdentifier(), SESSION_ID);
                assertTrue(Arrays.equals(sct.getSecurityContext().getSharedSecret().getEncoded(),
                                         td.secureConversationKey.getEncoded()));
            } else
                fail("Timestamp was signed with unrecognized security token of type " + signer.getClass() + ": " + signer);
        }

        // Make sure all requested elements were signed
        for (int i = 0; i < td.elementsToSign.length; ++i) {
            Element elementToSign = td.elementsToSign[i];

            boolean wasSigned = false;
            for (int j = 0; j < signed.length; ++j) {
                Element signedElement = signed[j].asElement();
                if (localNamePathMatches(elementToSign, signedElement)) {

                    // todo, fix
                    // assertEquals("Element " + elementToSign.getLocalName() + " signing token must match sender cert",
                    //             signed[j].getSigningSecurityToken().asX509Certificate(), td.senderCert);

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
    }

    private SecurityContextFinder makeSecurityContextFinder(final SecretKey secureConversationKey) {
        if (secureConversationKey == null) return null;
        return new SecurityContextFinder() {
            public SecurityContext getSecurityContext(String securityContextIdentifier) {
                return new SecurityContext() {
                    public SecretKey getSharedSecret() {
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
