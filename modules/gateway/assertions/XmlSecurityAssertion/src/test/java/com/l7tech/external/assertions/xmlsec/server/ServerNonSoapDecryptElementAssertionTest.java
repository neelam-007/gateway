package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.Pair;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapDecryptElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapDecryptElementAssertionTest.class.getName());

    private static SecurityTokenResolver securityTokenResolver;
    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        Pair<X509Certificate,PrivateKey> ks = TestCertificateGenerator.convertFromBase64Pkcs12(TEST_KEYSTORE);
        securityTokenResolver = new SimpleSecurityTokenResolver(ks.left, ks.right);
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", securityTokenResolver);
        }});
    }

    @Test
    public void testDecryptElement() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory, null).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
    }

    public static final String TEST_KEYSTORE =
            "MIIGVgIBAzCCBhAGCSqGSIb3DQEHAaCCBgEEggX9MIIF+TCCAx4GCSqGSIb3DQEHAaCCAw8EggML\n" +
            "MIIDBzCCAwMGCyqGSIb3DQEMCgECoIICsjCCAq4wKAYKKoZIhvcNAQwBAzAaBBQaj46rsqxvGtdc\n" +
            "F4A6PViGXJOoPgICBAAEggKAliSccagp7F+1Ifv0z7DgVOtIuuQ1M/i+g+Hdcg+8novUPMfBoWES\n" +
            "eCSZ/9qgINHvmFioMzoWKcMmUF0qPlB/gRRNq6NW5o+LxPLmpQkFACMkp2sp3mbkqeY6lvhuqxh4\n" +
            "EM1q4gAhDgmLrFLg3+m6qO+HiHcnXq+gNMc9LcQKubOaw1lYerskMdRIQLAdL68orbo+1o+EKqXS\n" +
            "JPRekRHPVkEro12r1R0xTVXzGY6L5apuyP4+dEVnSV+xZWHNaHS5mMl/2TeH08Vp9HFNQpQFU1oP\n" +
            "XSsXFcAP6rycBrw/nwujz8lFucNcEG9wBsmXScYs6Mfjz58iHj5MEwfeIu5mmN3tPAwOq74zN7Yn\n" +
            "sJ4mp+E7cJCkLq252EP0YbOm/KmwEqpdSMSD8M59BVRTbMG6tF4NHBZjpf2OeYAfo4aDQsYzRdh7\n" +
            "mcBJfokHBo3VExqHwgAtolYRDx+3xAcABvZYibLrUYqKF7m6dQM18UpFGbe3vtfDazWAk+CalNXc\n" +
            "GlyaL8F/8kKM5tdNwEeJDCZ0UjQmg1QOusWfdBWzN7+wYhJ52GKMtO0Jj3v3HeYCv9KUoe3jpXmD\n" +
            "x24Q/ZahE5jQ4sXL4H1Ul598SMA2hPz4U3PxlQWKRA9JK6r38Shg8j+RQdIcvuZHHrYHy3tXS7h0\n" +
            "LWcjbn9PsN9/Go1LWqXOHkFB8NgwRgQV6tTLAVug9vZiz4E8nsKzvlr9aOyJEmk0w6Z+fBwdYIIB\n" +
            "0fv2G8NJwZl6DDwT/22P1MMnYqRr7BZ6K3jvBKSnm6Vn+6BYuxB7DIxMnZxIYxhSSx7e31wq/Wz0\n" +
            "MvbC3LEpZQgDMqVd97QcJdFDO5OJnd9hxig+UswKbjE+MBkGCSqGSIb3DQEJFDEMHgoAZQBuAHQA\n" +
            "cgB5MCEGCSqGSIb3DQEJFTEUBBJUaW1lIDEyNDgyMzI2NDkyMzIwggLTBgkqhkiG9w0BBwagggLE\n" +
            "MIICwAIBADCCArkGCSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFFrUmi4ha436gkKpymSinqVr\n" +
            "DOiiAgIEAICCAoBeuxKc8iWN3aswqQpHWq9n657XqqpRpbTby5ASJSR3M3+YIQ4AJ6bhY4BIVTto\n" +
            "es5GFtTLH5l8VYbhNCHRh5gSmqKFIml5E2zqPkkXCTIgH+0sb1IkD6pjNs05n3vQkd4N/pMECueI\n" +
            "h8X04ojWgJE0/1RvVJrolPX7za1OhQu9T/kw6dBEqsKcG8Ik5tosnG/UUqX0Z02cnetX7aMEzRfv\n" +
            "DJLEEdP9SxQzC0aCj9CIPcVMWYLEBpg8fjESo/GC2OZQ9bI7t3CiSYpzqLA3Y1s+VOa1i+93+NE2\n" +
            "ncjejXzl5OKm/dA6pAu0qTjwjMEyLXFzT2y6W891x5a5cQocsUmQhNJcF2QRnt71pZDF5/XDRfD0\n" +
            "KcraNYqRpWuxyGSnXdbkUVCMbvuyvOq28lN037SOfvAqKWAGACur4hLZt+gd07UDVKfUXY9xKwgD\n" +
            "scux5ZdCE0l/FIYSLrw/fuj8zf71/6cEVCpf3ARsG9f3nx3ApNvDa7cqPfU2T3TfjPrcikmcVj0l\n" +
            "mADN/kP324yGGfzw9OWLJaF4uF3USIgYy2ljg0dhVfcpnxacZAWW8HNP59ISqs25+Rb4pTqd1+CX\n" +
            "tcAgfP2+Lr1QRxyNXpe7v/6TMuCbGQ8Wpf6fONXjXEg1pRj7G0OgTS77k5IWdnPTA8Lb4aaFTlha\n" +
            "JeykDheCjbPUBlhEktRjgSLHmkzR4cBT47Gfu0XJyT+NQw+ppBGnf9Bb5WlANNjSIO/yJXIp1Xks\n" +
            "3Xr00G9ELzHVkv4AIr/A5WBtQnutuExLJ4wg2PRlQrRTamuq6Vh0sZFxpaE6lRGPD94DIB4vMhsq\n" +
            "Wno7Ntev5R0t2yJ7AKLK9da73tcxT0j+MD0wITAJBgUrDgMCGgUABBSnAGLsWZ+fsDZQznWusnp8\n" +
            "/zG5ZwQUHITgA39NzDQicvtCtoyVlDfEF7gCAgQA";

    public static final String TEST_ENCRYPTED =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><dsig:X509Data><dsig:X509IssuerSerial><dsig:X509IssuerName>CN=test</dsig:X509IssuerName><dsig:X509SerialNumber>7730273685284174799</dsig:X509SerialNumber></dsig:X509IssuerSerial></dsig:X509Data></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>O0xs2VQa0p3d9tzUvy+2ljjgef/RX2zDMo8FIQ9rjYCCRKDFEsLb5XOFQWK5MtnTl+bC68khTfJq6FeKh+3NBI9D41BJipZAWAI+HZrnyU0iUPSJL936AAWsq9bgL+RkaGkXjsWAjb/XCluDMlQ+9pK2CiLoyIMRSHirES72vSM=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey></dsig:KeyInfo><xenc:CipherData><xenc:CipherValue>B3fvKCstaZwlTxSvsFBHZnoEobEjqbIy0P+hRxhotFy9vxacIfbpQRMrYbpRh4lJEUdgiDoD6JVeNT1xWJmakQ==</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

}

