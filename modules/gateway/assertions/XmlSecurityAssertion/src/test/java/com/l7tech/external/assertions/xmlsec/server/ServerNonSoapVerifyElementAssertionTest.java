package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;

/**
 *
 */
public class ServerNonSoapVerifyElementAssertionTest {
    static final String SIGNER_CERT =
            "MIIB8TCCAVqgAwIBAgIIJiwd8ruk6HcwDQYJKoZIhvcNAQEMBQAwGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMB4XDTA5MDYyNjIyMjMzOVoXDTE5MDYyNDIyMjMzOVowGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCTEO6rGMn+X+lVNIEprLUi9a2e6VVBg1Ozr91TPaOhK8JeWDNtkXUych0PFN6YpBDEsiLSb8aiej5CwZFt/mmWdRn2qAKfutJ8F52SgW5ZLuYTtD4MFcOMySfC6aLk726VUUIipYKngNPOLHxUqnMapTWT4x2Ssi9+23TN6QH63QIBA6NCMEAwHQYDVR0OBBYEFEJIF1caGbInfcje2ODXnxszns+yMB8GA1UdIwQYMBaAFEJIF1caGbInfcje2ODXnxszns+yMA0GCSqGSIb3DQEBDAUAA4GBAFU/MTZm3TZACawEgBUKSJd04FvnLV99lGIBLp91rHcbCAL9roZJp7RC/w7sHVUve8khtYm5ynaZVTu7S++NTqnCWQI1Bx021zFhAeucFsx3tynfAdjW/xRre8pQLF9A7PoJTYcaS2tNQgS7um3ZHxjA/JV3dQWzeR1Kwepvzmk9";

    static final String SIGNATURE_VALUE =
            "G+S/sK4hg3rPxAsjXm8jS4twP54ltDJQf/EK7elQCNi2Vd2wDrGQ0vkcIWm+fdfh4nYMA0Th8m/cQbDaicp/0Z990S4oNUllNL4cLPlkcIEPWG9r9siZg5346hdi/W0xHsO199ukHm51I6v4qujK5tqajP0AbJl1fe8ly8CnAos=";

    static final String SIGNED =
            "<foo><bar Id=\"bar-1-3511c4c29ab6a196290a5f79a61417a6\"><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">" +
            "<ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#bar-1-3511c4c29ab6a196290a5f79a61417a6\">" +
            "<ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>" +
            "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>" +
            "<ds:DigestValue>VU0equBu1QkCdTyzf6Dx6dulVxM=</ds:DigestValue></ds:Reference></ds:SignedInfo>" +
            "<ds:SignatureValue>" + SIGNATURE_VALUE + "</ds:SignatureValue>" +
            "<ds:KeyInfo><ds:X509Data><ds:X509Certificate>" + SIGNER_CERT + "</ds:X509Certificate>" +
            "</ds:X509Data></ds:KeyInfo></ds:Signature></bar><blat/></foo>";

    // Unfortunately this includes an EC cert that uses explicit parameters rather than a named curve, so
    // requires the Bouncy Castle certificate factory to parse it.
    public static final String APACHE_SAMPLE_SIGNED_XML =
            "<!-- Comment before -->\n" +
            "<RootElement>Some simple text\n" +
            "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "<ds:SignedInfo>\n" +
            "\n" +
            "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod>\n" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1\"></ds:SignatureMethod>\n" +
            "<ds:Reference URI=\"\">\n" +
            "<ds:Transforms>\n" +
            "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform>\n" +
            "<ds:Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\"></ds:Transform>\n" +
            "</ds:Transforms>\n" +
            "<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod>\n" +
            "<ds:DigestValue>LKyUpNaZJ2joznVzwEup5JDwtS0=</ds:DigestValue>\n" +
            "</ds:Reference>\n" +
            "</ds:SignedInfo>\n" +
            "<ds:SignatureValue>\n" +
            "Qma5I5AZiSzQ6J4UZwjpteD2qvQclABKATPQ5MZ7mmOFYfj8xAlpXWu2u+Oa/4mpP9jK9OUUcTU9\n" +
            "Psfucz+qPA==\n" +
            "</ds:SignatureValue>\n" +
            "<ds:KeyInfo>\n" +
            "<ds:X509Data>\n" +
            "<ds:X509Certificate>\n" +
            "MIICEjCCAbegAwIBAgIGARJQ/UmbMAsGByqGSM49BAEFADBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwHhcNMDcwNTAzMDgxMDE1WhcNMTEwNTAzMDgxMDE1WjBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwgbQwgY0GByqGSM49AgEwgYECAQEwLAYHKoZIzj0BAQIhAP//////////////////////////\n" +
            "//////////////2XMCcEIQD////////////////////////////////////////9lAQCAKYEAgMB\n" +
            "AiEA/////////////////////2xhEHCZWtEARYQbCbdhuJMDIgADZubz40WiQ+v/nrjhfizYmEIl\n" +
            "tKIr/n7hwGwpG3CDEk2jIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgGmMAsGByqGSM49\n" +
            "BAEFAANIADBFAiEA63Pq7/YfDDrnbCxXVX20T3dn77iL8dvC1Cb24Al9VFkCIHUeymf/N+H60OQL\n" +
            "v9Wg/X8Cbp2am42qjQvaKtb4+BFk\n" +
            "</ds:X509Certificate>\n" +
            "</ds:X509Data>\n" +
            "</ds:KeyInfo>\n" +
            "</ds:Signature></RootElement>\n" +
            "<!-- Comment after -->";

