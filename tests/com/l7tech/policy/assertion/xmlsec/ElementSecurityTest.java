package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.Keys;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.server.SessionManager;
import com.ibm.xml.dsig.KeyInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;

/**
 * Test the element security based assertions.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ElementSecurityTest extends TestCase {
    private Keys testKeys;
    private SignerInfo signerInfo;

    public ElementSecurityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ElementSecurityTest.class);
    }

    protected void setUp() throws Exception {
        testKeys = new Keys(1024);
        String subject = "CN=fred";
        signerInfo = testKeys.asSignerInfo(subject);
    }

    protected void tearDown() throws Exception {
        //
    }


    public void testSignerSignsBody() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);
        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, false, ElementSecurity.DEFAULT_CIPHER, ElementSecurity.DEFAULT_KEYBITS)
        };
        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);

        Document[] documents = soapMessagesAsDocuments(requests);

        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, session.getId(), data);
            Document secureDoc = signer.process(document).getDocument();
// System.out.println(XmlUtil.nodeToString(secureDoc));
            Document verifiedDoc = verifier.processInPlace(secureDoc).getDocument();
// System.out.println(XmlUtil.nodeToString(verifiedDoc));
        }
    }

    public void testSignerSignsAndEncryptsBody() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);


        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, session.getId(), data);
            Document secureDoc = signer.process(document).getDocument();
//System.out.println(XmlUtil.nodeToString(secureDoc));
            Document verifiedDoc = verifier.processInPlace(secureDoc).getDocument();
// System.out.println(XmlUtil.nodeToString(verifiedDoc));
        }
    }

    public void testKeyNotSpecifiedAndEncryptRequested() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);
        Document document = documents[0];
        try {
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, null, data);
            signer.process(document);
            fail("KeyException expected");
        } catch (KeyException e) {
            // expected
        }
        SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
        Document securedDocument = signer.process(document).getDocument();
        {
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, null, session.getId(), data);
            SecurityProcessor.Result result = verifier.process(securedDocument);
            assertTrue(result.getType() == SecurityProcessor.Result.Type.ERROR);
        }

        SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, session.getId(), data);
        Document verifiedDocument = verifier.process(securedDocument).getDocument();
        // System.out.println(XmlUtil.nodeToString(verifiedDocument));
    }

    public void testVerifyUnsecureDocument() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);
        Document document = documents[0];
        SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, session.getId(), data);
        SecurityProcessor.Result result = verifier.process(document);
        assertTrue(result.getType() == SecurityProcessor.Result.Type.POLICY_VIOLATION);
    }

    public void testUndecorateSignedAndEncryptedEnvelopeRequest() throws Exception {
        Document decorated = XmlUtil.stringToDocument(LIST_PRODUCTS_ENV_SIGNED_ENCRYPTED);

        System.out.println("Starting document: " + XmlUtil.nodeToString(decorated));
        Map nm = new HashMap();
        nm.put("soap", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        nm.put("s0", "http://warehouse.acme.com/ws");
        ElementSecurity elementSecurity = new ElementSecurity(new XpathExpression("/soap:Envelope", nm),
                                                              new XpathExpression("/soap:Envelope/soap:Body/s0:listProducts", nm),
                                                              true,
                                                              ElementSecurity.DEFAULT_CIPHER,
                                                              ElementSecurity.DEFAULT_KEYBITS);
        ElementSecurity[] elements = new ElementSecurity[] { elementSecurity };
        Session session = new Session(getSessionId(), System.currentTimeMillis(), getKeyReq(), getKeyRes(), 1);



        SecurityProcessor verifier = SecurityProcessor.getVerifier(session, new AesKey(getKeyReq(), 128),
                                                                   session.getId(), elements);

        SecurityProcessor.Result result = verifier.processInPlace(decorated);
        System.out.println("Undecorated status = " + result.getType().desc);
        System.out.println("Undecorated document: " + XmlUtil.nodeToString(decorated));

    }

    private long getSessionId() {
        return 7969434868552148141L;
    }

    private byte[] getKeyRes() throws IOException {
        return HexUtils.unHexDump("b7bdcf4bebf54884e7f481db15a6833d1018512a9c9a6f8b1654c3d3335866ca");
    }

    private byte[] getKeyReq() throws IOException {
        return HexUtils.unHexDump("7d4078f2202a90bdcf9b12befd25a446a8a2889253046d7aa9b728d152e6b8ea");
    }

    private Document[] soapMessagesAsDocuments(SoapMessageGenerator.Message[] requests)
      throws IOException, SOAPException, SAXException {
        Document[] documents = new Document[requests.length];
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            request.getSOAPMessage().writeTo(bos);
            documents[i] = XmlUtil.stringToDocument(bos.toString());
        }
        return documents;

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    private RSAPrivateKey clientCertPrivateKey = null;
    private RSAPrivateKey getClientCertPrivateKey() throws Exception {
        if (clientCertPrivateKey != null)
            return clientCertPrivateKey;
        final RSAPublicKey pubkey = getClientCertPublicKey();
        final BigInteger exp = getClientPrivateExponent();
        RSAPrivateKey privkey = new RSAPrivateKey() {
            public BigInteger getPrivateExponent() {
                return exp;
            }

            public byte[] getEncoded() {
                throw new UnsupportedOperationException();
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public String getFormat() {
                return "RAW";
            }

            public BigInteger getModulus() {
                return pubkey.getModulus();
            }
        };

        return clientCertPrivateKey = privkey;
    }

    private RSAPublicKey clientCertPublicKey = null;
    private RSAPublicKey getClientCertPublicKey() throws Exception {
        if (clientCertPublicKey != null) return clientCertPublicKey;
        return clientCertPublicKey = (RSAPublicKey)getClientCertificate().getPublicKey();
    }

    private BigInteger clientPrivateExponent = null;
    private BigInteger getClientPrivateExponent() throws Exception {
        if (clientPrivateExponent != null)
            return clientPrivateExponent;
        return clientPrivateExponent = new BigInteger("14210184001346182823171881835738381277827996577932081129870087126985936833978265455351921190436286000573299829913011073323996861977305812063483272040066774270348635117888804481969527730014308896683925327444582612170651505678366458811263904624399609896941064558149974398113006389383364750139120015434622210425");
    }

    private X509Certificate clientCertificate = null;
    private X509Certificate getClientCertificate() throws Exception {
        if (clientCertificate != null)
            return clientCertificate;
        Document d = XmlUtil.stringToDocument(KEY_INFO);

        // Find KeyInfo bodyElement, and extract certificate from this
        Element keyInfoElement = d.getDocumentElement();

        if (keyInfoElement == null) {
            throw new SignatureNotFoundException("KeyInfo bodyElement not found");
        }

        KeyInfo keyInfo = new KeyInfo(keyInfoElement);

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();

        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();

        X509Certificate cert = certs[0];

        return clientCertificate = cert;
    }


    final String LIST_PRODUCTS_ENV_SIGNED_ENCRYPTED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope Id=\"signref1\"\n" +
            "    xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soap:Header>\n" +
            "        <wsse:Security xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/xx/secext\">\n" +
            "            <wsse:SecurityContextToken>\n" +
            "                <wsu:Identifier xmlns:wsu=\"http://schemas.xmlsoap.org/ws/2002/07/utility\">7969434868552148141</wsu:Identifier>\n" +
            "                <l7:SeqNr xmlns:l7=\"http://l7tech.com/ns/msgseqnr\">1</l7:SeqNr>\n" +
            "            </wsse:SecurityContextToken>\n" +
            "            <xenc:ReferenceList>\n" +
            "                <xenc:DataReference URI=\"encref1\"/>\n" +
            "            </xenc:ReferenceList>\n" +
            "            <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                <ds:SignedInfo>\n" +
            "                    <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>\n" +
            "                    <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
            "                    <ds:Reference URI=\"#signref1\">\n" +
            "                        <ds:Transforms>\n" +
            "                            <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
            "                            <ds:Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>\n" +
            "                        </ds:Transforms>\n" +
            "                        <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
            "                        <ds:DigestValue>3jI+1MzTXZ9qfsUMFvlGxKbQYFI=</ds:DigestValue>\n" +
            "                    </ds:Reference>\n" +
            "                </ds:SignedInfo>\n" +
            "                <ds:SignatureValue>ZkdId9mWHa4Do0qhHOcqTxaBqWBoQtepwKH0eTql5oI8KQsIcuotgyAP/aXApSh0TM0I3welpQ62Eqo8U4Cv2SbV6riEjp2Q36ul9JcOwLeQu0RDj5FX7JSfmn57bx71CXi80fbZGXKuso9tQSHNrj+LOFINB0b6+HGjenaAaLs=</ds:SignatureValue>\n" +
            KEY_INFO +
            "            </ds:Signature>\n" +
            "        </wsse:Security>\n" +
            "    </soap:Header>\n" +
            "    <soap:Body>\n" +
            "        <EncryptedData Id=\"encref1\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "            <EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/>\n" +
            "            <KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                <KeyName>7969434868552148141</KeyName>\n" +
            "            </KeyInfo>\n" +
            "            <CipherData>\n" +
            "                <CipherValue>VSvvrqyLtsWU3BBLUshbTOITPZEgBJkj10IcQ7366HNptuLg7pBZY9ftGIP4dK/p+UWshoiKUXW5I1t/LpaZ++o79RF9haYQWiX1Vuvfjsw=</CipherValue>\n" +
            "            </CipherData>\n" +
            "        </EncryptedData>\n" +
            "    </soap:Body>\n" +
            "</soap:Envelope>";

    private static final String KEY_INFO =
            "                <ds:KeyInfo>\n" +
            "                    <ds:X509Data>\n" +
            "                        <ds:X509IssuerSerial>\n" +
            "                            <ds:X509IssuerName>CN=root.bones.l7tech.com</ds:X509IssuerName>\n" +
            "                            <ds:X509SerialNumber>904794165846900942</ds:X509SerialNumber>\n" +
            "                        </ds:X509IssuerSerial>\n" +
            "                        <ds:X509SKI>coFBySN+O+hPYLGEGcXu4tmnAas=</ds:X509SKI>\n" +
            "                        <ds:X509SubjectName>CN=mike</ds:X509SubjectName>\n" +
            "\n" +
            "                        <ds:X509Certificate>MIICDjCCAXegAwIBAgIIDI55gAImgM4wDQYJKoZIhvcNAQEFBQAwIDEeMBwGA1UEAxMVcm9vdC5i\n" +
            "                            b25lcy5sN3RlY2guY29tMB4XDTA0MDQyNzAzMDgxNFoXDTA2MDQyNzAzMTgxNFowDzENMAsGA1UE\n" +
            "                            AxMEbWlrZTCBnTANBgkqhkiG9w0BAQEFAAOBiwAwgYcCgYEArAF6lF/xFyPyaNkdQ38srbZ5fY/S\n" +
            "                            dLSuHb7R62aBiA15Mlshh7/AdRszW55b4AGKpiAer9Qx4pq9HQ+dGGQICbX79zzYkwEGAivjQA7H\n" +
            "                            1QEP8hdHsh5ipWJG2q7WIWlcHdowiBHxyG+/hpEKXQBhErRoJkEj2CIXBo6v/k/lBMkCARGjZDBi\n" +
            "                            MA8GA1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUcoFBySN+O+hPYLGE\n" +
            "                            GcXu4tmnAaswHwYDVR0jBBgwFoAUYTEm6UF1LGrdWuLzjWvkwqYElZ0wDQYJKoZIhvcNAQEFBQAD\n" +
            "                            gYEAOckoy2Ev53OyAS/HCVS1IDQlnPMd7YbBfVwyHI2yAJh4NjCV+xkVyYyBwJeNiDTiDyiRaW//\n" +
            "                            gjSt4eKyCK2s3BMbAq5g6Z9YO2Y2hvwOm+Y5fpcaplKazzSXCl/sH/pYvecjKZOoNa2RMeOdXAHe Dpj4AFACBAaW+JolAo7Wdbc=</ds:X509Certificate>\n" +
            "                    </ds:X509Data>\n" +
            "                </ds:KeyInfo>\n";
}



