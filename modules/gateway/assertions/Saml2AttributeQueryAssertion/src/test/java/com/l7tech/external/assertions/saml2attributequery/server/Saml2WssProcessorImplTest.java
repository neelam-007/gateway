package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.message.Message;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.xml.soap.SoapUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 3-Feb-2009
 * Time: 6:12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2WssProcessorImplTest {
    private static final String QUERY_XML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soapenv:Header></soapenv:Header>\n" +
            "  <soapenv:Body>\n" +
            "    <samlp:AttributeQuery xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2004-12-05T09:22:04Z\" Version=\"2.0\">\n" +
            "      <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#aaf23196-1773-2113-474a-fe114412ab72\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>9iWhEcV8hVeWeGYylZs8jQF8QuA=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>dAFiKXCDpbb4x16UjqneI0gh+qJNhp6XsQF7JN8t0REidjL9Y2ZOPAANuoEouWgatJwTctucvP417dloSdxWFcV5HSPUh7+4PEqTXQJwuY14TKpCCwgzsxVsdn6bZpnwy1zX5TOcu9F4NsPuMyS7kMh9ul7v5u0iCeZH6kMWpmk=</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:KeyIdentifier EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">x3wjw2fVJClXxuT+EGlDDjLknc4=</wsse:KeyIdentifier></wsse:SecurityTokenReference></KeyInfo></ds:Signature><saml:Subject>\n" +
            "        <saml:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified\">jabba</saml:NameID>\n" +
            "      </saml:Subject>\n" +
            "      <saml:Attribute FriendlyName=\"givenName\" Name=\"urn:oid:givenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "        <saml:AttributeValue>test</saml:AttributeValue>\n" +
            "        <saml:AttributeValue>Jabba</saml:AttributeValue>\n" +
            "      </saml:Attribute>\n" +
            "      <saml:Attribute FriendlyName=\"cn\" Name=\"urn:oid:cn\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"></saml:Attribute>\n" +
            "    </samlp:AttributeQuery>\n" +
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

    @BeforeClass
    public static void initBc() {
        BouncyCastleProvider bcprov = new BouncyCastleProvider();
        if (Security.getProvider(bcprov.getName()) == null) {
            Security.addProvider(bcprov);
        }
    }

    @Test
    public void processMessageTest1() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(QUERY_XML.getBytes("UTF-8")));
        Message message = new Message(doc,0);

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        Saml2WssProcessorImpl securityProcessor = new Saml2WssProcessorImpl(message);
        securityProcessor.setSecurityTokenResolver(new DummySecurityTokenResolver(cert, keyPair.getPrivate()));

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAMLP2, "AttributeQuery");
        Element signedElement = (Element)nodes.item(0);
        nodes = doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
        Element signatureElement = (Element)nodes.item(0);

        securityProcessor.checkSigningCertValidity = false; // test cert has expired, so need to turn off validity check
        ProcessorResult result = securityProcessor.processMessage(signedElement, signatureElement);
        boolean signatureFound = false;
        for(SignedElement s : result.getElementsThatWereSigned()) {
            if(s.asElement() == signedElement) {
                signatureFound = true;
                break;
            }
        }

        assertTrue(signatureFound);
    }

    @Test
    public void processMessageTest2() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(QUERY_XML.getBytes("UTF-8")));
        Message message = new Message(doc,0);

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        Saml2WssProcessorImpl securityProcessor = new Saml2WssProcessorImpl(message);
        securityProcessor.setSecurityTokenResolver(new DummySecurityTokenResolver(cert, keyPair.getPrivate()));

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAMLP2, "AttributeQuery");
        Element signedElement = (Element)nodes.item(0);
        nodes = doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
        Element signatureElement = (Element)nodes.item(0);

        // Change the subject, to cause the signature to fail
        nodes = signedElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "NameID");
        Element nameIdElement = (Element)nodes.item(0);
        nameIdElement.setTextContent("test");

        try {
            ProcessorResult result = securityProcessor.processMessage(signedElement, signatureElement);
            fail();
        } catch(Exception e) {
        }
    }
}
