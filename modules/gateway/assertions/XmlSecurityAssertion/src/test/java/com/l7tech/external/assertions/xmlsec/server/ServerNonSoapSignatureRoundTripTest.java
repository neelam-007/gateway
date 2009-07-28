package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;

/**
 *
 */
public class ServerNonSoapSignatureRoundTripTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignatureRoundTripTest.class.getName());


    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
        }});
    }

    @Test
    public void testSignatureRoundTrip() throws Exception {
        NonSoapSignElementAssertion ass = new NonSoapSignElementAssertion();
        ass.setKeyAlias("data");
        ass.setUsesDefaultKeyStore(false);
        ass.setNonDefaultKeystoreId(-1);
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument("<foo><bar/></foo>"));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Signed XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature").getLength());

        verify(NonSoapXmlSecurityTestUtils.getDataKey().getCertificate(), XmlUtil.nodeToString(doc));
    }

    public void verify(X509Certificate signerCert, String signedXml) throws Exception {
        NonSoapVerifyElementAssertion ass = new NonSoapVerifyElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name()='Signature']"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapVerifyElementAssertion sass = new ServerNonSoapVerifyElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument(signedXml));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        Document doc = request.getXmlKnob().getDocumentReadOnly();
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
        assertEquals("http://www.w3.org/2000/09/xmldsig#sha1", digestMethodUris[0]);

        Object[] signatureMethodUris = (Object[])context.getVariable("signatureMethodUris");
        assertNotNull(signatureMethodUris);
        assertEquals(1, signatureMethodUris.length);
        assertEquals("http://www.w3.org/2000/09/xmldsig#rsa-sha1", signatureMethodUris[0]);

        Object[] signatureValues = (Object[])context.getVariable("signatureValues");
        assertNotNull(signatureValues);
        assertEquals(1, signatureValues.length);
    }
}
