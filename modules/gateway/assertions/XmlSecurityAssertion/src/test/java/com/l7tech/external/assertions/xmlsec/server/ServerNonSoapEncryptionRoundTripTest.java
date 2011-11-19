package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ServerNonSoapEncryptionRoundTripTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapEncryptionRoundTripTest.class.getName());

    private static SecurityTokenResolver securityTokenResolver;
    private static BeanFactory beanFactory;
    private static X509Certificate recipCert;
    private static String recipb64;

    @BeforeClass
    public static void setupKeys() throws Exception {
        Pair<X509Certificate,PrivateKey> ks = TestCertificateGenerator.convertFromBase64Pkcs12(NonSoapXmlSecurityTestUtils.TEST_KEYSTORE);
        recipCert = ks.left;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());
        securityTokenResolver = new SimpleSecurityTokenResolver(recipCert, ks.right);
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", securityTokenResolver);
        }});
    }
    
    @Test
    public void testEncryptionRoundTrip() throws Exception {
        doRoundTripTest(false, false, null);
    }

    @Test
    @BugNumber(7805)
    public void testEncryptionRoundTripWithRecipientCertVariable() throws Exception {
        doRoundTripTest(true, false, null);
    }

    @Test
    @BugNumber(7805)
    public void testEncryptionRoundTripWithRecipientCertStringVariable() throws Exception {
        doRoundTripTest(true, true, null);
    }

    @Test
    @BugNumber(11320)
    public void testAes128GcmEncryptionRoundTrip() throws Exception {
        doRoundTripTest(false, false, XencUtil.AES_128_GCM);
    }

    @Test
    @BugNumber(11320)
    public void testAes256GcmEncryptionRoundTrip() throws Exception {
        doRoundTripTest(false, false, XencUtil.AES_256_GCM);
    }

    private void doRoundTripTest(boolean useCertVar, boolean encodeAsString, @Nullable String algUri) throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException, SAXException, NoSuchAlgorithmException, NoSuchVariableException {
        String encryptedXml = makeEncryptedMessage(useCertVar, encodeAsString, algUri);

        logger.info("Encrypted XML:\n" + encryptedXml);

        Message request = new Message(XmlUtil.stringAsDocument(encryptedXml));
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        final String docString = XmlUtil.nodeToString(doc);
        logger.info("Decrypted XML:\n" + docString);
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertEquals("password", ((Element[])context.getVariable("elementsDecrypted"))[0].getLocalName());
        if (algUri != null)
            assertEquals(algUri, ((String[]) context.getVariable("encryptionMethodUris"))[0]);
    }

    private String makeEncryptedMessage(boolean useCertVar, boolean encodeCertAsString, @Nullable String algUri) throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException, SAXException, NoSuchAlgorithmException, NoSuchVariableException {
        Message req = ServerNonSoapEncryptElementAssertionTest.makeReq();
        NonSoapEncryptElementAssertion ass = ServerNonSoapEncryptElementAssertionTest.makeAss();
        if (algUri != null) ass.setXencAlgorithm(algUri);
        ass.setRecipientCertificateBase64(recipb64);
        if (useCertVar) {
            ass.setRecipientCertContextVariableName(encodeCertAsString ? "recipCertString" : "recipCert[3]");
        }
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("recipCert", new X509Certificate[] { null, null, null, recipCert, null, null });
        context.setVariable("recipCertString", recipb64);
        AssertionStatus encryptResult = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, encryptResult);
        return XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
    }

}
