/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;
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


            Element[] encrypted = result.getElementsThatWereEncrypted();
            log.info("The following elements were encrypted:");
            for (int j = 0; j < encrypted.length; j++) {
                Element element = encrypted[j];
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }

            Element[] signed = result.getElementsThatWereSigned();
            log.info("The following elements were signed:");
            for (int j = 0; j < signed.length; j++) {
                Element element = signed[j];
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }

            WssProcessor.SecurityToken[] tokens = result.getSecurityTokens();
            log.info("The following security tokens were found:");
            for (int j = 0; j < tokens.length; j++) {
                WssProcessor.SecurityToken token = tokens[j];
                log.info("  " + token.getClass());
            }

            WssProcessor.Timestamp timestamp = result.getTimestamp();
            log.info("Timestamp created = " + timestamp.getCreated().asDate());
            log.info("Timestamp expires = " + timestamp.getExpires().asDate());

            Document undecorated = result.getUndecoratedMessage();
            log.info("Undecordated document:\n" + XmlUtil.documentToString(undecorated));
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
        makeDotNetTestDocument("dotnet signed tampered request", TestDocuments.DOTNET_SIGNED_TAMPERED_REQUEST),
        makeDotNetTestDocument("dotnet signed request using derived key token",
                               TestDocuments.DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN),
        makeDotNetTestDocument("dotnet signed request using derived key token",
                               TestDocuments.DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN),
        makeEttkTestDocument("ettk signed request", TestDocuments.ETTK_SIGNED_REQUEST),
        makeEttkTestDocument("ettk encrypted request", TestDocuments.ETTK_ENCRYPTED_REQUEST),
        makeEttkTestDocument("ettk signed encrypted request", TestDocuments.ETTK_SIGNED_ENCRYPTED_REQUEST),
    };

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);

            Properties ksp = new Properties();
            ksp.load(TestDocuments.getInputStream(TestDocuments.ETTK_KS_PROPERTIES));
            String keystorePassword = ksp.getProperty("keystore.storepass");
            String serverAlias = ksp.getProperty("keystore.server.alias");
            String serverKeyPassword = ksp.getProperty("keystore.server.keypass");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream fis = null;
            try {
                fis = TestDocuments.getInputStream(TestDocuments.SSL_KS);
                keyStore.load(fis, keystorePassword.toCharArray());
            } finally {
                if (fis != null)
                    fis.close();
            }
            PrivateKey rpk = (PrivateKey)keyStore.getKey(serverAlias, serverKeyPassword.toCharArray());
            X509Certificate rc = (X509Certificate)keyStore.getCertificate(serverAlias);
            return new TestDocument(testname, d, rpk, rc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TestDocument makeDotNetTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d, getDotNetRecipientPrivateKey(), getDotNetRecipientCertificate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey getDotNetRecipientPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream fis = TestDocuments.getInputStream(TestDocuments.SSL_KS);
        //fis = FileUtils.loadFileSafely(sslkeystorepath);
        final String RIKER_KEYSTORE_PASSWORD = "blahblah";
        keyStore.load(fis, RIKER_KEYSTORE_PASSWORD.toCharArray());
        fis.close();
        final String RIKER_PRIVATE_KEY_PASSWORD = "blahblah";
        final String RIKER_PRIVATE_KEY_ALIAS = "tomcat";
        PrivateKey output = (PrivateKey)keyStore.getKey(RIKER_PRIVATE_KEY_ALIAS,
                                                        RIKER_PRIVATE_KEY_PASSWORD.toCharArray());
        return output;
    }

    private static X509Certificate getDotNetRecipientCertificate() throws Exception {
        InputStream fis = TestDocuments.getInputStream(TestDocuments.SSL_CER);
        byte[] certbytes;
        try {
            certbytes = HexUtils.slurpStream(fis, 16384);
        } finally {
            fis.close();
        }
        // construct the x509 based on the bytes
        return (X509Certificate)(CertificateFactory.getInstance("X.509").
                                 generateCertificate(new ByteArrayInputStream(certbytes)));
    }
}
