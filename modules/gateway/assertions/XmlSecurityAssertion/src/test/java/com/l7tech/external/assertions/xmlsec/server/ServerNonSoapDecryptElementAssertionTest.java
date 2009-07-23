package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

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
        SignerInfo testks = new SignerInfo(TestCertificateGenerator.convertFromBase64Pkcs12(TEST_KEYSTORE));
        SignerInfo otherks = new SignerInfo(TestCertificateGenerator.convertFromBase64Pkcs12(DATA_KEYSTORE));
        securityTokenResolver = new SimpleSecurityTokenResolver(null, new SignerInfo[] { testks, otherks });
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
        assertEquals(1, ((Object[])context.getVariable("elementsDecrypted")).length);
        assertEquals(1, ((Object[])context.getVariable("encryptionMethodUris")).length);
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes256-cbc", ((String[])context.getVariable("encryptionMethodUris"))[0]);
    }

    @Test
    public void testDecryptElement_encryptedForSomeoneElse() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED_FOR_SOMEONE_ELSE);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED_FOR_SOMEONE_ELSE));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory, null).checkRequest(context);
        assertEquals(AssertionStatus.BAD_REQUEST, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("After decryption attempt:\n" + XmlUtil.nodeToString(doc));
        assertEquals(1, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertNoVariable(context, "elementsDecrypted");
        assertNoVariable(context, "encryptionMethodUris");
    }

    @Test
    public void testDecryptElement_encryptedForData() throws Exception {
        logger.info("Attempting to decrypt:\n" + TEST_ENCRYPTED_FOR_DATA);
        NonSoapDecryptElementAssertion ass = new NonSoapDecryptElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name() = 'EncryptedData']"));
        Message request = new Message(XmlUtil.stringAsDocument(TEST_ENCRYPTED_FOR_DATA));
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());
        AssertionStatus result = new ServerNonSoapDecryptElementAssertion(ass, beanFactory, null).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        final Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Decrypted XML:\n" + XmlUtil.nodeToString(doc));
        assertEquals(0, doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength());
        assertEquals(1, ((Object[])context.getVariable("elementsDecrypted")).length);
        assertEquals(1, ((Object[])context.getVariable("encryptionMethodUris")).length);
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes256-cbc", ((String[])context.getVariable("encryptionMethodUris"))[0]);
    }

    private void assertNoVariable(PolicyEnforcementContext context, String variableName) {
        try {
            context.getVariable(variableName);
            fail("Should have thrown NoSuchVariableException for " + variableName);
        } catch (NoSuchVariableException e) {
            // Ok
        }
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

    public static final String DATA_KEYSTORE =
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA+gwgDCABgkqhkiG9w0BBwGggCSABIIDCTCCAwUwggMB\n" +
            "BgsqhkiG9w0BDAoBAqCCArIwggKuMCgGCiqGSIb3DQEMAQMwGgQUg/8v7fUN0dEZxeU8zekx+evu\n" +
            "rfMCAgQABIICgBmDuRvh05wdJ93ia4jBe0UlwDtx7PzJRl5bGY3HuekP7ScwEItKPULNAkCSAaWe\n" +
            "V4PfsptD/isgKOjxbzN0fdHSPedmqdSL51X/XlMl37v7lCJ+8FAf5BBxgxa53ffyJUMtXYxNr484\n" +
            "6qKcwdMX8osUyeTLBqkB0VAbgHzFm2Z40Q675wlZ+N3afvbybFM9tBzODym/T/SBe1DHNSRy729U\n" +
            "5mVpwgPhoD44T2vhPE4NNMY2oRPjzlWvnj+s7mMOqR4ZPY6tEeiy6YA/5Y6+KsGCosbOFemUMEHD\n" +
            "vXwoXviKwbZD9Ldia3RDc6mzsttYBaoA2vBwHAgDY3iqay2k8+B+fael924s4BrirMfWoslcH8ME\n" +
            "CI/yEqPvq/BhNz0VshYQIo2ClYlICrPeBxG/eS2QuaOhEHr843fFjg9u6oXhNHaMDHmP0uS1eLpm\n" +
            "chbCf8QxbhznoSrqr4R3/92/eWb5zRIiIUxpzuj/sbSOiikDw/N1zgLCqwMgKsuZwHfdiN2/v5Eh\n" +
            "clAPLy1N1Xhjn/NNU1fILCsXBcTJ5pSAiiBfjGUKelb4cahvZoUMrVxt/9kEM5JqWqzQI3cEqgWW\n" +
            "NL44cNMiEpRVrwJSlHgxAFfGA1CrJbAFMazdzjuLx9SpocYYJtJiCBTiZz6oxD0UsEQTjWUp6w/5\n" +
            "YL3JoAMehQbtcQU0sLauJWXJERKCWs9/6qkb7fLiyPTdo0VRnKpArbJ2u33MMy8117VX0yDPLDN8\n" +
            "SK/R0MITd1CgRhBi3iGOT/re9Dhfo0brWXnRi6ytuqg+CiM29Mu+uPeae0weE5lGHNT5XyWU/leO\n" +
            "7WI61aEIE4umddeXCZciZ99u4eIJOU8xPDAVBgkqhkiG9w0BCRQxCB4GAHMAcwBsMCMGCSqGSIb3\n" +
            "DQEJFTEWBBRCSBdXGhmyJ33I3tjg158bM57PsgAAAAAAADCABgkqhkiG9w0BBwaggDCAAgEAMIAG\n" +
            "CSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFANSVdxVsXfz2+DmmHbMUsbpQ5dXAgIEAKCABIIC\n" +
            "aOQs9S/QyR0bqIcj5UaFhGzZp+OvpH3NTy8K3xQr/b3kDEYw9h5suLmq7v9uHI6r/lkifXGuOnAr\n" +
            "ot6WJvz04tgzJrWAB/k2R/rrZrnua2eIhBJ3yYqj57CZvFjcsnd8GTqbl2bnMqdmeQh48wYM/fOk\n" +
            "BIICA+LSXy2XkRoi7yiJXcsEAgIdHT6IF9qeFGwn0BFI3GKkCtGsGVCSYohCFvG6hCH/deskSxp6\n" +
            "NerYrenOs+97MVImMBewiwJ5g7CV176yhuWWCG8A4rkCqJLbTLLMMO2oiLfdLsysQ0C0sPWB6Mzs\n" +
            "EtBW4CasSdQD14UDVR/xIjdXNO44YwqyXqoRnGyQWNgXQNwusGZHsCJ70VpVar61wcaAGQ+HVjim\n" +
            "h7j7+nE03wj4ZuymKqq375gALgmq3Sr0CrT4bF9578GMp7ENcuYi32mhuiqSMJuxfuLYRGz/9sRt\n" +
            "5vZr1kOFXrx1ckgkxVrASHP7UERxVg8VJix9yldBjYiQA3/tg7RhtWzYcS3iSh4VC2sGGtvtR/K2\n" +
            "AE8vga8yAP4QY5XachMyr1BwTy/fgM/3X4hT3sziS2nqDnJz/zoPRbN2e+IRaaH9MQzyfzKikzoW\n" +
            "UubzoqJ0SYNqmZsgwitPbSMNfLB/qMQ5UqjXqoabljAdhSgyRzcQFtY0uCvYTHzTpF5uX0xqNxQ8\n" +
            "LG+Oy4/Dl0PKCG6Q9WXjgc4qtQ1nkSFp+EKbXEjlqjxuFqm9vlVGbMIYV8hSQkp4Zvf/Z7tubbNE\n" +
            "AeZ8iutWsWGrl+L74sec9bVuOrUBihLtxe2Hl9ukn8VYbDVOmrqu2Y26Npiw8fzP/zRXAAAAAAAA\n" +
            "AAAAAAAAAAAAAAAAMD0wITAJBgUrDgMCGgUABBQ3T/1UECawLciqzMgBw4qafQrElAQUiqH1WtTr\n" +
            "OSLH0cErDr/i3Gx8Wq4CAgQAAAA=";

    public static String TEST_ENCRYPTED_FOR_DATA =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "\t<par:username>brian</par:username>\n" +
            "\t<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "\t\t\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t\t\t<dsig:X509Data>\n" +
            "\t\t\t\t\t\t<dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t\t\t<dsig:X509IssuerName>CN=data.l7tech.com</dsig:X509IssuerName>\n" +
            "\t\t\t\t\t\t\t<dsig:X509SerialNumber>2750606400783968375</dsig:X509SerialNumber>\n" +
            "\t\t\t\t\t\t</dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t</dsig:X509Data>\n" +
            "\t\t\t\t</dsig:KeyInfo>\n" +
            "\t\t\t\t<xenc:CipherData>\n" +
            "\t\t\t\t\t<xenc:CipherValue>In+E+fH8yet5hEkxSLuF6c9XN1eI1E8xT/WLw67MkrCuwky9eB+bECWOt911CwzLUzwxDSEOpEv4RMlstZWBHwMxrEYFMJmmbtYNLqXd3DK067jZETX1MT7mWr+E8kXBBCThxeEEAzT6Is120A94E2yecKI2BjEdLDflT4K7Xb4=</xenc:CipherValue>\n" +
            "\t\t\t\t</xenc:CipherData>\n" +
            "\t\t\t</xenc:EncryptedKey>\n" +
            "\t\t</dsig:KeyInfo>\n" +
            "\t\t<xenc:CipherData>\n" +
            "\t\t\t<xenc:CipherValue>hTmUhapmXukxslLlOQMYARey3v9Pj1mmRGkjDKqKYwAFBaWJfS9usMnV7ClTSmy6JAmMa2ymkc3Pxnq98g7cwQ==</xenc:CipherValue>\n" +
            "\t\t</xenc:CipherData>\n" +
            "\t</xenc:EncryptedData>\n" +
            "\t<par:notice_id>12345</par:notice_id>\n" +
            "</par:GetNoaParties>";

    // Test message encrypted for an earlier instance of the CN=data.l7tech.com key (different cert serial number)
    public static final String TEST_ENCRYPTED_FOR_SOMEONE_ELSE =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "\t<par:username>brian</par:username>\n" +
            "\t<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "\t\t\t\t<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "\t\t\t\t<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "\t\t\t\t\t<dsig:X509Data>\n" +
            "\t\t\t\t\t\t<dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t\t\t<dsig:X509IssuerName>CN=data.l7tech.com</dsig:X509IssuerName>\n" +
            "\t\t\t\t\t\t\t<dsig:X509SerialNumber>480673818902921845</dsig:X509SerialNumber>\n" +
            "\t\t\t\t\t\t</dsig:X509IssuerSerial>\n" +
            "\t\t\t\t\t</dsig:X509Data>\n" +
            "\t\t\t\t</dsig:KeyInfo>\n" +
            "\t\t\t\t<xenc:CipherData>\n" +
            "\t\t\t\t\t<xenc:CipherValue>GWCLzipub89lXg6e9SBu5xHfUyD3Wm8i2muo5muHBlhk07FDzlhJBASoX/LpNto9mjcOUJezXrRat9LhUTE9GHsFFn4FQl66o5fvqigOvj7h+IsUPuNXC0xo0zVnQANTb99t/AyNx7fZAahPbhwne0U/BLwex8MuKzLWbLmfabA=</xenc:CipherValue>\n" +
            "\t\t\t\t</xenc:CipherData>\n" +
            "\t\t\t</xenc:EncryptedKey>\n" +
            "\t\t</dsig:KeyInfo>\n" +
            "\t\t<xenc:CipherData>\n" +
            "\t\t\t<xenc:CipherValue>NqbcmF3u4Vj34h7J9Y7uk5gVvqguLHIHeFAzKfab+o4AhBHXXzSK9eO8xIfQiPBgXJnoJpTZvVnFE0Z3AHh+yQ==</xenc:CipherValue>\n" +
            "\t\t</xenc:CipherData>\n" +
            "\t</xenc:EncryptedData>\n" +
            "\t<par:notice_id>12345</par:notice_id>\n" +
            "</par:GetNoaParties>";

}

