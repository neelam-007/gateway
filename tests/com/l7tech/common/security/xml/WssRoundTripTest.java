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
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import org.w3c.dom.Document;
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

        martha.decorateMessage(message,
                               td.recipientCert,
                               td.senderCert,
                               td.senderKey,
                               td.signTimestamp,
                               td.elementsToEncrypt,
                               td.elementsToSign);

        // Serialize to string to simulate network transport
        byte[] decoratedMessage = XmlUtil.documentToString(message).getBytes();

        // ... pretend HTTP goes here ...

        // Ooh, an incoming message has just arrived!
        Document incomingMessage = XmlUtil.stringToDocument(new String(decoratedMessage));

        WssProcessor.ProcessorResult r = trogdor.undecorateMessage(incomingMessage,
                                                                   td.recipientCert,
                                                                   td.recipientKey);

        Document undecorated = r.getUndecoratedMessage();
        log.info("After round-trip:" + XmlUtil.documentToFormattedString(undecorated));
    }
}
