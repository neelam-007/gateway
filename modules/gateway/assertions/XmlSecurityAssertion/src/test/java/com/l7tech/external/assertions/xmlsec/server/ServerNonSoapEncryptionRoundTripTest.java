package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.Pair;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapEncryptionRoundTripTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapEncryptionRoundTripTest.class.getName());

    private static SecurityTokenResolver securityTokenResolver;
    private static BeanFactory beanFactory;
    private static String recipb64;

    @BeforeClass
    public static void setupKeys() throws Exception {
        Pair<X509Certificate,PrivateKey> ks = TestCertificateGenerator.convertFromBase64Pkcs12(NonSoapXmlSecurityTestUtils.TEST_KEYSTORE);
        recipb64 = HexUtils.encodeBase64(ks.left.getEncoded());
        securityTokenResolver = new SimpleSecurityTokenResolver(ks.left, ks.right);
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", securityTokenResolver);
        }});
    }
    
    @Test
    public void testEncryptionRoundTrip() throws Exception {
        String encryptedXml = makeEncryptedMessage();

        logger.info("Encrypted XML:\n" + encryptedXml);

        Message request = new Message(XmlUtil.stringAsDocument(encryptedXml));
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory, null).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());

    }

    private String makeEncryptedMessage() throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException, SAXException {
        Message req = ServerNonSoapEncryptElementAssertionTest.makeReq();
        NonSoapEncryptElementAssertion ass = ServerNonSoapEncryptElementAssertionTest.makeAss();
        ass.setRecipientCertificateBase64(recipb64);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass, null);
        AssertionStatus encryptResult = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message()) );
        assertEquals(AssertionStatus.NONE, encryptResult);
        return XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
    }

}
