package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerNonSoapEncryptElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapEncryptElementAssertionTest.class.getName());

    static String recipb64;
    static X509Certificate recipCert;
    static PrivateKey recipPrivateKey;

    static final String TEST_XML =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <par:password>somepassword</par:password> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

    @Before
    public void setUpCert() throws Exception {
        Pair<X509Certificate, PrivateKey> got = TestKeys.getCertAndKey("RSA_1024");
        recipCert = got.left;
        recipCert.checkValidity();
        recipPrivateKey = got.right;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());
        logger.info("Recipient certificate PKCS#12 keystore: \n" + TestCertificateGenerator.convertToBase64Pkcs12(got.left, got.right));
    }

    @Test
    public void testEncryptElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    @BugNumber(10270)
    public void testEncryptForExpiredCert() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(2005,1,1);
        Date notBefore = cal.getTime();
        cal.set(2006,1,1);
        Date notAfter = cal.getTime();

        Pair<X509Certificate, PrivateKey> got = new TestCertificateGenerator().keyPair(new KeyPair(recipCert.getPublicKey(), recipPrivateKey)).notBefore(notBefore).notAfter(notAfter).generateWithKey();
        recipCert = got.left;
        recipPrivateKey = got.right;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());

        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    @BugNumber(11191)
    public void testEncryptEmptyElement() throws Exception {
        Message req = makeReq(TEST_XML.replace("<par:password>somepassword</par:password>", "<par:password/>"));
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setEncryptContentsOnly(true);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
        String reqString = toString(req);
        System.out.println("Encrypted message: " + reqString);
        assertTrue("only contents of password element should be encrypted, not its open and close tags as well", reqString.contains("<par:password><xenc:EncryptedData"));
        // the encryption round trip test can take care of testing that the <EncryptedData>, when decrypted, vanishes completely (results in 0 bytes of plaintext element content)
    }

    @Test
    @BugNumber(12600)
    public void testEncryptContentsOnly() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setEncryptContentsOnly(true);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
        final String reqString = toString(req);
        System.out.println("Encrypted message: " + reqString);
        assertTrue("only contents of password element should be encrypted, not its open and close tags as well", reqString.contains("<par:password><xenc:EncryptedData"));
    }

    private String toString(Message req) {
        try {
            return new String(IOUtils.slurpStream(req.getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidXpathException.class)
    public void testEncryptElement_withBadXpath() throws Exception {
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("[[[[["));
        new ServerNonSoapEncryptElementAssertion(ass);
    }

    @Test(expected = InvalidXpathException.class)
    public void testEncryptElement_withNullXpath() throws Exception {
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(null);
        new ServerNonSoapEncryptElementAssertion(ass);
    }

    @Test
    public void testEncryptElement_withBadVariable() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = $somevariable]"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("somevariable", "passwordd");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
        checkResult(req, 0);
    }

    @Test
    public void testEncryptElement_withVariable() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = $somevariable]"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("somevariable", "password");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    public void testEncryptElement_withNamespaceUri() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("//par:username", new HashMap<String, String>() {{
            put("par", "urn:noapar");
        }}));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    public void testEncryptMultipleElements() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/*/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 3);
    }

    @Test
    public void testEncryptDocumentElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    public void testEncryptElement_nomatch() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/nonexistent"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.FALSIFIED, result);
        checkResult(req, 0);
    }

    @Test
    public void testEncryptElement_notAnElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("3=4"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.FAILED, result);
        checkResult(req, 0);
    }

    @Test
    public void testEncryptElementWithAes128Gcm() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXencAlgorithm(XencUtil.AES_128_GCM);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        String doc = checkResult(req, 1);
        assertTrue(doc.contains(XencUtil.AES_128_GCM));
    }

    @Test
    public void testEncryptElementWithAes256Gcm() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXencAlgorithm(XencUtil.AES_256_GCM);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        String doc = checkResult(req, 1);
        assertTrue(doc.contains(XencUtil.AES_256_GCM));
    }

    @BugNumber(11697)
    @Test
    public void testEncryptWithTypeAndRecipientAttributes() throws Exception {
        // No context variables, also validate delegated getters and setters in assertion
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setIncludeEncryptedDataTypeAttribute(true);
        assertTrue(ass.isIncludeEncryptedDataTypeAttribute());
        final String customUri = "customuri";
        ass.setEncryptedDataTypeAttribute(customUri);
        assertEquals(customUri, ass.getEncryptedDataTypeAttribute());
        final String recipientValue = "im not a URI %";
        ass.setEncryptedKeyRecipientAttribute(recipientValue);
        assertEquals(recipientValue, ass.getEncryptedKeyRecipientAttribute());
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1, customUri, recipientValue);
    }

    @Test
    public void testEncryptWithAttributes_ContextVariables_CertAsBase64() throws Exception {
        // With context variables - Cert specified as base64 via a variable
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = new NonSoapEncryptElementAssertion();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setEncryptContentsOnly(false);
        ass.setXencAlgorithm(XencUtil.AES_256_CBC);
        ass.setRecipientCertContextVariableName("cert");
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'password']"));

        ass.setIncludeEncryptedDataTypeAttribute(true);
        final String customUri = "customuri";
        ass.setEncryptedDataTypeAttribute(customUri + "${var1}");
        final String recipientValue = "im not a URI %";
        ass.setEncryptedKeyRecipientAttribute(recipientValue + "${var1}");
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("var1", "1");
        context.setVariable("cert", recipb64);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1, customUri + "1", recipientValue + "1");
    }

    @Test
    public void testEncryptWithAttributes_ContextVariables_CertAsX509Object() throws Exception {
        // Same again but with Cert specified as X509Certificate via a variable
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = new NonSoapEncryptElementAssertion();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setEncryptContentsOnly(false);
        ass.setXencAlgorithm(XencUtil.AES_256_CBC);
        ass.setRecipientCertContextVariableName("cert");
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'password']"));

        ass.setIncludeEncryptedDataTypeAttribute(true);
        final String customUri = "customuri";
        ass.setEncryptedDataTypeAttribute(customUri + "${var1}");
        final String recipientValue = "im not a URI %";
        ass.setEncryptedKeyRecipientAttribute(recipientValue + "${var1}");
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("var1", "1");
        context.setVariable("cert", CertUtils.decodeFromPEM(recipb64, false));
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1, customUri + "1", recipientValue + "1");
    }

    @Test
    public void testEncryptWithInvalidTypeAttribute() throws Exception {
        // Non URI resolved for Type attribute
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setIncludeEncryptedDataTypeAttribute(true);
        final String customUri = "not a uri";
        ass.setEncryptedDataTypeAttribute(customUri);
        final String recipientValue = "im not a URI %";
        ass.setEncryptedKeyRecipientAttribute(recipientValue);
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(sass, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final AssertionStatus result = sass.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()));
        assertEquals("Assertion should have throw for invalid uri", AssertionStatus.SERVER_ERROR, result);

        // validate audits
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("Type attribute for EncryptedData is not a valid URI: 'not a uri'"));
    }

    public static NonSoapEncryptElementAssertion makeAss() {
        NonSoapEncryptElementAssertion ass = new NonSoapEncryptElementAssertion();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setEncryptContentsOnly(false);
        ass.setXencAlgorithm(XencUtil.AES_256_CBC);
        ass.setRecipientCertificateBase64(recipb64);
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'password']"));
        return ass;
    }

    public static Message makeReq() {
        return makeReq(TEST_XML);
    }

    static Message makeReq(String testXml) {
        return new Message(XmlUtil.stringAsDocument(testXml));
    }

    private String checkResult(Message req,
                               int expectedLength) throws SAXException, IOException {
        return checkResult(req, expectedLength, null, null);
    }

    private String checkResult(Message req,
                               int expectedLength,
                               @Nullable String expectedEncryptedDataTypeAttribute,
                               @Nullable String expectedEncryptedKeyRecipientAttribute) throws SAXException, IOException {
        final Document doc = req.getXmlKnob().getDocumentReadOnly();
        final String docString = XmlUtil.nodeToString(doc);
        logger.info("Encrypted result: \n" + docString);
        NodeList nodeList = doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData");
        assertTrue(nodeList.getLength() == expectedLength);
        for (int i = 0; i < expectedLength; ++i){
            final Node encryptedDataNode = nodeList.item(i);
            assertEquals("EncryptedData", encryptedDataNode.getLocalName());
            if (expectedEncryptedDataTypeAttribute != null) {
                final NamedNodeMap attributes = encryptedDataNode.getAttributes();
                final Node type = attributes.getNamedItem("Type");
                assertNotNull(type);
                assertEquals(expectedEncryptedDataTypeAttribute, type.getTextContent());
            }

            if (expectedEncryptedKeyRecipientAttribute != null) {
                final Element encryptedKey = DomUtils.findFirstDescendantElement((Element) encryptedDataNode, "http://www.w3.org/2001/04/xmlenc#", "EncryptedKey");
                assertNotNull(encryptedKey);
                final NamedNodeMap attributes = encryptedKey.getAttributes();
                final Node type = attributes.getNamedItem("Recipient");
                assertNotNull(type);
                assertEquals(expectedEncryptedKeyRecipientAttribute, type.getTextContent());
            }
        }

        assertWellFormed(doc);
        return docString;
    }

    private void assertWellFormed(Document doc) {
        try {
            XmlUtil.stringToDocument(XmlUtil.nodeToString(doc));
        } catch (IOException e) {
            throw (AssertionError)new AssertionError("IOException serializing and reparsing XML document: " + e.getMessage()).initCause(e);
        } catch (SAXException e) {
            throw (AssertionError)new AssertionError("XML document is not well-formed: " + e.getMessage()).initCause(e);
        }
    }
}
