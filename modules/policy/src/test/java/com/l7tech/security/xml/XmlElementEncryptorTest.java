package com.l7tech.security.xml;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlElementEncryptorTest {
    private static final Logger logger = Logger.getLogger(XmlElementEncryptorTest.class.getName());

    static String recipb64;
    static X509Certificate recipCert;
    static PrivateKey recipPrivateKey;

    private static final String TEST_XML =
            "<par:GetNoaParties xmlns:par=\"urn:noapar\">\n" +
            "  <par:username>brian</par:username> \n" +
            "  <par:password>somepassword</par:password> \n" +
            "  <par:notice_id>12345</par:notice_id> \n" +
            "</par:GetNoaParties>";

    @Before
    public void setUpCert() throws Exception {
        Pair<X509Certificate, PrivateKey> got = TestKeys.getCertAndKey("RSA_1024");
        recipCert = got.left;
        recipCert.checkValidity();
        recipPrivateKey = got.right;
        recipb64 = HexUtils.encodeBase64(recipCert.getEncoded());
        logger.info("Recipient certificate PKCS#12 keystore: \n" + TestCertificateGenerator.convertToBase64Pkcs12(got.left, got.right));
    }

    @BugNumber(11697)
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
        final Pair<Element, SecretKey> keyPair = elementEncryptor.createEncryptedKey(doc);
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
}
