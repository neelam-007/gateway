/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.lang.reflect.Method;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.l7tech.common.util.XmlUtil;

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

    private void runRoundTripTest(NamedTestDocument ntd) throws Exception {
        log.info("Running round-trip test on test document: " + ntd.name);
        WssDecoratorTest.TestDocument td = ntd.td;
        WssDecoratorTest.Context c = td.c;
        Document message = c.message;

        WssDecorator martha = new WssDecoratorImpl();
        WssProcessor trogdor = new WssProcessorImpl();

        log.info("Message before decoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(message));

        martha.decorateMessage(message,
                               td.recipientCert,
                               td.senderCert,
                               td.senderKey,
                               td.signTimestamp,
                               td.elementsToEncrypt,
                               td.elementsToSign,
                               null);

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
                                                                   td.recipientKey);

        Document undecorated = r.getUndecoratedMessage();
        log.info("After undecoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(undecorated));

        Element[] encrypted = r.getElementsThatWereEncrypted();
        assertNotNull(encrypted);
        WssProcessor.SignedElement[] signed = r.getElementsThatWereSigned();
        assertNotNull(signed);

        // If timestamp was supposed to be signed, make sure it actually was
        if (td.signTimestamp) {
            assertTrue("Timestamp was supposed to have been signed", r.getTimestamp().isSigned());
            assertTrue("Timestamp signging security token must match sender cert",
                       r.getTimestamp().getSigningSecurityToken().asX509Certificate().equals(td.senderCert));
        }

        // Make sure all requested elements were signed
        for (int i = 0; i < td.elementsToSign.length; ++i) {
            Element elementToSign = td.elementsToSign[i];

            boolean wasSigned = false;
            for (int j = 0; j < signed.length; ++j) {
                Element signedElement = signed[j].asElement();
                if (localNamePathMatches(elementToSign, signedElement)) {
                    assertEquals("Element " + elementToSign.getLocalName() + " signing token must match sender cert",
                                 signed[j].getSigningSecurityToken().asX509Certificate(), td.senderCert);
                    wasSigned = true;
                    break;
                }
            }
            assertTrue("Element " + elementToSign.getLocalName() + " must be signed", wasSigned);
            log.info("Element " + elementToSign.getLocalName() + " verified as signed successfully.");
        }

        // Make sure all requested elements were encrypted
        for (int i = 0; i < td.elementsToEncrypt.length; ++i) {
            Element elementToEncrypt = td.elementsToEncrypt[i];
            log.info("Looking to ensure element was encrypted: " + XmlUtil.nodeToString(elementToEncrypt));

            boolean wasEncrypted = false;
            for (int j = 0; j < encrypted.length; ++j) {
                Element encryptedElement = encrypted[j];
                log.info("Checking if element matches: " + XmlUtil.nodeToString(encryptedElement));
                if (localNamePathMatches(elementToEncrypt, encryptedElement)) {
                    assertEquals("Original element " + encryptedElement.getLocalName() +
                                 " content must match decrypted element content",
                                 XmlUtil.nodeToString(elementToEncrypt),
                                 XmlUtil.nodeToString(encryptedElement));
                    wasEncrypted = true;
                    break;
                }
            }
            assertTrue("Element " + elementToEncrypt.getLocalName() + " must be encrypted", wasEncrypted);
            log.info("Element " + elementToEncrypt.getLocalName() + " verified as encrypted successfully.");
        }
    }

    /**
     * @return true iff. the two elements have the same namespace and localname and the same number of direct ancestor
     *         elements, and each pair of corresponding direct ancestor elements have the same namespace and localname.
     */
    private boolean localNamePathMatches(Element a, Element b) {
        Element aroot = a.getOwnerDocument().getDocumentElement();
        Element broot = b.getOwnerDocument().getDocumentElement();
        while (a != null && b != null) {
            if (!a.getLocalName().equals(b.getLocalName()))
                return false;
            if (!a.getNamespaceURI().equals(b.getNamespaceURI()))
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
