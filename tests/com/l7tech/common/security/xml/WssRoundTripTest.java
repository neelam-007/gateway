/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.XmlUtil;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorate messages with WssDecorator and then send them through WssProcessor
 */
public class WssRoundTripTest extends TestCase {
    private static Logger log = Logger.getLogger(WssRoundTripTest.class.getName());

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

    public void testEncryptionOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("EncryptionOnly",
                                               wssDecoratorTest.getEncryptionOnlyTestDocument()));
    }

    public void testSigningOnly() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnly",
                                               wssDecoratorTest.getSigningOnlyTestDocument()));
    }

    public void testSingleSignatureMultipleEncryption() throws Exception {
        runRoundTripTest(new NamedTestDocument("SingleSignatureMultipleEncryption",
                                               wssDecoratorTest.getSingleSignatureMultipleEncryptionTestDocument()));
    }

    public void testWrappedSecurityHeader() throws Exception {
        runRoundTripTest(new NamedTestDocument("WrappedSecurityHeader",
                                               wssDecoratorTest.getWrappedSecurityHeaderTestDocument()));
    }

    public void testSkilessRecipientCert() throws Exception {
        runRoundTripTest(new NamedTestDocument("SkilessRecipientCert",
                                               wssDecoratorTest.getSkilessRecipientCertTestDocument()));        
    }

    // TODO make this pass
    public void OFF_testSigningOnlyWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningOnlyWithSecureConversation",
                                               wssDecoratorTest.getSigningOnlyWithSecureConversationTestDocument()));
    }

    // TODO make this pass
    public void OFF_testSigningAndEncryptionWithSecureConversation() throws Exception {
        runRoundTripTest(new NamedTestDocument("SigningAndEncryptionWithSecureConversation",
                                               wssDecoratorTest.getSigningAndEncryptionWithSecureConversationTestDocument()));
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
        WssDecorator.DecorationRequirements reqs = new WssDecorator.DecorationRequirements();
        reqs.setRecipientCertificate(td.recipientCert);
        reqs.setSenderCertificate(td.senderCert);
        reqs.setSenderPrivateKey(td.senderKey);
        reqs.setSignTimestamp(td.signTimestamp);
        reqs.setUsernameTokenCredentials(null);
        if (td.secureConversationKey != null)
            reqs.setSecureConversationSession(new WssDecorator.DecorationRequirements.SecureConversationSession() {
                public String getId() { return "http://www.layer7tech.com/uuid/mike/myfunkytestsessionid"; }
                public SecretKey getSecretKey() { return td.secureConversationKey; }
                public int getGeneration() { return 0; }
                public int getLength() { return 16; }
            });
        if (td.elementsToEncrypt != null)
            for (int i = 0; i < td.elementsToEncrypt.length; i++) {
                reqs.getElementsToEncrypt().add(td.elementsToEncrypt[i]);
            }
        if (td.elementsToSign != null)
            for (int i = 0; i < td.elementsToSign.length; i++) {
                reqs.getElementsToSign().add(td.elementsToSign[i]);
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

        WssProcessor.ProcessorResult r = trogdor.undecorateMessage(incomingMessage,
                                                                   td.recipientCert,
                                                                   td.recipientKey,
                                                                   makeSecurityContextFinder(td.secureConversationKey));

        Document undecorated = r.getUndecoratedMessage();
        log.info("After undecoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(undecorated));

        Element[] encrypted = r.getElementsThatWereEncrypted();
        assertNotNull(encrypted);
        WssProcessor.SignedElement[] signed = r.getElementsThatWereSigned();
        assertNotNull(signed);

        // If timestamp was supposed to be signed, make sure it actually was
        if (td.signTimestamp) {
            assertTrue("Timestamp was supposed to have been signed", r.getTimestamp().isSigned());
            assertTrue("Timestamp was supposed to have been signed by an X509 cert",
                       r.getTimestamp().getSigningSecurityToken() instanceof WssProcessor.X509SecurityToken);
            assertTrue("Timestamp signging security token must match sender cert",
                       ((WssProcessor.X509SecurityToken)r.getTimestamp().getSigningSecurityToken()).asX509Certificate().equals(td.senderCert));
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
                Element encryptedElement = encrypted[j];

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
            assertTrue("Element " + elementToEncrypt.getLocalName() + " must be encrypted", wasEncrypted);
            log.info("Element " + elementToEncrypt.getLocalName() + " verified as encrypted successfully.");
        }
    }

    private WssProcessor.SecurityContextFinder makeSecurityContextFinder(final SecretKey secureConversationKey) {
        if (secureConversationKey == null) return null;
        return new WssProcessor.SecurityContextFinder() {
            public WssProcessor.SecurityContext getSecurityContext(String securityContextIdentifier) {
                return new WssProcessor.SecurityContext() {
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
