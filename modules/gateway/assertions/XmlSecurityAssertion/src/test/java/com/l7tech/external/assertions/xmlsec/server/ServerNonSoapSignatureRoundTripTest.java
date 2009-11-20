package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapSignatureRoundTripTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignatureRoundTripTest.class.getName());


    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        JceProvider.init();
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
        }});
    }

    @Test
    public void testSignatureRoundTrip() throws Exception {
        Document doc = createAndSign("data");

        verifySuccess(NonSoapXmlSecurityTestUtils.getDataKey().getCertificate(),
                XmlUtil.nodeToString(doc),
                "http://www.w3.org/2000/09/xmldsig#sha1",
                "http://www.w3.org/2000/09/xmldsig#rsa-sha1");
    }

    @Test
    public void testSignatureRoundTripEcdsa() throws Exception {
        Document doc = createAndSign(NonSoapXmlSecurityTestUtils.ECDSA_KEY_ALIAS);

        verifySuccess(NonSoapXmlSecurityTestUtils.getEcdsaKey().getCertificate(),
                XmlUtil.nodeToString(doc),
                "http://www.w3.org/2001/04/xmldsig-more#sha384",
                "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384");
    }

    @Test
    public void testBadDigest() throws Exception {
        Document doc = createAndSign(NonSoapXmlSecurityTestUtils.TEST_KEY_ALIAS);
        doc.getElementsByTagName("bar").item(0).appendChild(doc.createTextNode("Extra added text!"));
        verify(XmlUtil.nodeToString(doc),
                AssertionStatus.BAD_REQUEST);
    }

    @Test
    public void testBadDigestEcdsa() throws Exception {
        Document doc = createAndSign(NonSoapXmlSecurityTestUtils.ECDSA_KEY_ALIAS);
        ((Element)(doc.getElementsByTagName("bar").item(0))).setAttribute("newattr", "added an attr");
        verify(XmlUtil.nodeToString(doc),
                AssertionStatus.BAD_REQUEST);
    }

    @Test
    public void testBadSignatureValue() throws Exception {
        Document doc = createAndSign(NonSoapXmlSecurityTestUtils.DATA_KEY_ALIAS);
        Element sve = (Element)doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue").item(0);
        byte[] svebytes = HexUtils.decodeBase64(sve.getTextContent());
        svebytes[13]++;
        sve.setTextContent(HexUtils.encodeBase64(svebytes));
        verify(XmlUtil.nodeToString(doc),
                AssertionStatus.BAD_REQUEST);
    }

    @Test
    public void testBadSignatureValueEcdsa() throws Exception {
        Document doc = createAndSign(NonSoapXmlSecurityTestUtils.ECDSA_KEY_ALIAS);
        Element sve = (Element)doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue").item(0);
        byte[] svebytes = HexUtils.decodeBase64(sve.getTextContent());
        svebytes[13]++;
        sve.setTextContent(HexUtils.encodeBase64(svebytes));
        verify(XmlUtil.nodeToString(doc),
                AssertionStatus.BAD_REQUEST);
    }

    void verifySuccess(X509Certificate signerCert, String signedXml, String expectedDigestMethodUri, String expectedSignatureMethodUri) throws Exception {
        final AssertionStatus expectedAssertionResult = AssertionStatus.NONE;
        PolicyEnforcementContext context = verify(signedXml, expectedAssertionResult);

        Document doc = context.getRequest().getXmlKnob().getDocumentReadOnly();
        Element bar = (Element)doc.getElementsByTagName("bar").item(0);

        Object[] elementsVerified = (Object[])context.getVariable("elementsVerified");
        assertNotNull(elementsVerified);
        assertEquals(1, elementsVerified.length);
        assertTrue(elementsVerified[0] == bar);

        Object[] signingCertificates = (Object[])context.getVariable("signingCertificates");
        assertNotNull(signingCertificates);
        assertEquals(1, signingCertificates.length);
        assertTrue(CertUtils.certsAreEqual((X509Certificate) signingCertificates[0], signerCert));

        Object[] digestMethodUris = (Object[])context.getVariable("digestMethodUris");
        assertNotNull(digestMethodUris);
        assertEquals(1, digestMethodUris.length);
        assertEquals(expectedDigestMethodUri, digestMethodUris[0]);

        Object[] signatureMethodUris = (Object[])context.getVariable("signatureMethodUris");
        assertNotNull(signatureMethodUris);
        assertEquals(1, signatureMethodUris.length);
        assertEquals(expectedSignatureMethodUri, signatureMethodUris[0]);

        Object[] signatureValues = (Object[])context.getVariable("signatureValues");
        assertNotNull(signatureValues);
        assertEquals(1, signatureValues.length);
    }

    PolicyEnforcementContext verify(String signedXml, AssertionStatus expectedAssertionResult) throws InvalidXpathException, IOException, PolicyAssertionException {
        NonSoapVerifyElementAssertion ass = new NonSoapVerifyElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name()='Signature']"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapVerifyElementAssertion sass = new ServerNonSoapVerifyElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument(signedXml));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(expectedAssertionResult, result);
        return context;
    }

    Document createAndSign(String keyAlias) throws InvalidXpathException, IOException, PolicyAssertionException, SAXException {
        NonSoapSignElementAssertion ass = new NonSoapSignElementAssertion();
        ass.setKeyAlias(keyAlias);
        ass.setUsesDefaultKeyStore(false);
        ass.setNonDefaultKeystoreId(-1);
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument("<foo><bar/></foo>"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Signed XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature").getLength());
        return doc;
    }
}
