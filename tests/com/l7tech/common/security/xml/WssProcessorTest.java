/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
<<<<<<< WssProcessorTest.java
=======
 *
 * $Id$
>>>>>>> 1.37.4.3
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.message.Message;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SignedSamlTest;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
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
        CertificateResolver certificateResolver = testDocument.certificateResolver;


        log.info("Testing document: " + testDocument.name);
        log.info("Original decorated message (reformatted): " + XmlUtil.nodeToFormattedString(request));
        ProcessorResult result = wssProcessor.undecorateMessage(new Message(request),
                                                                testDocument.senderCeritifcate,
                                                                recipientCertificate,
                                                                recipientPrivateKey,
                                                                testDocument.securityContextFinder,
                                                                certificateResolver);
        assertTrue(result != null);

        ParsedElement[] encrypted = result.getElementsThatWereEncrypted();
        assertTrue(encrypted != null);
        if (encrypted.length > 0) {
            log.info("The following elements were encrypted:");
            for (int j = 0; j < encrypted.length; j++) {
                Element element = encrypted[j].asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were encrypted.");

        SignedElement[] signed = result.getElementsThatWereSigned();
        assertTrue(signed != null);
        if (signed.length > 0) {
            log.info("The following elements were signed:");
            for (int j = 0; j < signed.length; j++) {
                Element element = signed[j].asElement();
                log.info("  " + element.getNodeName() + " (" + element.getNamespaceURI() + ")");
            }
        } else
            log.info("No elements were signed.");


        SecurityToken[] tokens = result.getSecurityTokens();
        assertTrue(tokens != null);
        if (tokens.length > 0) {
            log.info("The following security tokens were found:");
            for (int j = 0; j < tokens.length; j++) {
                SecurityToken token = tokens[j];
                if (token instanceof SamlSecurityToken) {
                    log.info("Possession proved: " + ((SamlSecurityToken)token).isPossessionProved());
                    log.info("  " + ((SamlSecurityToken)token).getSubjectCertificate());
                } else if (token instanceof X509SecurityToken) {
                    log.info("Possession proved: " + ((X509SecurityToken)token).isPossessionProved());
                    log.info("  " + token);
                } else {
                    log.info("  " + token);
                }
            }
        } else
            log.info("No security tokens were found.");

        WssTimestamp timestamp = result.getTimestamp();
        if (timestamp != null) {
            log.info("Timestamp created = " + timestamp.getCreated().asDate());
            log.info("Timestamp expires = " + timestamp.getExpires().asDate());
        } else
            log.info("No timestamp was found.");

        log.info("Undecorated document:\n" + XmlUtil.nodeToFormattedString(request));
        log.info("Security namespace observed:\n" + result.getSecurityNS());
        log.info("WSU namespace observed:\n" + result.getWSUNS());
    }

    private static class TestDocument {
        String name;
        Document document;
        X509Certificate senderCeritifcate;
        PrivateKey recipientPrivateKey;
        X509Certificate recipientCertificate;
        SecurityContextFinder securityContextFinder = null;
        CertificateResolver certificateResolver;

        TestDocument(String n, Document d, PrivateKey rpk, X509Certificate rc,
                     SecurityContextFinder securityContextFinder, X509Certificate senderCert,
                     CertificateResolver certificateResolver)
        {
            this.name = n;
            this.document = d;
            this.recipientPrivateKey = rpk;
            this.recipientCertificate = rc;
            this.securityContextFinder = securityContextFinder;
            this.senderCeritifcate = senderCert;
            this.certificateResolver = certificateResolver;
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

    /*public void testRequestWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request wrapped l7 actor", TestDocuments.WRAPED_L7ACTOR));
    }

    public void testRequestMultipleWrappedL7Actor() throws Exception {
        doTest(makeDotNetTestDocument("request multiple wrapped l7 actor", TestDocuments.MULTIPLE_WRAPED_L7ACTOR));
    }*/

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
        doTest(makeDotNetTestDocument("Bridge failing signed request", TestDocuments.BRIDGE_FAILING_SIGNED_REQUEST));
    }

    public void testSampleSignedSamlHolderOfKeyRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest("blah");
        sst.setUp();
        doTest(makeEttkTestDocument("sample signed SAML holder-of-key request",
                                    /*TestDocuments.SAMPLE_SIGNED_SAML_HOLDER_OF_KEY_REQUEST*/sst.getRequestSignedWithSamlToken(false, false)));
    }

    public void testSignedSamlSenderVouchesRequest() throws Exception {
        SignedSamlTest sst = new SignedSamlTest("blah");
        sst.setUp();
        doTest(makeEttkTestDocument("Signed SAML sender-vouches request",
                                    sst.getSignedRequestWithSenderVouchesToken()));
    }

    public void testNonSoapRequest() throws Exception {
        try {
            doTest(makeEttkTestDocument("non-SOAP request",
                                        TestDocuments.NON_SOAP_REQUEST));
            fail("Expected MessageNotSoapException was not thrown");
        } catch (MessageNotSoapException e) {
            // Ok
        }
    }

    public void testWssInterop2005JulyRequest() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_REQUEST);

            result = new TestDocument("WssInterop2005JulyRequest", d,
                                                TestDocuments.getWssInteropBobKey(),
                                                TestDocuments.getWssInteropBobCert(),
                                                null,
                                                TestDocuments.getWssInteropAliceCert(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        doTest(result);
    }

    public void testKeyInfoThumbprintRequest() throws Exception {
        TestDocument result;
        try {
            Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "/keyinfothumbreq.xml");

            CertificateResolver certificateResolver = new SimpleCertificateResolver(TestDocuments.getWssInteropAliceCert());
            result = new TestDocument("KeyInfoThumbprintRequest",
                                      d,
                                      null,
                                      null,
                                      null,
                                      null,
                                      certificateResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        doTest(result);
    }

    public void testSignedSvAssertionWithThumbprintSha1() throws Exception {
        TestDocument r;
        Document ass = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedSvThumbAssertion.xml");
        Document d = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element security = SoapUtil.getOrMakeSecurityElement(d);
        security.appendChild(d.importNode(ass.getDocumentElement(), true));
        CertificateResolver certificateResolver = new SimpleCertificateResolver(TestDocuments.getDotNetServerCertificate());
        r = new TestDocument("SignedSvAssertionWithThumbprintSha1",
                             d,
                             null,
                             null,
                             null,
                             null,
                             certificateResolver);
        doTest(r);
    }

    public void testCompleteEggRequest() throws Exception {
        Document d = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/ValidBlueCardRequest.xml");

        Element sec = SoapUtil.getSecurityElement(d);
        Element ass = XmlUtil.findFirstChildElementByName(sec, SamlConstants.NS_SAML, "Assertion");

        Document assDoc = TestDocuments.getTestDocument(TestDocuments.DIR + "/egg/generatedAttrThumbAssertion.xml");
        sec.replaceChild(d.importNode(assDoc.getDocumentElement(), true), ass);

        //XmlUtil.nodeToOutputStream(d, new FileOutputStream("c:/eggerequest.xml"));

        CertificateResolver certificateResolver = new SimpleCertificateResolver(TestDocuments.getDotNetServerCertificate());
        TestDocument td = new TestDocument("CompleteEggRequest",
                                           d,
                                           null,
                                           null,
                                           null,
                                           null,
                                           certificateResolver);
        doTest(td);
    }

    public void testWssInterop2005JulyResponse() throws Exception {
        TestDocument result;
        Document d = TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_RESPONSE);

        result = new TestDocument("WssInterop2005JulyResponse", d,
                                  TestDocuments.getWssInteropAliceKey(),
                                  TestDocuments.getWssInteropAliceCert(),
                                  null,
                                  TestDocuments.getWssInteropBobCert(),
                                  null);
        doTest(result);
    }

    private TestDocument makeEttkTestDocument(String testname, String docname) {
        try {
            Document d = TestDocuments.getTestDocument(docname);
            return new TestDocument(testname, d,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(),
                                    null,
                                    null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestDocument makeEttkTestDocument(String testname, Document doc) {
        try {
            return new TestDocument(testname, doc,
                                    TestDocuments.getEttkServerPrivateKey(),
                                    TestDocuments.getEttkServerCertificate(),
                                    null,
                                    null, null);
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
            SecurityContextFinder dotNetSecurityContextFinder = new SecurityContextFinder() {
                public SecurityContext getSecurityContext(String securityContextIdentifier) {
                    return session;
                }
            };

            return new TestDocument(testname, d,
                                    TestDocuments.getDotNetServerPrivateKey(),
                                    TestDocuments.getDotNetServerCertificate(),
                                    dotNetSecurityContextFinder,
                                    null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
