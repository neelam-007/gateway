package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.util.Charsets;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 3-Feb-2009
 * Time: 7:43:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSignerTest {
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

    private static final String SIGNED_XML_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soapenv:Header/>\n" +
            "  <soapenv:Body>\n" +
            "    <samlp2:Response xmlns:samlp2=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" ID=\"samlp2-5cb4e012f211b7fc935d002defae3562\" InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "      <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "      <samlp2:Status>\n" +
            "        <samlp2:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
            "      </samlp2:Status>\n" +
            "      <saml2:Assertion ID=\"samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "        <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "        <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>XDXOSYLe/Wwzd0k2CZuKALAKr5g=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>dM2m9/Ym1xId63Zr0O5KMifA8icgBuTkjomepYdFEQ/GhDMydEr8fCLXnD5xHmwAk3TbzspkLkInfg6SaUPiliXafaTT7EITUk3IeczS+0HWLItc5QDl64TrWOaWpVhflV4TGTW5T+yhBIs+Z+sh4dPzoLdHxXLI+h0S0KBaHUU=</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SKI>x3wjw2fVJClXxuT+EGlDDjLknc4=</X509SKI></X509Data></KeyInfo></ds:Signature><saml2:Subject>\n" +
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

    private static final String SIGNED_XML_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soapenv:Header/>\n" +
            "  <soapenv:Body>\n" +
            "    <samlp2:Response xmlns:samlp2=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" ID=\"samlp2-5cb4e012f211b7fc935d002defae3562\" InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "      <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "      <samlp2:Status>\n" +
            "        <samlp2:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
            "      </samlp2:Status>\n" +
            "      <saml2:Assertion ID=\"samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "        <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "        <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>XDXOSYLe/Wwzd0k2CZuKALAKr5g=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>dM2m9/Ym1xId63Zr0O5KMifA8icgBuTkjomepYdFEQ/GhDMydEr8fCLXnD5xHmwAk3TbzspkLkInfg6SaUPiliXafaTT7EITUk3IeczS+0HWLItc5QDl64TrWOaWpVhflV4TGTW5T+yhBIs+Z+sh4dPzoLdHxXLI+h0S0KBaHUU=</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509Certificate>MIIDyTCCAzKgAwIBAgIGAR3QDW8IMA0GCSqGSIb3DQEBBQUAMIG3MQswCQYDVQQGEwJDQTELMAkGA1UECAwCQkMxGzAZBgNVBAoMEkxheWVyIDcgVGVjaG5vbG9neTERMA8GA1UECwwIVGFjdGljYWwxFjAUBgNVBAMMDU5vcm1hbiBKb3JkYW4xGjAYBgoJkiaJk/IsZAEZFgpsYXllcjd0ZWNoMTcwNQYKCZImiZPyLGQBGRYnY29tL2VtYWlsQWRkcmVzcz1uam9yZGFuQGxheWVyN3RlY2guY29tMB4XDTA3MTEyNTE5NTAyMFoXDTA5MTEyNDE5NTAyMFowgbcxCzAJBgNVBAYTAkNBMQswCQYDVQQIDAJCQzEbMBkGA1UECgwSTGF5ZXIgNyBUZWNobm9sb2d5MREwDwYDVQQLDAhUYWN0aWNhbDEWMBQGA1UEAwwNTm9ybWFuIEpvcmRhbjEaMBgGCgmSJomT8ixkARkWCmxheWVyN3RlY2gxNzA1BgoJkiaJk/IsZAEZFidjb20vZW1haWxBZGRyZXNzPW5qb3JkYW5AbGF5ZXI3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANed4rC098rPSShMnq9Rf5a++Z5uu4FwcSY0S2tPpiNMSCmSQRjLZgwsriHolnaUWFUUU6gfZ3ZOBSlgcZ7hf1sKZXMN/wwLLR/gqs5mw7fVC7Jl+8ufHPPJ/gnl0l8hqCWSiCu6ZXwJAG+K4K1j+5L0N35H2isCe64u2ZRdDtRdAgMBAAGjgd0wgdowLQYDVR0JBCYwJDAQBggrBgEFBQcJBDEEEwJDQTAQBggrBgEFBQcJBDEEEwJUVzAxBgNVHREEKjAogg5sYXllcjd0ZWNoLmNvbYEWbmpvcmRhbkBsYXllcjd0ZWNoLmNvbTAxBgNVHRIEKjAogg5sYXllcjd0ZWNoLmNvbYEWbmpvcmRhbkBsYXllcjd0ZWNoLmNvbTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIBBjAWBgNVHSAEDzANMAsGCWCGSAFlAgELBTATBgNVHSUEDDAKBggrBgEFBQcDAzANBgkqhkiG9w0BAQUFAAOBgQCurgro6YQrFO8oT+YKnuJ7WOrX8MRQWrnTNIWN/SNib/FNSdz0P8q/hohRQ/2F79m+xrXPBGieanYASN3MgBAN57/gBBAA0bzHDYsjP31bO+IfqubLJ6W5RKbxOfsW+ceTTDcSm3DhT+LlfMVzN7VNOtuUaa5AG6CdLDzMUo2avg==</X509Certificate></X509Data></KeyInfo></ds:Signature><saml2:Subject>\n" +
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

    private static final String SIGNED_XML_3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soapenv:Header/>\n" +
            "  <soapenv:Body>\n" +
            "    <samlp2:Response xmlns:samlp2=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" ID=\"samlp2-5cb4e012f211b7fc935d002defae3562\" InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "      <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "      <samlp2:Status>\n" +
            "        <samlp2:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\n" +
            "      </samlp2:Status>\n" +
            "      <saml2:Assertion ID=\"samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\" IssueInstant=\"2009-02-03T19:55:36.844Z\" Version=\"2.0\">\n" +
            "        <saml2:Issuer>http://njordan-desktop:8080/</saml2:Issuer>\n" +
            "        <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#samlp2Assertion-3e3d1fa6dd6f3d7936f9ddd9af088af1\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>XDXOSYLe/Wwzd0k2CZuKALAKr5g=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>dM2m9/Ym1xId63Zr0O5KMifA8icgBuTkjomepYdFEQ/GhDMydEr8fCLXnD5xHmwAk3TbzspkLkInfg6SaUPiliXafaTT7EITUk3IeczS+0HWLItc5QDl64TrWOaWpVhflV4TGTW5T+yhBIs+Z+sh4dPzoLdHxXLI+h0S0KBaHUU=</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:KeyIdentifier EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1\">Rkpr9scEUraxMEeGYDtHNf73V24=</wsse:KeyIdentifier></wsse:SecurityTokenReference></KeyInfo></ds:Signature><saml2:Subject>\n" +
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

    @BeforeClass
    public static void initBc() {
        BouncyCastleProvider bcprov = new BouncyCastleProvider();
        if (Security.getProvider(bcprov.getName()) == null) {
            Security.addProvider(bcprov);
        }
    }

    @Test
    public void testSignSamlpRequest1() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(BASE_XML.getBytes("UTF-8")));

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Assertion");
        Element assertionElement = (Element)nodes.item(0);

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        RequestSigner.signSamlpRequest(doc, assertionElement, keyPair.getPrivate(), new X509Certificate[] {cert}, KeyInfoInclusionType.STR_SKI);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));

        assertEquals(SIGNED_XML_1, fixLineEndings(new String(baos.toByteArray(), Charsets.UTF8)));
    }

    @Test
    public void testSignSamlpRequest2() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(BASE_XML.getBytes("UTF-8")));

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Assertion");
        Element assertionElement = (Element)nodes.item(0);

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        RequestSigner.signSamlpRequest(doc, assertionElement, keyPair.getPrivate(), new X509Certificate[] {cert}, KeyInfoInclusionType.CERT);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));

        String s = new String(baos.toByteArray(), "UTF-8");

        assertEquals(SIGNED_XML_2, fixLineEndings(new String(baos.toByteArray(), Charsets.UTF8)));
    }

    @Test
    public void testSignSamlpRequest3() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(BASE_XML.getBytes("UTF-8")));

        NodeList nodes = doc.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Assertion");
        Element assertionElement = (Element)nodes.item(0);

        PEMReader pemReader = new PEMReader(new StringReader(CERTIFICATE_PEM));
        X509Certificate cert = (X509Certificate)pemReader.readObject();
        pemReader = new PEMReader(new StringReader(PRIVATE_KEY_PEM));
        KeyPair keyPair = (KeyPair)pemReader.readObject();

        RequestSigner.signSamlpRequest(doc, assertionElement, keyPair.getPrivate(), new X509Certificate[] {cert}, KeyInfoInclusionType.STR_THUMBPRINT);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));

        String s = new String(baos.toByteArray(), "UTF-8");

        assertEquals(SIGNED_XML_3, fixLineEndings(new String(baos.toByteArray(), Charsets.UTF8)));
    }

    private static String fixLineEndings(String instr) {
        return instr.replaceAll("\r\n|\n\r|(?<!\n)\r|(?<!\r)\n", "\n");
    }
}
