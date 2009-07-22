package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapEncryptElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapEncryptElementAssertionTest.class.getName());

    static String recipb64;
    static X509Certificate recipCert;
    static PrivateKey recipPrivateKey;

    @BeforeClass
    public static void setUpCert() throws Exception {
        Pair<X509Certificate, PrivateKey> got = new TestCertificateGenerator().daysUntilExpiry(5000).generateWithKey();
        recipCert = got.left;
        recipPrivateKey = got.right;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());
        logger.info("Recipient certificate PKCS#12 keystore: \n" + TestCertificateGenerator.convertToBase64Pkcs12(got.left, got.right));
    }
        
    @Test
    public void testEncryptElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus result = sass.checkRequest(new PolicyEnforcementContext(req, new Message()));
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test(expected = InvalidXpathException.class)
    public void testEncryptElement_withBadXpath() throws Exception {
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("[[[[["));
        new ServerNonSoapEncryptElementAssertion(ass, null);
    }

    @Test(expected = InvalidXpathException.class)
    public void testEncryptElement_withNullXpath() throws Exception {
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(null);
        new ServerNonSoapEncryptElementAssertion(ass, null);
    }

    @Test
    public void testEncryptElement_withBadVariable() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = $somevariable]"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        final PolicyEnforcementContext context = new PolicyEnforcementContext(req, new Message());
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
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        final PolicyEnforcementContext context = new PolicyEnforcementContext(req, new Message());
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
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        final PolicyEnforcementContext context = new PolicyEnforcementContext(req, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    public void testEncryptMultipleElements() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/*/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus result = sass.checkRequest(new PolicyEnforcementContext(req, new Message()));
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 3);
    }

    @Test
    public void testEncryptDocumentElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/*"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus result = sass.checkRequest(new PolicyEnforcementContext(req, new Message()));
        assertEquals(AssertionStatus.NONE, result);
        checkResult(req, 1);
    }

    @Test
    public void testEncryptElement_nomatch() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("/nonexistent"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus result = sass.checkRequest(new PolicyEnforcementContext(req, new Message()));
        assertEquals(AssertionStatus.FALSIFIED, result);
        checkResult(req, 0);
    }

    @Test
    public void testEncryptElement_notAnElement() throws Exception {
        Message req = makeReq();
        NonSoapEncryptElementAssertion ass = makeAss();
        ass.setXpathExpression(new XpathExpression("3=4"));
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus result = sass.checkRequest(new PolicyEnforcementContext(req, new Message()));
        assertEquals(AssertionStatus.FAILED, result);
        checkResult(req, 0);
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
        return new Message(XmlUtil.stringAsDocument(
                "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
                "  <par:username>brian</par:username> \n" +
                "  <par:password>somepassword</par:password> \n" +
                "  <par:notice_id>12345</par:notice_id> \n" +
                "</par:GetNoaParties>"));
    }

    private void checkResult(Message req, int expectedLength) throws SAXException, IOException {
        final Document doc = req.getXmlKnob().getDocumentReadOnly();
        logger.info("Encrypted result: \n" + XmlUtil.nodeToString(doc));
        NodeList nodeList = doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData");
        assertTrue(nodeList.getLength() == expectedLength);
        for (int i = 0; i < expectedLength; ++i)
            assertEquals("EncryptedData", nodeList.item(i).getLocalName());
        assertWellFormed(doc);
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
