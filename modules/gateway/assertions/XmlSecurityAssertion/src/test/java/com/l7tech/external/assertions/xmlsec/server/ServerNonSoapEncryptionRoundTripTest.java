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

import static com.l7tech.external.assertions.xmlsec.server.ServerNonSoapEncryptElementAssertionTest.TEST_XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    @BugNumber(11191)
    public void testEmptyElementContentsOnly() throws Exception {
        doRoundTripTest(false, false, true, true, null);
    }

    @Test
    @BugNumber(12600)
    public void testContentsOnly() throws Exception {
        doRoundTripTest(false, false, false, true, null);
    }

    private void doRoundTripTest(boolean useCertVar, boolean encodeAsString, @Nullable String algUri) throws Exception {
        doRoundTripTest(useCertVar, encodeAsString, false, false, algUri);
    }

    private void doRoundTripTest(boolean useCertVar, boolean encodeAsString, boolean useEmptyPasswordElement, boolean contentsOnly, @Nullable String algUri) throws Exception {
        String encryptedXml = makeEncryptedMessage(useCertVar, encodeAsString, useEmptyPasswordElement, contentsOnly, algUri);

        logger.info("Encrypted XML:\n" + encryptedXml);

        Message request = new Message(XmlUtil.stringAsDocument(encryptedXml));
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setReportContentsOnly(contentsOnly);
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        final String docString = XmlUtil.nodeToString(doc);
        logger.info("Decrypted XML:\n" + docString);

        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        final Element[] elementsDecrypteds = (Element[]) context.getVariable("elementsDecrypted");
        assertEquals(1, elementsDecrypteds.length);
        final String[] encryptionMethodUris = (String[]) context.getVariable("encryptionMethodUris");
        assertEquals(elementsDecrypteds.length, encryptionMethodUris.length);
        assertEquals("password", elementsDecrypteds[0].getLocalName());
        if (algUri != null) {
            assertEquals(algUri, encryptionMethodUris[0]);
        }
        if (contentsOnly) {
            final Boolean[] contentsOnlies = (Boolean[]) context.getVariable("contentsOnly");
            assertEquals(elementsDecrypteds.length, contentsOnlies.length);
            assertTrue(contentsOnlies[0]);
        } else {
            assertNoSuchVariable(context, "contentsOnly");
        }
    }

    private String makeEncryptedMessage(boolean useCertVar, boolean encodeCertAsString, boolean useEmptyPasswordElement, boolean contentsOnly, @Nullable String algUri) throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException, SAXException, NoSuchAlgorithmException, NoSuchVariableException {
        final String reqXml = useEmptyPasswordElement ? TEST_XML.replace("<par:password>somepassword</par:password>", "<par:password/>") : TEST_XML;
        Message req = ServerNonSoapEncryptElementAssertionTest.makeReq(reqXml);
        NonSoapEncryptElementAssertion ass = ServerNonSoapEncryptElementAssertionTest.makeAss();
        if (algUri != null) ass.setXencAlgorithm(algUri);
        ass.setRecipientCertificateBase64(recipb64);
        if (useCertVar) {
            ass.setRecipientCertContextVariableName(encodeCertAsString ? "recipCertString" : "recipCert[3]");
        }
        if (contentsOnly)
            ass.setEncryptContentsOnly(true);
        ServerNonSoapEncryptElementAssertion sass = new ServerNonSoapEncryptElementAssertion(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        context.setVariable("recipCert", new X509Certificate[] { null, null, null, recipCert, null, null });
        context.setVariable("recipCertString", recipb64);
        AssertionStatus encryptResult = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, encryptResult);
        return XmlUtil.nodeToString(req.getXmlKnob().getDocumentReadOnly());
    }

    private static void assertNoSuchVariable(PolicyEnforcementContext context, String variable) {
        try {
            Object val = context.getVariable(variable);
            fail("Variable " + variable + " was not expected to exist at this time: existed with value=" + val);
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

}
