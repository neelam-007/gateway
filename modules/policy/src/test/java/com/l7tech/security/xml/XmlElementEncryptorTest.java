package com.l7tech.security.xml;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.soap.SoapUtil;
import com.safelogic.cryptocomply.jce.provider.SLProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class XmlElementEncryptorTest {
    private static final Logger logger = Logger.getLogger(XmlElementEncryptorTest.class.getName());

    static String recipb64;
    static X509Certificate recipCert;
    static PrivateKey recipPrivateKey;
    static String expectedRsaProvider;

    private static final String TEST_XML =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <par:password>somepassword</par:password> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

    @Before
    public void setUpCertAndProviders() throws Exception {
        JceProvider.init(); // Init JceProvider...
        useDefaultProvider(); // ...then ensure we use the default providers for the test (unless the test manually switches it back)
        Pair<X509Certificate, PrivateKey> got = TestKeys.getCertAndKey("RSA_1024");
        recipCert = got.left;
        recipCert.checkValidity();
        recipPrivateKey = got.right;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());
        logger.info("Recipient certificate PKCS#12 keystore: \n" + TestCertificateGenerator.convertToBase64Pkcs12(got.left, got.right));
    }

    @After
    public void cleanUp() throws Exception {
        SyspropUtil.clearProperties( XmlElementEncryptor.PROP_OAEP_DIGEST_SHA256 );
        useDefaultProvider();
    }

    private static void useCCJProvider() {
        Provider provider = new SLProvider();
        Security.removeProvider("WF");
        Security.insertProviderAt(provider, 1);
        expectedRsaProvider = "WF";
    }

    private static void useDefaultProvider() {
        Security.removeProvider( "WF" );
        Security.removeProvider( "RsaJsse" );
        expectedRsaProvider = "SunJCE";
    }

    private static void setCCJ(boolean useCCJ ) {
        if ( useCCJ ) {
            useCCJProvider();
        } else {
            useDefaultProvider();
        }
    }

    @AfterClass
    public static void cleanUpAll() throws Exception {
        SyspropUtil.clearProperties( XmlElementEncryptor.PROP_OAEP_DIGEST_SHA256 );
    }

    @BugNumber(11697)
    @BugId("SSM-3743")
    @Test
    public void testTypeAndRecipientAttributes() throws Exception {
        XmlElementEncryptionConfig rawConfig = new XmlElementEncryptionConfig();
        rawConfig.setRecipientCertificateBase64(recipb64);
        final XmlElementEncryptionResolvedConfig config =
            new XmlElementEncryptionResolvedConfig(CertUtils.decodeFromPEM(recipb64, false), XencUtil.AES_128_CBC, false);

        final String customUri = "customuri";
        config.setEncryptedDataTypeAttribute(customUri);
        final String recipientValue = "my recipient value";
        config.setEncryptedKeyRecipientAttribute(recipientValue);

        XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(config);
        final Document doc = XmlUtil.parse(TEST_XML);
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc, false, null, null);
        {
            final Element encryptedKey = keyPair.left;
            assertNotNull(encryptedKey);
            final NamedNodeMap attributes = encryptedKey.getAttributes();
            final Node type = attributes.getNamedItem("Recipient");
            assertNotNull(type);
            assertEquals(recipientValue, type.getTextContent());
        }

        {
            final Element encryptedData = elementEncryptor.encryptAndReplaceElement(doc.getDocumentElement(), keyPair);
            final NamedNodeMap attributes = encryptedData.getAttributes();
            final Node type = attributes.getNamedItem("Type");
            assertNotNull(type);
            assertEquals(customUri, type.getTextContent());
        }
    }

    @Test
    @BugId("SSG-7462")
    public void testOaep() throws Exception {
        runOaepTest( false );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaep_withCCJ() throws Exception {
        runOaepTest( true );
    }

    private void runOaepTest( boolean useCCJ ) throws Exception {
        setCCJ( useCCJ );

        XmlElementEncryptionConfig rawConfig = new XmlElementEncryptionConfig();
        rawConfig.setRecipientCertificateBase64(recipb64);
        final XmlElementEncryptionResolvedConfig config =
            new XmlElementEncryptionResolvedConfig(CertUtils.decodeFromPEM(recipb64, false), XencUtil.AES_128_CBC, false);

        XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(config);
        final Document doc = XmlUtil.parse(TEST_XML);
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc, true, null, null);
        elementEncryptor.encryptAndReplaceElement(doc.getDocumentElement(), keyPair);
        String encryptedXml = XmlUtil.nodeToString(doc);
        assertTrue("encrypted data must be present", encryptedXml.contains("EncryptedData"));
        assertTrue("plaintext must be gone", !encryptedXml.contains("somepassword"));
        assertTrue("RSA 1.5 must not be used", !encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
        assertTrue("OAEP must be used", encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertTrue("Custom DigestMethod should not have been used", !encryptedXml.contains( "DigestMethod" ) );
        assertTrue("SHA256 should not have been used", !encryptedXml.contains( "sha256" ) );

        // Now try and unwrap and decrypt it
        Document doc2 = XmlUtil.parse( encryptedXml );
        XmlElementDecryptor.unwrapDecryptAndReplaceElement( doc2.getDocumentElement(),
                new SimpleSecurityTokenResolver( recipCert, recipPrivateKey ),
                null,
                null );
        String decryptedXml = XmlUtil.nodeToString( doc2 );
        assertTrue("Shall have been decrypted OK from just output XML and recipient private key",
                decryptedXml.contains( "password>somepassword" ) );

        // Make sure decryption uses actual OAEP with default params
        OAEPParameterSpec spec = new OAEPParameterSpec( "SHA-1", "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified( new byte[0] ) );
        byte[] decryptedKey = unwrapKeyWithOaep( XmlUtil.stringAsDocument( encryptedXml ).getDocumentElement(), spec, useCCJ );
        assertArrayEquals( keyPair.right.getEncoded(), decryptedKey );
    }

    // Examines a passed-in xenc:EncryptedData element and tries to unwrap an embedded KeyInfo CipherValue using the specified OAEPParameterSpec
    private static byte[] unwrapKeyWithOaep( Element encryptedData, OAEPParameterSpec spec, boolean useCCJ ) throws Exception {
        // Check default provider for RSA was as expected during encryption
        assertEquals( expectedRsaProvider, Cipher.getInstance( "RSA/ECB/OAEPPadding" ).getProvider().getName() );

        Element keyInfo = XmlUtil.findExactlyOneChildElementByName( encryptedData, SoapUtil.DIGSIG_URI, "KeyInfo" );
        Element encryptedKey = XmlUtil.findExactlyOneChildElementByName( keyInfo, SoapUtil.XMLENC_NS, "EncryptedKey" );
        Element cipherData = XmlUtil.findExactlyOneChildElementByName( encryptedKey, SoapUtil.XMLENC_NS, "CipherData" );
        Element cipherValue = XmlUtil.findExactlyOneChildElementByName( cipherData, SoapUtil.XMLENC_NS, "CipherValue" );
        byte[] ciphertextBytes = HexUtils.decodeBase64( XmlUtil.getTextValue( cipherValue ) );

        Cipher rsa = Cipher.getInstance( "RSA/ECB/OAEPPadding", "SunJCE" );
        rsa.init( Cipher.DECRYPT_MODE, recipPrivateKey, spec );
        byte[] sunBytes = rsa.doFinal( ciphertextBytes );

        if (useCCJ) {
            rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", "WF");
            rsa.init(Cipher.DECRYPT_MODE, recipPrivateKey, spec);
            byte[] ccjBytes = rsa.doFinal(ciphertextBytes);

            //Ensure both SunJCE and CCJ can decrypt
            assertArrayEquals(sunBytes, ccjBytes);
        }
        return sunBytes;
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha256Mgf1Sha1() throws Exception {
        runOaepWithSha256Mgf1Sha1Test( false );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha256Mgf1Sha1_withCCJ() throws Exception {
        runOaepWithSha256Mgf1Sha1Test( true );
    }

    private void runOaepWithSha256Mgf1Sha1Test( boolean useCCJ ) throws Exception {
        setCCJ( useCCJ );

        XmlElementEncryptionConfig rawConfig = new XmlElementEncryptionConfig();
        rawConfig.setRecipientCertificateBase64(recipb64);
        final XmlElementEncryptionResolvedConfig config =
                new XmlElementEncryptionResolvedConfig(CertUtils.decodeFromPEM(recipb64, false), XencUtil.AES_128_CBC, false);

        XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(config);
        final Document doc = XmlUtil.parse(TEST_XML);
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc, true, SupportedDigestMethods.SHA256, null);
        elementEncryptor.encryptAndReplaceElement(doc.getDocumentElement(), keyPair);
        String encryptedXml = XmlUtil.nodeToString(doc);
        assertTrue("encrypted data must be present", encryptedXml.contains("EncryptedData"));
        assertTrue("plaintext must be gone", !encryptedXml.contains("somepassword"));
        assertTrue("RSA 1.5 must not be used", !encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
        assertTrue("OAEP must be used", encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertTrue("dsig:DigestMethod for SHA-256 element must be present beneath OAEP xenc:EncryptionMethod",
                encryptedXml.contains( "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\"><dsig:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256" ));

        // Now try and unwrap and decrypt it
        Document doc2 = XmlUtil.parse( encryptedXml );
        XmlElementDecryptor.unwrapDecryptAndReplaceElement( doc2.getDocumentElement(),
                new SimpleSecurityTokenResolver( recipCert, recipPrivateKey ),
                null,
                null );
        String decryptedXml = XmlUtil.nodeToString( doc2 );
        assertTrue("Shall have been decrypted OK from just output XML and recipient private key",
                decryptedXml.contains( "password>somepassword" ) );

        // Make sure decryption uses actual OAEP with SHA-256 params
        OAEPParameterSpec spec = new OAEPParameterSpec( "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified( new byte[0] ) );
        byte[] decryptedKey = unwrapKeyWithOaep( XmlUtil.stringAsDocument( encryptedXml ).getDocumentElement(), spec, useCCJ );
        assertArrayEquals( keyPair.right.getEncoded(), decryptedKey );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha384Mgf1Sha1() throws Exception {
        runOaepWithSha384Mgf1Sha1Test( false );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha384Mgf1Sha1_withCCJ() throws Exception {
        runOaepWithSha384Mgf1Sha1Test( true );
    }

    private void runOaepWithSha384Mgf1Sha1Test( boolean useCCJ ) throws Exception {
        setCCJ( useCCJ );

        XmlElementEncryptionConfig rawConfig = new XmlElementEncryptionConfig();
        rawConfig.setRecipientCertificateBase64(recipb64);
        final XmlElementEncryptionResolvedConfig config =
                new XmlElementEncryptionResolvedConfig(CertUtils.decodeFromPEM(recipb64, false), XencUtil.AES_128_CBC, false);

        XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(config);
        final Document doc = XmlUtil.parse(TEST_XML);
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc, true, SupportedDigestMethods.SHA384, null);
        elementEncryptor.encryptAndReplaceElement(doc.getDocumentElement(), keyPair);
        String encryptedXml = XmlUtil.nodeToString(doc);
        assertTrue("encrypted data must be present", encryptedXml.contains("EncryptedData"));
        assertTrue("plaintext must be gone", !encryptedXml.contains("somepassword"));
        assertTrue("RSA 1.5 must not be used", !encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
        assertTrue("OAEP must be used", encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertTrue( "dsig:DigestMethod for SHA-384 element must be present beneath OAEP xenc:EncryptionMethod",
                encryptedXml.contains( "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\"><dsig:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#sha384" ));

        // Now try and unwrap and decrypt it
        Document doc2 = XmlUtil.parse( encryptedXml );
        XmlElementDecryptor.unwrapDecryptAndReplaceElement( doc2.getDocumentElement(),
                new SimpleSecurityTokenResolver( recipCert, recipPrivateKey ),
                null,
                null );
        String decryptedXml = XmlUtil.nodeToString( doc2 );
        assertTrue("Shall have been decrypted OK from just output XML and recipient private key",
                decryptedXml.contains( "password>somepassword" ) );

        // Make sure decryption uses actual OAEP with SHA-384 params
        OAEPParameterSpec spec = new OAEPParameterSpec( "SHA-384", "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified( new byte[0] ) );
        byte[] decryptedKey = unwrapKeyWithOaep( XmlUtil.stringAsDocument( encryptedXml ).getDocumentElement(), spec, useCCJ );
        assertArrayEquals( keyPair.right.getEncoded(), decryptedKey );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha256Mgf1Sha1_defaultViaSystemProperty() throws Exception {
        runOaepWithSha256Mgf1Sha1_defaultViaSystemPropertyTest( false );
    }

    @Test
    @BugId("SSG-12386")
    public void testOaepWithSha256Mgf1Sha1_defaultViaSystemProperty_withCCJ() throws Exception {
        runOaepWithSha256Mgf1Sha1_defaultViaSystemPropertyTest( true );
    }

    private void runOaepWithSha256Mgf1Sha1_defaultViaSystemPropertyTest( boolean useCCJ ) throws Exception {
        setCCJ( useCCJ );

        // Make sure encryption of XML using OAEP uses SHA-256 by default (note: not currently known to be necessary for security, SHA-1 seems to be fine for this application, even without collision resistance)
        SyspropUtil.setProperty( XmlElementEncryptor.PROP_OAEP_DIGEST_SHA256, "true" );

        XmlElementEncryptionConfig rawConfig = new XmlElementEncryptionConfig();
        rawConfig.setRecipientCertificateBase64(recipb64);
        final XmlElementEncryptionResolvedConfig config =
                new XmlElementEncryptionResolvedConfig(CertUtils.decodeFromPEM(recipb64, false), XencUtil.AES_128_CBC, false);

        XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(config);
        final Document doc = XmlUtil.parse(TEST_XML);
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc, true, null, null);
        elementEncryptor.encryptAndReplaceElement(doc.getDocumentElement(), keyPair);
        String encryptedXml = XmlUtil.nodeToString(doc);
        assertTrue("encrypted data must be present", encryptedXml.contains("EncryptedData"));
        assertTrue("plaintext must be gone", !encryptedXml.contains("somepassword"));
        assertTrue("RSA 1.5 must not be used", !encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
        assertTrue("OAEP must be used", encryptedXml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertTrue("dsig:DigestMethod for SHA-256 element must be present beneath OAEP xenc:EncryptionMethod",
                encryptedXml.contains( "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\"><dsig:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256" ));

        // Now try and unwrap and decrypt it
        Document doc2 = XmlUtil.parse( encryptedXml );
        XmlElementDecryptor.unwrapDecryptAndReplaceElement( doc2.getDocumentElement(),
                new SimpleSecurityTokenResolver( recipCert, recipPrivateKey ),
                null,
                null );
        String decryptedXml = XmlUtil.nodeToString( doc2 );
        assertTrue("Shall have been decrypted OK from just output XML and recipient private key",
                decryptedXml.contains( "password>somepassword" ) );

        // Make sure decryption uses actual OAEP with SHA-256 params
        OAEPParameterSpec spec = new OAEPParameterSpec( "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified( new byte[0] ) );
        byte[] decryptedKey = unwrapKeyWithOaep( XmlUtil.stringAsDocument( encryptedXml ).getDocumentElement(), spec, useCCJ );
        assertArrayEquals( keyPair.right.getEncoded(), decryptedKey );
    }
}
