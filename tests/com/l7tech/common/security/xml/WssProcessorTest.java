/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.server.secureconversation.SecureConversationSession;
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

    private void doTest(TestDocument testDocument) throws Exception {
        WssProcessor wssProcessor = new WssProcessorImpl();
        Document request = testDocument.document;
        X509Certificate recipientCertificate = testDocument.recipientCertificate;
        PrivateKey recipientPrivateKey = testDocument.recipientPrivateKey;


        log.info("Testing document: " + testDocument.name);
        log.info("Original decorated message (reformatted): " + XmlUtil.nodeToFormattedString(request));
        WssProcessor.ProcessorResult result = wssProcessor.undecorateMessage(request,
                                                                             recipientCertificate,
                                                                             recipientPrivateKey,
                                                                             testDocument.securityContextFinder);
        assertTrue(result != null);

        WssProcessor.ParsedElement[] encrypted = result.getElementsThatWereEncrypted();
        assertTrue(encrypted != null);
        if (encrypted.length > 0) {
            log.info("The following elements were encrypted:");
            for (int j = 0; j < encrypted.length; j++) {
                Element element = encrypted[j].asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were encrypted.");

        WssProcessor.SignedElement[] signed = result.getElementsThatWereSigned();
        assertTrue(signed != null);
        if (signed.length > 0) {
            log.info("The following elements were signed:");
            for (int j = 0; j < signed.length; j++) {
                Element element = signed[j].asElement();
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
                if (token instanceof WssProcessor.SamlSecurityToken) {
                    log.info("Possession proved: " + ((WssProcessor.SamlSecurityToken)token).isPossessionProved());
                    log.info("  " + ((WssProcessor.SamlSecurityToken)token).getSubjectCertificate());                    
                } else if (token instanceof WssProcessor.X509SecurityToken) {
                    log.info("Possession proved: " + ((WssProcessor.X509SecurityToken)token).isPossessionProved());
                    log.info("  " + token);
                } else {
                    log.info("  " + token);
                }
            }
        } else
            log.info("No security tokens were found.");

        WssProcessor.Timestamp timestamp = result.getTimestamp();
        if (timestamp != null) {
            log.info("Timestamp created = " + timestamp.getCreated().asDate());
            log.info("Timestamp expires = " + timestamp.getExpires().asDate());
        } else
            log.info("No timestamp was found.");

        Document undecorated = result.getUndecoratedMessage();
        assertTrue(undecorated != null);
        log.info("Undecorated document:\n" + XmlUtil.nodeToFormattedString(undecorated));
        log.info("Security namespace observed:\n" + result.getSecurityNS());
    }

    private static class TestDocument {
        String name;
        Document document;
        PrivateKey recipientPrivateKey;
        X509Certificate recipientCertificate;
        WssProcessor.SecurityContextFinder securityContextFinder = null;
        TestDocument(String n, Document d, PrivateKey rpk, X509Certificate rc,
                     WssProcessor.SecurityContextFinder securityContextFinder)
        {
            this.name = n;
            this.document = d;
            this.recipientPrivateKey = rpk;
            this.recipientCertificate = rc;
            this.securityContextFinder = securityContextFinder;
        }
    }

    public void testDotnetEncryptedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet encrypted request", TestDocuments.DOTNET_ENCRYPTED_REQUEST));
    }

    public void testDotnetSignedRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request", TestDocuments.DOTNET_SIGNED_REQUEST));
    }

    public void testDotnetRequestWithUsernameToken() throws Exception {
        doTest(makeDotNetTestDocument("dotnet request with username token", TestDocuments.DOTNET_USERNAME_TOKEN));
    }

    public void testEttkSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed request", TestDocuments.ETTK_SIGNED_REQUEST));
    }

    public void testEttkEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk encrypted request", TestDocuments.ETTK_ENCRYPTED_REQUEST));
    }

    public void testEttkSignedEncryptedRequest() throws Exception {
        doTest(makeEttkTestDocument("ettk signed encrypted request", TestDocuments.ETTK_SIGNED_ENCRYPTED_REQUEST));
    }

    public void testRequestWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request wrapped l7 actor", TestDocuments.WRAPED_L7ACTOR));
    }

    public void testRequestMultipleWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request multiple wrapped l7 actor", TestDocuments.MULTIPLE_WRAPED_L7ACTOR));
    }

    public void testDotnetSignedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed SecureConversation request", TestDocuments.DOTNET_SIGNED_USING_DERIVED_KEY_TOKEN));
    }

    public void testDotnetSignedEncryptedSecureConversationRequest() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed encrypted SecureConversation request", TestDocuments.DOTNET_ENCRYPTED_USING_DERIVED_KEY_TOKEN));
    }

    public void testDotnetSignedRequest2() throws Exception {
        doTest(makeDotNetTestDocument("dotnet signed request 2", TestDocuments.DOTNET_SIGNED_REQUEST2));
    }

    public void testWebsphereSignedRequest() throws Exception {
        doTest(makeEttkTestDocument("websphere signed request", TestDocuments.WEBSPHERE_SIGNED_REQUEST));
    }

    public void testFrancisAgentSignedRequest() throws Exception {
        doTest(makeDotNetTestDocument("agent failing signed request", TestDocuments.AGENT_FAILING_SIGNED_REQUEST));
    }

    public void testSampleSignedSamlHolderOfKeyRequest() throws Exception {
        doTest(makeEttkTestDocument("sample signed SAML holder-of-key request",
                                    TestDocuments.SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST));
    }

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(),
                                    null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TestDocument makeDotNetTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            final SecureConversationSession session = new SecureConversationSession();

            // Set up a fake ws-sc session, in case this example will be needing it
            session.setSharedSecret(TestDocuments.getDotNetSecureConversationSharedSecret());
            WssProcessor.SecurityContextFinder dotNetSecurityContextFinder = new WssProcessor.SecurityContextFinder() {
                public WssProcessor.SecurityContext getSecurityContext(String securityContextIdentifier) {
                    return session;
                }
            };

            return new TestDocument(testname, d,
                                    TestDocuments.getDotNetServerPrivateKey(),
                                    TestDocuments.getDotNetServerCertificate(),
                                    dotNetSecurityContextFinder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
