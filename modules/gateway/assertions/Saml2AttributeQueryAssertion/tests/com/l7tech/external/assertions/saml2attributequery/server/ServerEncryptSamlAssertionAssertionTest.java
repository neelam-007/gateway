package com.l7tech.external.assertions.saml2attributequery.server;

import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.bouncycastle.openssl.PEMReader;
import org.apache.commons.codec.binary.Base64;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.security.KeyPair;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.util.SoapUtil;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 3-Feb-2009
 * Time: 9:44:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerEncryptSamlAssertionAssertionTest {
    private static final String BASE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                           "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                           "  <soapenv:Header></soapenv:Header>\n" +
                                           "  <soapenv:Body>\n" +
                                           "    <samlp2:Response xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp2=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" ID=\"samlp2-5cb4e012f211b7fc935d002defae3562\" InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
                                           "      <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
                                           "      <samlp2:Status>\n" +
                                           "        <samlp2:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"></samlp2:StatusCode>\n" +
                                           "      </samlp2:Status>\n" +
                                           "      <saml2:Assertion ID=\"samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
                                           "        <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
                                           "        <saml2:Subject>\n" +
                                           "          <saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified\">jabba</saml2:NameID>\n" +
                                           "        </saml2:Subject>\n" +
                                           "        <saml2:Conditions>\n" +
                                           "          <saml2:AudienceRestriction>\n" +
                                           "            <saml2:Audience>http://layer7tech.com/</saml2:Audience>\n" +
                                           "          </saml2:AudienceRestriction>\n" +
                                           "        </saml2:Conditions>\n" +
                                           "        <saml2:AttributeStatement>\n" +
                                           "          <saml2:Attribute Name=\"urn:oid:givenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
                                           "            <saml2:AttributeValue xmlns:ns6=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns6:string\">Jabba</saml2:AttributeValue>\n" +
                                           "          </saml2:Attribute>\n" +
                                           "          <saml2:Attribute Name=\"urn:oid:cn\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
                                           "            <saml2:AttributeValue xmlns:ns6=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns6:string\">jabba</saml2:AttributeValue>\n" +
                                           "          </saml2:Attribute>\n" +
                                           "        </saml2:AttributeStatement>\n" +
                                           "      </saml2:Assertion>\n" +
                                           "    </samlp2:Response>\n" +
                                           "  </soapenv:Body>\n" +
                                           "</soapenv:Envelope>";

    private static final String CERTIFICATE_PEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDyTCCAzKgAwIBAgIGAR3QDW8IMA0GCSqGSIb3DQEBBQUAMIG3MQswCQYDVQQG\n" +
            "EwJDQTELMAkGA1UECAwCQkMxGzAZBgNVBAoMEkxheWVyIDcgVGVjaG5vbG9neTER\n" +
            "MA8GA1UECwwIVGFjdGljYWwxFjAUBgNVBAMMDU5vcm1hbiBKb3JkYW4xGjAYBgoJ\n" +
            "kiaJk/IsZAEZFgpsYXllcjd0ZWNoMTcwNQYKCZImiZPyLGQBGRYnY29tL2VtYWls\n" +
            "QWRkcmVzcz1uam9yZGFuQGxheWVyN3RlY2guY29tMB4XDTA3MTEyNTE5NTAyMFoX\n" +
            "DTA5MTEyNDE5NTAyMFowgbcxCzAJBgNVBAYTAkNBMQswCQYDVQQIDAJCQzEbMBkG\n" +
            "A1UECgwSTGF5ZXIgNyBUZWNobm9sb2d5MREwDwYDVQQLDAhUYWN0aWNhbDEWMBQG\n" +
            "A1UEAwwNTm9ybWFuIEpvcmRhbjEaMBgGCgmSJomT8ixkARkWCmxheWVyN3RlY2gx\n" +
            "NzA1BgoJkiaJk/IsZAEZFidjb20vZW1haWxBZGRyZXNzPW5qb3JkYW5AbGF5ZXI3\n" +
            "dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANed4rC098rPSShM\n" +
            "nq9Rf5a++Z5uu4FwcSY0S2tPpiNMSCmSQRjLZgwsriHolnaUWFUUU6gfZ3ZOBSlg\n" +
            "cZ7hf1sKZXMN/wwLLR/gqs5mw7fVC7Jl+8ufHPPJ/gnl0l8hqCWSiCu6ZXwJAG+K\n" +
            "4K1j+5L0N35H2isCe64u2ZRdDtRdAgMBAAGjgd0wgdowLQYDVR0JBCYwJDAQBggr\n" +
            "BgEFBQcJBDEEEwJDQTAQBggrBgEFBQcJBDEEEwJUVzAxBgNVHREEKjAogg5sYXll\n" +
            "cjd0ZWNoLmNvbYEWbmpvcmRhbkBsYXllcjd0ZWNoLmNvbTAxBgNVHRIEKjAogg5s\n" +
            "YXllcjd0ZWNoLmNvbYEWbmpvcmRhbkBsYXllcjd0ZWNoLmNvbTAJBgNVHRMEAjAA\n" +
            "MAsGA1UdDwQEAwIBBjAWBgNVHSAEDzANMAsGCWCGSAFlAgELBTATBgNVHSUEDDAK\n" +
            "BggrBgEFBQcDAzANBgkqhkiG9w0BAQUFAAOBgQCurgro6YQrFO8oT+YKnuJ7WOrX\n" +
            "8MRQWrnTNIWN/SNib/FNSdz0P8q/hohRQ/2F79m+xrXPBGieanYASN3MgBAN57/g\n" +
            "BBAA0bzHDYsjP31bO+IfqubLJ6W5RKbxOfsW+ceTTDcSm3DhT+LlfMVzN7VNOtuU\n" +
            "aa5AG6CdLDzMUo2avg==\n" +
            "-----END CERTIFICATE-----";
    private static final String PRIVATE_KEY_PEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIICXAIBAAKBgQDXneKwtPfKz0koTJ6vUX+WvvmebruBcHEmNEtrT6YjTEgpkkEY\n" +
            "y2YMLK4h6JZ2lFhVFFOoH2d2TgUpYHGe4X9bCmVzDf8MCy0f4KrOZsO31QuyZfvL\n" +
            "nxzzyf4J5dJfIaglkogrumV8CQBviuCtY/uS9Dd+R9orAnuuLtmUXQ7UXQIDAQAB\n" +
            "AoGARE2+102sxbGeskZ7anx916pN9zOK8LlHDtw4HBmSPtJWddzgBFPC0w6AZzuA\n" +
            "FrZtuR4EVlkEdITIu8/SjotOxVoLmY8Z6vd5wkBDvj1s8uoOJkiWN2KmH+k1z7/w\n" +
            "7hVfdsb0Bcfx9GgQwbU6F2nyXLKSz7L8yp5IHkhVnjn1ov0CQQD/N59bfrxTJeNp\n" +
            "bkPk8+Lg365ZqdHla4RqJOOZUYnta4qVQyi10Pqzik51CXoFdUx6udmBtkgVq2Mk\n" +
            "5t1XzXHbAkEA2Ecr8LRMGeBsEyKQ7enrbs0TozTJ7suuMh3epcUbtTj9myS4fajb\n" +
            "6m2P5wwz8sSvmHWjOQhbSQOuVVPcy4Y0JwJAdBVnrWUi4ar9GipmRVBNJL14/x2H\n" +
            "9BMIYoMu5sC4vL3KhgPLE4/fSCSjdQZ/ctYcmEHKVf6EIR8YdGNx0AsJOwJAJeos\n" +
            "MNFaufqW16/qmlq0tELtW2IouF0ql4yW+JaaaeWox+bjFNxiWTGF1apU/Q0v/1k4\n" +
            "GQp2/lDP4hOGlINdZwJBAKf2YDAX53Oe6p5JZnToUMU4F49J6iUgGf0QjWUWbENa\n" +
            "2rnuYDWVLTxb9ZEu/Gv20KCjhpsi6OiVew3fQS2HqGA=\n" +
            "-----END RSA PRIVATE KEY-----";

    @Test
    public void signRequestTest() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(BASE_XML.getBytes("UTF-8")));

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        ServerEncryptSamlAssertionAssertion.signRequest(doc, cert);

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAMLP2, "Response");
        Element responseElement = (Element)nodes.item(0);

        Element encryptedAssertion = null;
        for(Node n = responseElement.getFirstChild();n != null;n = n.getNextSibling()) {
            if(n.getNodeType() == Node.ELEMENT_NODE && SamlConstants.NS_SAML2.equals(n.getNamespaceURI())) {
                if("Assertion".equals(n.getLocalName())) {
                    fail();
                } else if("EncryptedAssertion".equals(n.getLocalName())) {
                    if(encryptedAssertion != null) {
                        fail();
                    } else {
                        encryptedAssertion = (Element)n;
                    }
                }
            }
        }

        assertNotNull(encryptedAssertion);

        // Find the symmetric key
        nodes = responseElement.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData");
        assertEquals(1, nodes.getLength());
        Element encryptedData = (Element)nodes.item(0);

        nodes = encryptedData.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "KeyInfo");
        assertEquals(2, nodes.getLength());
        assertSame(encryptedData, nodes.item(0).getParentNode());
        assertNotSame(encryptedData, nodes.item(1).getParentNode());
        Element keyInfo = (Element)nodes.item(0);

        nodes = keyInfo.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedKey");
        assertEquals(1, nodes.getLength());
        Element encryptedKey = (Element)nodes.item(0);

        nodes = keyInfo.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptionMethod");
        assertEquals(1, nodes.getLength());
        Element tempElement = (Element)nodes.item(0);
        assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-1_5", tempElement.getAttribute("Algorithm"));

        nodes = encryptedKey.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherData");
        assertEquals(1, nodes.getLength());
        Element cipherData = (Element)nodes.item(0);

        nodes = cipherData.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherValue");
        assertEquals(1, nodes.getLength());
        Element cipherValue = (Element)nodes.item(0);

        byte[] encryptedKeyBytes = Base64.decodeBase64(cipherValue.getTextContent().getBytes("UTF-8"));

        // Try to decrypt the symmetric key
        Cipher cipher = Cipher.getInstance(keyPair.getPrivate().getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] keyBytes = cipher.doFinal(encryptedKeyBytes);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ServerEncryptSamlAssertionAssertion.SYMMETRIC_KEY_ENCRYPTION_ALGORITHM);

        // Find the encrypted data
        nodes = encryptedData.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherData");
        assertEquals(2, nodes.getLength());
        assertNotSame(encryptedData, nodes.item(0).getParentNode());
        assertSame(encryptedData, nodes.item(1).getParentNode());
        cipherData = (Element)nodes.item(1);

        nodes = cipherData.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherValue");
        assertEquals(1, nodes.getLength());
        cipherValue = (Element)nodes.item(0);

        byte[] encryptedBytes = Base64.decodeBase64(cipherValue.getTextContent().getBytes("UTF-8"));

        // Try to decrypt the data
        cipher = Cipher.getInstance(secretKey.getAlgorithm());
        IvParameterSpec ivSpec = new IvParameterSpec(encryptedBytes, 0, 16);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] bytes = cipher.doFinal(encryptedBytes);

        String startTag = "<saml2:Assertion ID=\"samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">";
        String endTag = "</saml2:Assertion>";

        int startIndex = BASE_XML.indexOf(startTag);
        int endIndex = BASE_XML.indexOf(endTag) + endTag.length();

        String assertionXml = BASE_XML.substring(startIndex, endIndex);

        assertEquals(assertionXml, new String(bytes, 16, bytes.length - 16, "UTF-8"));
    }
}
