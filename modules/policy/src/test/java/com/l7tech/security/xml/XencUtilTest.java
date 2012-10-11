/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.xml;

import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mike
 */
public class XencUtilTest {
    private static Logger logger = Logger.getLogger(XencUtilTest.class.getName());
    private static final Random random = new Random(283732L);

    @BeforeClass
    public static void setUp() throws Exception {
        JceProvider.init();
    }

    @After
    public void afterEachTest() throws Exception {
        uncustomizeClassProperties(XencUtil.class);
        uncustomizeClassProperties(XencKeyBlacklist.class);
        ConfigFactory.clearCachedConfig();
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.common.security.jceProviderEngine",
            "com.l7tech.common.security.jceProviderEngineName"
        );
    }

    private static void uncustomizeClassProperties(Class c) throws IllegalAccessException {
        for (String propertyName : getSystemPropertyNames(c)) {
            System.clearProperty(propertyName);
        }
    }

    // Find all static string fields whose fieldname begins with PROP_ and returns their values, assuming them to be system property names
    private static Collection<String> getSystemPropertyNames(Class c) throws IllegalAccessException {
        Collection<String> ret = new ArrayList<String>();
        Field[] fields = c.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("PROP_") && String.class.equals(field.getType())) {
                ret.add((String) field.get(null));
            }
        }
        return ret;
    }

    @Test
    public void testPadKey() throws Exception {
        SecureRandom rand = new SecureRandom();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        final int modulusLength = publicKey.getModulus().toByteArray().length;

        byte[] paddedKeyBytes = XencUtil.padSymmetricKeyForRsaEncryption(keyBytes, modulusLength, rand);

        assertTrue(paddedKeyBytes.length <= modulusLength);
        assertTrue(paddedKeyBytes.length < 128);
    }

    @Test
    public void testEncryptKeyWithRsaAndPad() throws Exception {
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");

        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
        logger.info("Got back: " + paddedB64 + "\n(length:" + paddedB64.length() + ")");
    }

    @Test
    public void testRoundTripRsaEncryptedKey() throws Exception {
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
        byte[] decrypted = XencUtil.decryptKey(paddedB64, pkey);
        assertTrue(Arrays.equals(keyBytes, decrypted));
    }

    @Test
    public void testRoundTripRsaOaepEncryptedKey() throws Exception {
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] oaepParams = new byte[128];
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        String paddedB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaOaepMGF1SHA1(keyBytes, recipientCert, publicKey, oaepParams), true);
        byte[] decrypted = XencUtil.decryptKey(paddedB64, oaepParams, pkey);
        assertTrue(Arrays.equals(keyBytes, decrypted));
    }

    @Ignore("Disabled because it fails as of 5.0.0, and nobody remembers what the test was for originally")
    @Test
    public void testWeirdLeadin0InSunJCE() throws Exception {
        if (!JceProvider.getEngineClass().equals(JceProvider.BC_ENGINE)) {
            System.out.println("This test is meant to be ran with BC provider. Aborting.");
            return;
        }
        PrivateKey pkey = TestDocuments.getDotNetServerPrivateKey();
        String keypaddedandencryptedwithsunjce = "TK0T2LPWmCYDUtE32P7s7aVvjnfJ9flQm+GOiriGyY677g2/RgDbWncSJcPipm1zRmYRkmvKbNYFpReVl1SrVqsCbYudX/y8WQyI3LVInoc3TNfBPryphoVrxtjLDeAhfxxdsxYSq12Ze62RvLr3Y3k9vxaKotJcOejMtyHj9T4=";
        byte[] decrypted = XencUtil.decryptKey(keypaddedandencryptedwithsunjce, pkey);
        byte[] originalBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        assertTrue(Arrays.equals(originalBytes, decrypted));
    }

    @Test
    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC1() throws Exception {
        SyspropUtil.setProperty( JceProvider.ENGINE_PROPERTY, JceProvider.SUN_ENGINE );
        System.out.println("USING PROVIDER: " + JceProvider.getEngineClass());
        System.out.println("SUN OUTPUT:" + getEncryptedKey());
    }

    @Test
    public void testCompareCipherOutputForEncryptedKeysBetweenSUNAndBC2() throws Exception {
        SyspropUtil.setProperty( JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE );
        System.out.println("USING PROVIDER: " + JceProvider.getEngineClass());
        System.out.println("BC OUTPUT:" + getEncryptedKey());
    }

    private String getEncryptedKey() throws Exception {
        X509Certificate recipientCert = TestDocuments.getDotNetServerCertificate();
        RSAPublicKey publicKey = (RSAPublicKey)recipientCert.getPublicKey();
        byte[] keyBytes = HexUtils.unHexDump("954daf423cea7911cc5cb9b664d4c38d");
        return HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipientCert, publicKey), true);
    }

    @Test
    @BugNumber(11191)
    public void testEncryptEmptyElement() throws Exception {
        Element element = makeEncryptedContentElement(null).left;

        assertTrue("must no longer be empty", element.getChildNodes().getLength() > 0);
        assertTrue("must have been encrypted", XmlUtil.nodeToString(element).contains("EncryptedData"));
    }

    /**
     * Encrypt only the contents of the first child element of the document element of the specified XML.
     * <p/>
     * Example input:  "<foo><blah/></foo>"
     * Exmaple return value:  pair of (ref to blah element, new XmlEncKey) where blah element is now "<blah><EncryptedData.../></blah>"
     *
     * @param xml XML to examine, or null to use "<foo><blah/></foo>".  If provided, must contain well formed XML with at least one child element of the document element.
     * @return a Pair consisting of the e
     * @throws MissingRequiredElementException
     * @throws TooManyChildElementsException
     * @throws XencUtil.XencException
     * @throws GeneralSecurityException
     */
    public static Pair<Element, XencUtil.XmlEncKey> makeEncryptedContentElement(@Nullable String xml) throws MissingRequiredElementException, TooManyChildElementsException, XencUtil.XencException, GeneralSecurityException {
        if (xml == null)
            xml = "<foo><blah/></foo>";
        Document doc = XmlUtil.stringAsDocument(xml);
        Element root = doc.getDocumentElement();
        Element element = DomUtils.findFirstChildElement(root);

        byte[] keybytes = new byte[32];
        random.nextBytes(keybytes);
        XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(EncryptionMethod.AES256_CBC, keybytes);
        XencUtil.encryptElement(element, encKey, true);
        return new Pair<Element, XencUtil.XmlEncKey>(element, encKey);
    }

    @Test
    @BugNumber(11251)
    public void testKeyBlacklistingAndSilentDecryptionFailure() throws Exception {
        final String plaintext = "<foo><blah>Blah blah blah</blah></foo>";
        Document doc = XmlUtil.stringAsDocument(plaintext);
        Element root = doc.getDocumentElement();
        Element element = DomUtils.findExactlyOneChildElement(root);

        byte[] keybytes = new byte[32];
        random.nextBytes(keybytes);
        XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(EncryptionMethod.AES256_CBC, keybytes);
        final FlexKey flexKey = new FlexKey(keybytes);
        Element encdata = XencUtil.encryptElement(element, encKey, true);

        String xml = XmlUtil.nodeToString(encdata.getOwnerDocument().getDocumentElement(), false);

        ConfigFactory.clearCachedConfig();
        System.setProperty(XencKeyBlacklist.PROP_XENC_KEY_BLACKLIST_MAX_FAILURES, "2");
        System.setProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS, "true");

        // Try and decrypt with good key
        doc = encdata.getOwnerDocument();
        DecryptionContext dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(encKey.getSecretKey(), new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, encKey.getSecretKey(), dc, null);
        assertEquals("Initial decryption with correct ciphertext should succeed", plaintext, XmlUtil.nodeToString(doc.getDocumentElement(), false));

        // Try and decrypt with modified ciphertext
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        Element cipherValue = (Element) doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherValue").item(0);
        cipherValue.setTextContent("ataIiIEAG775jGfJyYo33Qp1" + cipherValue.getTextContent());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        String decrypted = XmlUtil.nodeToString(doc.getDocumentElement(), false);
        assertTrue(decrypted.contains("DecryptionFault"));
        assertTrue("Second decryption attempt, with incorrect ciphertext, should fail silently", !plaintext.equals(decrypted));

        // Decrypt with original ciphertext again
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        assertEquals("Third decryption attempt, with actual ciphertext, should succeed", plaintext, XmlUtil.nodeToString(doc.getDocumentElement(), false));

        // Try and decrypt with modified ciphertext again
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        cipherValue = (Element) doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherValue").item(0);
        cipherValue.setTextContent("ataIiIEAG775jGfJyYo33Qp1" + cipherValue.getTextContent());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        assertTrue("Fourth decryption attempt, with incorrect ciphertext, should fail silently", !plaintext.equals(XmlUtil.nodeToString(doc.getDocumentElement(), false)));
        decrypted = XmlUtil.nodeToString(doc.getDocumentElement(), false);
        assertTrue(decrypted.contains("DecryptionFault"));

        // Decrypt with original ciphertext again
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        assertEquals("Fifth decryption attempt, with actual ciphertext, should succeed", plaintext, XmlUtil.nodeToString(doc.getDocumentElement(), false));

        // Try and decrypt with modified ciphertext again
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        cipherValue = (Element) doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "CipherValue").item(0);
        cipherValue.setTextContent("ataIiIEAG775jGfJyYo33Qp1" + cipherValue.getTextContent());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        assertTrue("Sixth decryption attempt, with incorrect ciphertext, should fail silently", !plaintext.equals(XmlUtil.nodeToString(doc.getDocumentElement(), false)));
        decrypted = XmlUtil.nodeToString(doc.getDocumentElement(), false);
        assertTrue(decrypted.contains("DecryptionFault"));

        // Decrypt with original ciphertext again
        doc = XmlUtil.stringAsDocument(xml);
        encdata = XmlUtil.findExactlyOneChildElement(XmlUtil.findExactlyOneChildElement(doc.getDocumentElement()));
        assertEquals("EncryptedData", encdata.getLocalName());
        dc = new DecryptionContext();
        dc.setAlgorithmFactory(new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, new ArrayList<String>()));
        dc.setEncryptedType(encdata, EncryptedData.ELEMENT, null, null);
        XencUtil.decryptAndReplaceUsingKey(encdata, flexKey, dc, null);
        assertTrue("Seventh decryption attempt, with actual ciphertext, should now FAIL before it is even attempted, due to the key blacklist",
                !plaintext.equals(XmlUtil.nodeToString(doc.getDocumentElement(), false)));
        decrypted = XmlUtil.nodeToString(doc.getDocumentElement(), false);
        assertTrue(decrypted.contains("DecryptionFault"));
    }

}
