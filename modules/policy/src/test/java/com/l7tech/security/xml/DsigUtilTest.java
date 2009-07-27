package com.l7tech.security.xml;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.xml.soap.SoapUtil;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
 *
 */
public class DsigUtilTest {
    private static final Key secretKey = new SecretKeySpec(new byte[] { (byte)0, (byte) 0 }, "proprietary");
    private static Key rsaPublicKey;

    @BeforeClass
    public static void setUpSuite() throws Exception {
        rsaPublicKey = new TestCertificateGenerator().generate().getPublicKey();
    }

    @Test
    @BugNumber(7526)
    public void testHmacOutputLength_notpresent() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "       <z:HMACOutputLengths>1</z:HMACOutputLengths>\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "</foo>"), secretKey);
    }

    @Test
    @BugNumber(7526)
    public void testHmacOutputLength_presentButWrongNs() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "       <z:HMACOutputLength>1</z:HMACOutputLength>\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "</foo>"), secretKey);
    }

    @Test(expected = SignatureException.class)
    @BugNumber(7526)
    public void testHmacOutputLength_present() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                " <x:SignedInfo>" +
                "  <x:SignatureMethod Algorithm=\"urn:blah\">\n" +
                "   <x:HMACOutputLength/>\n" +
                "  </x:SignatureMethod>\n" +
                " </x:SignedInfo>" +
                "</x:Signature>\n" +
                "</foo>"), secretKey);
    }

    @Test
    @BugNumber(7526)
    public void testHmacOutputLength_outside() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "<z:HMACOutputLength>1</z:HMACOutputLength>\n" +
                "</foo>"), secretKey);
    }

    @Test(expected = SignatureException.class)
    @BugNumber(7528)
    public void testHmacWithRsaPublicKey() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><wsu:Timestamp wsu:Id=\"Timestamp-2-3da76250a5f88758a573018569639c26\"><wsu:Created>2009-07-15T03:19:55.201295877Z</wsu:Created><wsu:Expires>2009-07-15T04:19:55.201Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\">MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"/><ds:Reference URI=\"#Body-1-c67a6221595a455b11752be4a50fc8da\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>07QjRM+zFVZ5e+SpsSzYidXioak=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#Timestamp-2-3da76250a5f88758a573018569639c26\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>DYUO6YI6uU4a67DK1EJR6nYN+y8=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ccigGEfagZJPMqy1uSWDhJ0Wp+A=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>n0PTfkaVFF8Ym2FpgJMhh3jcFp0=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:Reference URI=\"#BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soapenv:Header>\n" +
                "    <soapenv:Body wsu:Id=\"Body-1-c67a6221595a455b11752be4a50fc8da\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
                "            <tns:delay>0</tns:delay>\n" +
                "        </tns:listProducts>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>"), rsaPublicKey);
    }

    @BugNumber(7528)
    public void testHmacWithSecretKey() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><wsu:Timestamp wsu:Id=\"Timestamp-2-3da76250a5f88758a573018569639c26\"><wsu:Created>2009-07-15T03:19:55.201295877Z</wsu:Created><wsu:Expires>2009-07-15T04:19:55.201Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\">MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"/><ds:Reference URI=\"#Body-1-c67a6221595a455b11752be4a50fc8da\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>07QjRM+zFVZ5e+SpsSzYidXioak=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#Timestamp-2-3da76250a5f88758a573018569639c26\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>DYUO6YI6uU4a67DK1EJR6nYN+y8=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>ccigGEfagZJPMqy1uSWDhJ0Wp+A=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>n0PTfkaVFF8Ym2FpgJMhh3jcFp0=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:Reference URI=\"#BinarySecurityToken-0-fa5890c661587e974ee3d8260b0f7dad\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soapenv:Header>\n" +
                "    <soapenv:Body wsu:Id=\"Body-1-c67a6221595a455b11752be4a50fc8da\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
                "            <tns:delay>0</tns:delay>\n" +
                "        </tns:listProducts>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>"), secretKey);
    }

    @Ignore("Enable to ensure that dsig prechecks have negligible impact on performance")
    @Test
    public void testCheckPerformance() throws Exception {
        final Element sigEl = getSigElement(
                "<foo xmlns=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                " <x:SignedInfo>" +
                "  <x:SignatureMethod Algorithm=\"urn:blah\"/>\n" +
                " </x:SignedInfo>" +
                "</x:Signature>\n" +
                "</foo>");

        // Ensure DOM is fully parsed
        XmlUtil.nodeToOutputStream(sigEl.getOwnerDocument(), new NullOutputStream());

        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    DsigUtil.precheckSigElement(sigEl, secretKey);
                } catch (SignatureException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1000000, 10, "Dsig prechecks").run();
    }

    private static Element getSigElement(String testDoc) {
        Document doc = XmlUtil.stringAsDocument(testDoc);
        Element sig = (Element)doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature").item(0);
        assertNotNull("Must be able to locate ds:Signature element", sig);
        assertEquals("Signature", sig.getLocalName());
        return sig;
    }

    // Ensure behavior of DsigUtil.createEnvelopedSignature() has not changed due to SupportedSignatureMethods refactor
    @Test
    public void testGetSignatureMethodForSignerPrivateKey() throws Exception {
        Pair<X509Certificate, PrivateKey> rsa = new TestCertificateGenerator().keySize(1024).generateWithKey();
        Pair<X509Certificate, PrivateKey> ec = new TestCertificateGenerator().curveName("secp384r1").generateWithKey();
        SecretKey secretKey = new SecretKeySpec(new byte[] { 0, 0 }, "proprietarySecretKey");

        Key[] keys = { rsa.right, ec.right, secretKey };
        String[] hashes = { "SHA-1", "SHA-256", "SHA-384", "SHA-512" };

        for (String hash : hashes) {
            for (Key key : keys) {
                String wantKeyAlg = key instanceof SecretKey ? "SecretKey" : key.getAlgorithm();

                try {
                    SupportedSignatureMethods method = DsigUtil.getSignatureMethodForSignerPrivateKey(key, hash);
                    assertEquals(method.getKeyAlgorithmName(), wantKeyAlg);
                    assertEquals(method.getDigestAlgorithmName(), hash);

                } catch (SignatureException e) {
                    assertNull("getSignatureMethodForSignerPrivateKey() should only throw SignatureException if the requested combination is not supported",
                            SupportedSignatureMethods.fromKeyAndMessageDigest(wantKeyAlg, hash));
                }
            }
        }
    }
}

