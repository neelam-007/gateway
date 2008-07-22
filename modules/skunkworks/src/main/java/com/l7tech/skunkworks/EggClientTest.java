/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.ssl.SslPeer;
import org.w3c.dom.Document;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class EggClientTest {
    private static final Logger logger = Logger.getLogger(EggClientTest.class.getName());
    private static SslPeer sslPeer;

    public static void main(String[] args) throws Exception {
        JceProvider.init();

        String sa = "\"http://warehouse.acme.com/ws/listProducts\"";

        Ssg ssg = new Ssg(10, "data.l7tech.com");
        ssg.setUsername("mike");
        ssg.setPersistPassword("asdfasdf".getBytes());
        SsgKeyStoreManager ksm = ssg.getRuntime().getSsgKeyStoreManager();
        final X509Certificate dataCert = CertUtils.decodeCert(HexUtils.decodeBase64(SERVER_CERT, true));
        final X509Certificate aliceCert = null;//TestDocuments.getWssInteropAliceCert();
        if (ssg.getServerCertificate() == null || !CertUtils.certsAreEqual(ssg.getServerCertificate(), dataCert))
            ksm.saveSsgCertificate(dataCert);
        if (ssg.getClientCertificate() == null || !CertUtils.certsAreEqual(ssg.getClientCertificate(), aliceCert));
            ksm.saveClientCertificate(null/*TestDocuments.getWssInteropAliceKey()*/, aliceCert, "asdfasdf".toCharArray());

        Document doc = XmlUtil.stringToDocument(REQ);
        GenericHttpRequestParams req = new GenericHttpRequestParams(new URL("https://data.l7tech.com:8443/ssg/soap"));
        req.setExtraHeaders(new HttpHeader[] {new GenericHttpHeader("SOAPAction", sa)});

        SimpleHttpClient client = ssg.getRuntime().getHttpClient();
        SimpleHttpClient.SimpleXmlResponse got = client.postXml(req, doc);
        logger.info("Posted successfully.  Result status = " + got.getStatus());
        logger.info("Result content type = " + got.getContentType());
        logger.info("Result content length = " + got.getContentLength());
        logger.info("Result content (reformatted):\n" + XmlUtil.nodeToFormattedString(got.getDocument()));
    }

    public static final String SERVER_CERT = "MIICFjCCAX+gAwIBAgIIcIus8v6FUa4wDQYJKoZIhvcNAQEFBQAwHzEdMBsGA1UEAxMUcm9vdC5k\n" +
            "YXRhLmw3dGVjaC5jb20wHhcNMDQwNzEzMjIxMTQ3WhcNMDYwNzEzMjIxMTQ3WjAaMRgwFgYDVQQD\n" +
            "Ew9kYXRhLmw3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIapFkyAC6djQiq2\n" +
            "7GDONltOS67FmJkEU/iAFg5JW7Tbhx0fNnyLqkrerpMwzMbSqN2Q1clCDyU7Wpa6lPoXGuOfhasB\n" +
            "thlIMZuhYKu+QAwipdHwsyWJn9hcqVzAc+KNqcfovUpITnM/149zojjUKKsL4MZyIlFTDjO4Wwil\n" +
            "X+MnAgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgWgMB0GA1UdDgQWBBRt+zXV\n" +
            "b6zrr/IAHfQ73JkXZ8v0zzAfBgNVHSMEGDAWgBSXFmqQs1KU4jlBhZmozPBjzyP4eTANBgkqhkiG\n" +
            "9w0BAQUFAAOBgQCQoQKUOvvNui/J/McpqkNdnwpWbV+zcM7daecYuWWwgkrHX1ForNhbCfqVrcao\n" +
            "m0B8w9eQcMSV/m5yAwAyH7oVtoiPiiiSciTbsgmVmEu8OHeb76Ths913TDCGPZozHt07r88l6lK7\n" +
            "o4QR4Jtny+np9Ctu4beAMb9fRURXWMHxtQ==";

    public static final String REQ = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" SOAP-ENV:mustUnderstand=\"1\"><saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AssertionID=\"EggSvTest-1\" IssueInstant=\"2005-08-18T15:53:51.968-07:00\" Issuer=\"data.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"0\"><saml:Conditions NotBefore=\"2005-08-18T15:53:51.968-07:00\" NotOnOrAfter=\"2005-08-19T15:53:52.031-07:00\"></saml:Conditions><saml:AttributeStatement><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">mike@l7tech.com</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:sender-vouches</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:Attribute AttributeName=\"ProductPortfolio\" AttributeNamespace=\"http://www.egg.com/ns/attributes\"><saml:AttributeValue><xyz:ProductPortfolio xmlns:xyz=\"http://www.egg.com/ns/products\">\n" +
            "                                <xyz:Product>\n" +
            "                                    <xyz:Name>Red</xyz:Name>\n" +
            "                                    <xyz:Type>CreditCard</xyz:Type>\n" +
            "                                    <xyz:Activated>true</xyz:Activated>\n" +
            "                                </xyz:Product>\n" +
            "                                <xyz:Product>\n" +
            "                                    <xyz:Name>Blue</xyz:Name>\n" +
            "                                    <xyz:Type>CreditCard</xyz:Type>\n" +
            "                                    <xyz:Activated>true</xyz:Activated>\n" +
            "                                </xyz:Product>\n" +
            "                            </xyz:ProductPortfolio></saml:AttributeValue></saml:Attribute><saml:Attribute AttributeName=\"CustomerRiskCategory\" AttributeNamespace=\"http://www.egg.com/ns/attributes\"><saml:AttributeValue>low</saml:AttributeValue></saml:Attribute></saml:AttributeStatement><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#EggSvTest-1\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>TUP/5pXjKn/PjYtVHfXGo1Z9kaM=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>opZH7UrN95xMX+szKIJ2PdQLoSAKrpoC6mZ4GQce3b8ZR1K2LUFbJso+u15SWYzafBRRJV4itMO/iiiu6Bk/r029ixDQgz0f5rtwVIwFT5yqfKmZuGZQHH/01Ez+vfz3vkBHPk4aGt8Mv4xtsupJ/y/mFc9mR2GHQ+2lEnFbNao=</ds:SignatureValue>\n" +
            "  <KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "    <wsse:SecurityTokenReference><wsse:KeyIdentifier ValueType=\"http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-soap-message-security-1.1#ThumbprintSHA1\">9Ehmp+dbGAm4PO8uKwCsaZDNXvA=</wsse:KeyIdentifier></wsse:SecurityTokenReference>\n" +
            "  </KeyInfo></ds:Signature></saml:Assertion></wsse:Security></SOAP-ENV:Header><SOAP-ENV:Body><listProducts xmlns=\"http://warehouse.acme.com/ws\"/></SOAP-ENV:Body></SOAP-ENV:Envelope>";
}