    public static final String APACHE_SIGNER_CERT =
            "MIICEjCCAbegAwIBAgIGARJQ/UmbMAsGByqGSM49BAEFADBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwHhcNMDcwNTAzMDgxMDE1WhcNMTEwNTAzMDgxMDE1WjBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwgbQwgY0GByqGSM49AgEwgYECAQEwLAYHKoZIzj0BAQIhAP//////////////////////////\n" +
            "//////////////2XMCcEIQD////////////////////////////////////////9lAQCAKYEAgMB\n" +
            "AiEA/////////////////////2xhEHCZWtEARYQbCbdhuJMDIgADZubz40WiQ+v/nrjhfizYmEIl\n" +
            "tKIr/n7hwGwpG3CDEk2jIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgGmMAsGByqGSM49\n" +
            "BAEFAANIADBFAiEA63Pq7/YfDDrnbCxXVX20T3dn77iL8dvC1Cb24Al9VFkCIHUeymf/N+H60OQL\n" +
            "v9Wg/X8Cbp2am42qjQvaKtb4+BFk";

    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
        }});
    }

    @Test
    public void testVerify() throws Exception {
        NonSoapVerifyElementAssertion ass = new NonSoapVerifyElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name()='Signature']"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapVerifyElementAssertion sass = new ServerNonSoapVerifyElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument(SIGNED));
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
        assertTrue(CertUtils.certsAreEqual((X509Certificate) signingCertificates[0], CertUtils.decodeFromPEM(SIGNER_CERT, false)));

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
        assertEquals(SIGNATURE_VALUE, signatureValues[0]);

        Object[] signatureElements = (Object[])context.getVariable("signatureElements");
        assertNotNull(signatureElements);
        assertEquals(1, signatureElements.length);
        assertTrue(Element.class.isInstance(signatureElements[0]));
        Element sigElement = (Element) signatureElements[0];
        assertEquals("Signature", sigElement.getLocalName());
        assertEquals(SoapConstants.DIGSIG_URI, sigElement.getNamespaceURI());

    }
    
    @Test
    public void testVerifyApacheSignedEcdsa() throws Exception {
        ServerNonSoapVerifyElementAssertion.CERT_PARSE_BC_FALLBACK = true;

        // This test unfortunately needs to use the Bouncy Castle CertificateFactory, since the signing cert does not use a named curve
        JceProvider.init();
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        NonSoapVerifyElementAssertion ass = new NonSoapVerifyElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name()='Signature']"));
        ass.setTarget(TargetMessageType.REQUEST);

        ServerNonSoapVerifyElementAssertion sass = new ServerNonSoapVerifyElementAssertion(ass, beanFactory, null);
        Message request = new Message(XmlUtil.stringAsDocument(APACHE_SAMPLE_SIGNED_XML));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        Document doc = request.getXmlKnob().getDocumentReadOnly();
        Element bar = (Element)doc.getElementsByTagName("RootElement").item(0);

        Object[] elementsVerified = (Object[])context.getVariable("elementsVerified");
        assertNotNull(elementsVerified);
        assertEquals(1, elementsVerified.length);
        assertTrue(elementsVerified[0] == bar);

        Object[] signingCertificates = (Object[])context.getVariable("signingCertificates");
        assertNotNull(signingCertificates);
        assertEquals(1, signingCertificates.length);
        //assertTrue(CertUtils.certsAreEqual((X509Certificate) signingCertificates[0], CertUtils.decodeFromPEM(APACHE_SIGNER_CERT, false)));

        Object[] digestMethodUris = (Object[])context.getVariable("digestMethodUris");
        assertNotNull(digestMethodUris);
        assertEquals(1, digestMethodUris.length);
        assertEquals("http://www.w3.org/2000/09/xmldsig#sha1", digestMethodUris[0]);

        Object[] signatureMethodUris = (Object[])context.getVariable("signatureMethodUris");
        assertNotNull(signatureMethodUris);
        assertEquals(1, signatureMethodUris.length);
        assertEquals("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", signatureMethodUris[0]);

        Object[] signatureValues = (Object[])context.getVariable("signatureValues");
        assertNotNull(signatureValues);
        assertEquals(1, signatureValues.length);

        Object[] signatureElements = (Object[])context.getVariable("signatureElements");
        assertNotNull(signatureElements);
        assertEquals(1, signatureElements.length);
        assertTrue(Element.class.isInstance(signatureElements[0]));
        Element sigElement = (Element) signatureElements[0];
        assertEquals("Signature", sigElement.getLocalName());
        assertEquals(SoapConstants.DIGSIG_URI, sigElement.getNamespaceURI());

    }
}
