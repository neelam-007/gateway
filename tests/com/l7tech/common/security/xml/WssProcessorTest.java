/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssProcessorTest extends TestCase {
    private static Logger log = Logger.getLogger(WssProcessorTest.class.getName());

    public WssProcessorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssProcessorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testInteroperableDocumentProcessing() throws Exception {
        WssProcessor wssProcessor = new WssProcessorImpl();
        for (int i = 0; i < TEST_DOCUMENTS.length; i++) {
            TestDocument testDocument = TEST_DOCUMENTS[i];
            Document request = testDocument.document;
            X509Certificate recipientCertificate = testDocument.recipientCertificate;
            PrivateKey recipientPrivateKey = testDocument.recipientPrivateKey;


            log.info("Testing document: " + testDocument.name);
            WssProcessor.ProcessorResult result = wssProcessor.undecorateMessage(request,
                                                                                 recipientCertificate,
                                                                                 recipientPrivateKey);
            assertTrue(result != null);

            Element[] encrypted = result.getElementsThatWereEncrypted();
            assertTrue(encrypted != null);
            if (encrypted.length > 0) {
                log.info("The following elements were encrypted:");
                for (int j = 0; j < encrypted.length; j++) {
                    Element element = encrypted[j];
                    log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
                }
            } else
                log.info("No elements were encrypted.");

            Element[] signed = result.getElementsThatWereSigned();
            assertTrue(signed != null);
            if (signed.length > 0) {
                log.info("The following elements were signed:");
                for (int j = 0; j < signed.length; j++) {
                    Element element = signed[j];
                    log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
                }
            } else
                log.info("No elements were signed.");


            WssProcessor.SecurityToken[] tokens = result.getSecurityTokens();
            assertTrue(tokens != null);
            if (tokens.length > 0) {
                log.info("The following security tokens were found:");
                for (int j = 0; j < tokens.length; j++) {
                    WssProcessor.SecurityToken token = tokens[j];
                    log.info("  " + token.asObject());
                }
            } else
                log.info("No security tokens were found.");

            WssProcessor.Timestamp timestamp = result.getTimestamp();
            if (timestamp != null) {
                log.info("Timestamp created = " + timestamp.getCreated().asDate());
                log.info("Timestamp expires = " + timestamp.getExpires().asDate());
            } else
                log.info("No timestamp was found.");

            if (result.getWsaMessageId() != null) {
                log.info("MessageID = " + result.getWsaMessageId());
            }
            if (result.getWsaRelatesTo() != null) {
                log.info("Relates to = " + result.getWsaRelatesTo());
            }

            Document undecorated = result.getUndecoratedMessage();
            assertTrue(undecorated != null);
            log.info("Undecorated document:\n" + XmlUtil.nodeToFormattedString(undecorated));
        }
    }

    private static class TestDocument {
        String name;
        Document document;
        PrivateKey recipientPrivateKey;
        X509Certificate recipientCertificate;
        TestDocument(String n, Document d, PrivateKey rpk, X509Certificate rc) {
            this.name = n;
            this.document = d;
            this.recipientPrivateKey = rpk;
            this.recipientCertificate = rc;
        }
    }

    TestDocument[] TEST_DOCUMENTS = {
        makeDotNetTestDocument("dotnet encrypted request", TestDocuments.DOTNET_ENCRYPTED_REQUEST),
        makeDotNetTestDocument("dotnet signed request", TestDocuments.DOTNET_SIGNED_REQUEST),
        makeDotNetTestDocument("dotnet request with username token", TestDocuments.DOTNET_USERNAME_TOKEN),
        makeEttkTestDocument("ettk signed request", TestDocuments.ETTK_SIGNED_REQUEST),
        makeEttkTestDocument("ettk encrypted request", TestDocuments.ETTK_ENCRYPTED_REQUEST),
        makeEttkTestDocument("ettk signed encrypted request", TestDocuments.ETTK_SIGNED_ENCRYPTED_REQUEST),

        makeDotNetTestDocument("request wrapped l7 actor", TestDocuments.WRAPED_L7ACTOR),
        makeDotNetTestDocument("request multiple wrapped l7 actor", TestDocuments.MULTIPLE_WRAPED_L7ACTOR)
    };

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TestDocument makeDotNetTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getDotNetServerPrivateKey(),
                                    TestDocuments.getDotNetServerCertificate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
