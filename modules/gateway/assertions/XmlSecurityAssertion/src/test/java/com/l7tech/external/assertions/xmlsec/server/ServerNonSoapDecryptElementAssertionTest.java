package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.XencUtilTest;
import com.l7tech.security.xml.XmlElementEncryptor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class ServerNonSoapDecryptElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapDecryptElementAssertionTest.class.getName());

    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
        }});
    }

    @Test
    public void testDecryptElement() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertEquals(1, ((Object[])context.getVariable("elementsDecrypted")).length);
        assertEquals(1, ((Object[])context.getVariable("encryptionMethodUris")).length);
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes256-cbc", ((String[])context.getVariable("encryptionMethodUris"))[0]);
    }

    @Test
    @BugNumber(11320)
    public void testDecryptElementAes128Gcm() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED_AES128GCM);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED_AES128GCM));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertEquals(1, ((Object[])context.getVariable("elementsDecrypted")).length);
        assertEquals(1, ((Object[])context.getVariable("encryptionMethodUris")).length);
        assertEquals("http://www.w3.org/2009/xmlenc11#aes128-gcm", ((String[])context.getVariable("encryptionMethodUris"))[0]);
    }

    @Test
    public void testDecryptElement_encryptedForSomeoneElse() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED_FOR_SOMEONE_ELSE);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED_FOR_SOMEONE_ELSE));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.BAD_REQUEST, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("After decryption attempt:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertNoVariable(context, "elementsDecrypted");
        assertNoVariable(context, "encryptionMethodUris");
    }

    @Test
    public void testDecryptElement_encryptedForData() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED_FOR_DATA);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED_FOR_DATA));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertEquals(1, ((Object[])context.getVariable("elementsDecrypted")).length);
        assertEquals(1, ((Object[])context.getVariable("encryptionMethodUris")).length);
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes256-cbc", ((String[])context.getVariable("encryptionMethodUris"))[0]);
    }

    @Test
    @BugNumber(11191)
    public void testDecryptEmptyElementWithoutReport() throws Exception {
        Pair<Element, XencUtil.XmlEncKey> pair = XencUtilTest.makeEncryptedContentElement(null);
        Element element = pair.left;
        XencUtil.XmlEncKey key = pair.right;

        Element encryptedKey = XmlElementEncryptor.createEncryptedKey(element.getOwnerDocument(), NonSoapXmlSecurityTestUtils.getTestKey().getCertificate(), key);
        final Element encDataElement = XmlUtil.findExactlyOneChildElement(element);
        XmlElementEncryptor.insertKeyInfoAndDeUglifyNamespaces(encDataElement, encryptedKey, key.getAlgorithm());

        String xml = XmlUtil.nodeToString(element.getOwnerDocument());
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));

        ServerNonSoapDecryptElementAssertion sass = new ServerNonSoapDecryptElementAssertion(ass, beanFactory);
        Message request = new Message(XmlUtil.stringAsDocument(xml));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        assertEquals(AssertionStatus.NONE, sass.checkRequest(context));
        assertEquals("<foo><blah/></foo>", XmlUtil.nodeToString(request.getXmlKnob().getDocumentReadOnly(), false));

        assertNoVariable(context, "contentsOnly");
    }

    @Test
    @BugNumber(12600)
    public void testDecryptEmptyElementWithAttrsWithReport() throws Exception {
        Pair<Element, XencUtil.XmlEncKey> pair = XencUtilTest.makeEncryptedContentElement("<foo><blah a1=\"val1\" a2=\"val2\"/></foo>");
        Element element = pair.left;
        XencUtil.XmlEncKey key = pair.right;

        Element encryptedKey = XmlElementEncryptor.createEncryptedKey(element.getOwnerDocument(), NonSoapXmlSecurityTestUtils.getTestKey().getCertificate(), key);
        final Element encDataElement = XmlUtil.findExactlyOneChildElement(element);
        XmlElementEncryptor.insertKeyInfoAndDeUglifyNamespaces(encDataElement, encryptedKey, key.getAlgorithm());

        String xml = XmlUtil.nodeToString(element.getOwnerDocument());
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setReportContentsOnly(true);
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));

        ServerNonSoapDecryptElementAssertion sass = new ServerNonSoapDecryptElementAssertion(ass, beanFactory);
        Message request = new Message(XmlUtil.stringAsDocument(xml));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        assertEquals(AssertionStatus.NONE, sass.checkRequest(context));
        assertEquals("<foo><blah a1=\"val1\" a2=\"val2\"/></foo>", XmlUtil.nodeToString(request.getXmlKnob().getDocumentReadOnly(), false));

        final Element[] elementsDecrypted = (Element[]) context.getVariable("elementsDecrypted");
        assertEquals(1, elementsDecrypted.length);
        final Element expectedElement = XmlUtil.findFirstChildElement(request.getXmlKnob().getDocumentReadOnly().getDocumentElement());
        assertEquals(expectedElement, elementsDecrypted[0]);

        final Boolean[] contentsOnly = (Boolean[]) context.getVariable("contentsOnly");
        assertEquals(elementsDecrypted.length, contentsOnly.length);
        assertEquals(Boolean.TRUE, contentsOnly[0]);
    }

    @Test
    @BugNumber(12600)
    public void testDecryptElementContentWithAttrsWithReport() throws Exception {
        Pair<Element, XencUtil.XmlEncKey> pair = XencUtilTest.makeEncryptedContentElement("<foo><blah a1=\"val1\" a2=\"val2\">Test with <some/> content <really><yeah/></really></blah></foo>");
        Element element = pair.left;
        XencUtil.XmlEncKey key = pair.right;

        Element encryptedKey = XmlElementEncryptor.createEncryptedKey(element.getOwnerDocument(), NonSoapXmlSecurityTestUtils.getTestKey().getCertificate(), key);
        final Element encDataElement = XmlUtil.findExactlyOneChildElement(element);
        XmlElementEncryptor.insertKeyInfoAndDeUglifyNamespaces(encDataElement, encryptedKey, key.getAlgorithm());

        String xml = XmlUtil.nodeToString(element.getOwnerDocument());
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setReportContentsOnly(true);
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));

        ServerNonSoapDecryptElementAssertion sass = new ServerNonSoapDecryptElementAssertion(ass, beanFactory);
        Message request = new Message(XmlUtil.stringAsDocument(xml));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        assertEquals(AssertionStatus.NONE, sass.checkRequest(context));
        assertEquals("<foo><blah a1=\"val1\" a2=\"val2\">Test with <some/> content <really><yeah/></really></blah></foo>", XmlUtil.nodeToString(request.getXmlKnob().getDocumentReadOnly(), false));

        final Element[] elementsDecrypted = (Element[]) context.getVariable("elementsDecrypted");
        assertEquals(3, elementsDecrypted.length);
        final Element expectedElement = XmlUtil.findFirstChildElement(request.getXmlKnob().getDocumentReadOnly().getDocumentElement());
        int index = indexOf(elementsDecrypted, expectedElement);
        assertTrue(index >= 0);

        final Boolean[] contentsOnly = (Boolean[]) context.getVariable("contentsOnly");
        assertEquals(elementsDecrypted.length, contentsOnly.length);
        assertEquals(Boolean.TRUE, contentsOnly[index]);
    }

    @Test
    @BugNumber(12600)
    public void testDecryptElementContentWithAttrs() throws Exception {
        Pair<Element, XencUtil.XmlEncKey> pair = XencUtilTest.makeEncryptedContentElement("<foo><blah a1=\"val1\" a2=\"val2\">Test with <some/> content <really><yeah/></really></blah></foo>");
        Element element = pair.left;
        XencUtil.XmlEncKey key = pair.right;

        Element encryptedKey = XmlElementEncryptor.createEncryptedKey(element.getOwnerDocument(), NonSoapXmlSecurityTestUtils.getTestKey().getCertificate(), key);
        final Element encDataElement = XmlUtil.findExactlyOneChildElement(element);
        XmlElementEncryptor.insertKeyInfoAndDeUglifyNamespaces(encDataElement, encryptedKey, key.getAlgorithm());

        String xml = XmlUtil.nodeToString(element.getOwnerDocument());
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));

        ServerNonSoapDecryptElementAssertion sass = new ServerNonSoapDecryptElementAssertion(ass, beanFactory);
        Message request = new Message(XmlUtil.stringAsDocument(xml));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        assertEquals(AssertionStatus.NONE, sass.checkRequest(context));
        assertEquals("<foo><blah a1=\"val1\" a2=\"val2\">Test with <some/> content <really><yeah/></really></blah></foo>", XmlUtil.nodeToString(request.getXmlKnob().getDocumentReadOnly(), false));

        final Element[] elementsDecrypted = (Element[]) context.getVariable("elementsDecrypted");
        assertEquals(2, elementsDecrypted.length); // for <some/> and <really>...</really> immediate child elements
        final Element expectedElement = XmlUtil.findFirstChildElement(request.getXmlKnob().getDocumentReadOnly().getDocumentElement());
        int index = indexOf(elementsDecrypted, expectedElement);
        assertTrue("with reportContentOnly=false, content-only elements shall not be included in the reported decryption results", index == -1);
        assertNoVariable(context, "contentsOnly");
    }

    private int indexOf(Object[] items, Object item) {
        for (int i = 0; i < items.length; i++) {
            Object o = items[i];
            if (item == o)
                return i;
        }
        return -1;
    }

    private void assertNoVariable(PolicyEnforcementContext context, String variableName) {
        try {
            context.getVariable(variableName);
            fail("Should have thrown NoSuchVariableException for " + variableName);
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

    public static final String TEST_ENCRYPTED =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:X509Data><dsig:X509IssuerSerial><dsig:X509IssuerName>CN=test</dsig:X509IssuerName><dsig:X509SerialNumber>7730273685284174799</dsig:X509SerialNumber></dsig:X509IssuerSerial></dsig:X509Data></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>O0xs2VQa0p3d9tzUvy+2ljjgef/RX2zDMo8FIQ9rjYCCRKDFEsLb5XOFQWK5MtnTl+bC68khTfJq6FeKh+3NBI9D41BJipZAWAI+HZrnyU0iUPSJL936AAWsq9bgL+RkaGkXjsWAjb/XCluDMlQ+9pK2CiLoyIMRSHirES72vSM=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>B3fvKCstaZwlTxSvsFBHZnoEobEjqbIy0P+hRxhotFy9vxacIfbpQRMrYbpRh4lJEUdgiDoD6JVeNT1xWJmakQ==</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

    public static final String TEST_ENCRYPTED_AES128GCM =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:X509Data><dsig:X509IssuerSerial><dsig:X509IssuerName>CN=test_rsa_1024</dsig:X509IssuerName><dsig:X509SerialNumber>15005762314007893580</dsig:X509SerialNumber></dsig:X509IssuerSerial></dsig:X509Data></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>Aag2dhLqgLmNGSIUUlcxmc+D4H3OZtb2tDxBMX7tkYKpTJwcJVqi6YWWYneRRe++7+pWGVfUDtGmnKoIXNlyJXF055iBwc8Lq65GXog/oXbo8PUyYqtItZRmRhtyw6199TJgczjSPmzmXaX192iPOiIzqa4Ud8bBTST9vb6z/Q4=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>S/fXPTNecZBGD1pZay3t2H6D8tQeS0WMS8g73aZ6C530uZN/oZLpkOPcZ7KGr2o9LIprOY4CCx1eIv2thWOswdDnKpGZ</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

    public static String TEST_ENCRYPTED_FOR_DATA =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "\t<par:username>brian</par:username>\n" +
            "\t<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "\t\t\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t\t\t<dsig:X509Data>\n" +
            "\t\t\t\t\t\t<dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t\t\t<dsig:X509IssuerName>CN=data.l7tech.com</dsig:X509IssuerName>\n" +
            "\t\t\t\t\t\t\t<dsig:X509SerialNumber>2750606400783968375</dsig:X509SerialNumber>\n" +
            "\t\t\t\t\t\t</dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t</dsig:X509Data>\n" +
            "\t\t\t\t</dsig:KeyInfo>\n" +
            "\t\t\t\t<xenc:CipherData>\n" +
            "\t\t\t\t\t<xenc:CipherValue>In+E+fH8yet5hEkxSLuF6c9XN1eI1E8xT/WLw67MkrCuwky9eB+bECWOt911CwzLUzwxDSEOpEv4RMlstZWBHwMxrEYFMJmmbtYNLqXd3DK067jZETX1MT7mWr+E8kXBBCThxeEEAzT6Is120A94E2yecKI2BjEdLDflT4K7Xb4=</xenc:CipherValue>\n" +
            "\t\t\t\t</xenc:CipherData>\n" +
            "\t\t\t</xenc:EncryptedKey>\n" +
            "\t\t</dsig:KeyInfo>\n" +
            "\t\t<xenc:CipherData>\n" +
            "\t\t\t<xenc:CipherValue>hTmUhapmXukxslLlOQMYARey3v9Pj1mmRGkjDKqKYwAFBaWJfS9usMnV7ClTSmy6JAmMa2ymkc3Pxnq98g7cwQ==</xenc:CipherValue>\n" +
            "\t\t</xenc:CipherData>\n" +
            "\t</xenc:EncryptedData>\n" +
            "\t<par:notice_id>12345</par:notice_id>\n" +
            "</par:GetNoaParties>";

    // Test message encrypted for an earlier instance of the CN=data.l7tech.com key (different cert serial number)
    public static final String TEST_ENCRYPTED_FOR_SOMEONE_ELSE =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "\t<par:username>brian</par:username>\n" +
            "\t<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "\t\t\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t\t\t<dsig:X509Data>\n" +
            "\t\t\t\t\t\t<dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t\t\t<dsig:X509IssuerName>CN=data.l7tech.com</dsig:X509IssuerName>\n" +
            "\t\t\t\t\t\t\t<dsig:X509SerialNumber>480673818902921845</dsig:X509SerialNumber>\n" +
            "\t\t\t\t\t\t</dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t</dsig:X509Data>\n" +
            "\t\t\t\t</dsig:KeyInfo>\n" +
            "\t\t\t\t<xenc:CipherData>\n" +
            "\t\t\t\t\t<xenc:CipherValue>GWCLzipub89lXg6e9SBu5xHfUyD3Wm8i2muo5muHBlhk07FDzlhJBASoX/LpNto9mjcOUJezXrRat9LhUTE9GHsFFn4FQl66o5fvqigOvj7h+IsUPuNXC0xo0zVnQANTb99t/AyNx7fZAahPbhwne0U/BLwex8MuKzLWbLmfabA=</xenc:CipherValue>\n" +
            "\t\t\t\t</xenc:CipherData>\n" +
            "\t\t\t</xenc:EncryptedKey>\n" +
            "\t\t</dsig:KeyInfo>\n" +
            "\t\t<xenc:CipherData>\n" +
            "\t\t\t<xenc:CipherValue>NqbcmF3u4Vj34h7J9Y7uk5gVvqguLHIHeFAzKfab+o4AhBHXXzSK9eO8xIfQiPBgXJnoJpTZvVnFE0Z3AHh+yQ==</xenc:CipherValue>\n" +
            "\t\t</xenc:CipherData>\n" +
            "\t</xenc:EncryptedData>\n" +
            "\t<par:notice_id>12345</par:notice_id>\n" +
            "</par:GetNoaParties>";

}

