package com.l7tech.security.xml.processor;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.Resolver;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class EncryptedKeyImplTest {

    Element elm;
    SimpleSecurityTokenResolver tokenResolver;
    Resolver<String,X509Certificate> x509Resolver;
    boolean restricted = false;
    String eksha1;
    String secretkeybytes;

    @Before
    public void setup() throws Exception {
        Pair<X509Certificate, PrivateKey> certkey = TestKeys.getCertAndKey("RSA_1024");
        SignerInfo signerInfo = new SignerInfo(certkey) {
            @Override
            public boolean isRestrictedAccess() {
                return restricted;
            }
        };

        String xml =
                "            <xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
                "                <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "                    <wsse:SecurityTokenReference>\n" +
                "                        <wsse:KeyIdentifier\n" +
                "                            EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">3bG81B25MHuoBRi9apZWR2bVqHM=</wsse:KeyIdentifier>\n" +
                "                    </wsse:SecurityTokenReference>\n" +
                "                </dsig:KeyInfo>\n" +
                "                <xenc:CipherData>\n" +
                "                    <xenc:CipherValue>BAWGeiNZmDq9a+xK7Q3Q9P0VbqI2wQ0m8NTM/bw6lq7RamAt7TudPzlEBnVLaAk6jgm1RePCnmPay5dEmP3HFhq7nsgRyvkiWNa5A7s4+ypKVWfU7U4mkNN81PTRJrpAGd7nqXshY4KSoZw+/UxVgJbnYZFngOpcsSTg2wEZx8w=</xenc:CipherValue>\n" +
                "                </xenc:CipherData>\n" +
                "                <xenc:ReferenceList>\n" +
                "                    <xenc:DataReference URI=\"#Body-2-abcdea4322a60a13bdb9aeb03f6d9cc2\"/>\n" +
                "                </xenc:ReferenceList>\n" +
                "            </xenc:EncryptedKey>";
        Document doc = XmlUtil.stringAsDocument(xml);
        elm = (Element) doc.getElementsByTagNameNS(doc.getDocumentElement().getNamespaceURI(), "EncryptedKey").item(0);
        tokenResolver = new SimpleSecurityTokenResolver(null, new SignerInfo[] {signerInfo});
        x509Resolver = null;
        eksha1 = "YKOL7WvI07yN+KvVSVSpZD0fHAI=";
        secretkeybytes = "b810485d6717b1b35d1b37f8b806c074";
    }

    @Test
    public void testPublishUnwrappedKey() throws Exception {
        restricted = false;
        assertNull(tokenResolver.getSecretKeyByEncryptedKeySha1(eksha1));
        EncryptedKeyImpl ek = new EncryptedKeyImpl(elm, tokenResolver, x509Resolver);
        String skhex = HexUtils.hexDump(ek.getSecretKey());
        assertEquals(secretkeybytes, skhex);
        assertEquals(skhex, HexUtils.hexDump(tokenResolver.getSecretKeyByEncryptedKeySha1(eksha1)));
    }

    @Test
    public void testDoNotPublishUnwrappedRestrictedKey() throws Exception {
        restricted = true;
        assertNull(tokenResolver.getSecretKeyByEncryptedKeySha1(eksha1));
        EncryptedKeyImpl ek = new EncryptedKeyImpl(elm, tokenResolver, x509Resolver);
        String skhex = HexUtils.hexDump(ek.getSecretKey());
        assertEquals(secretkeybytes, skhex);
        assertNull(tokenResolver.getSecretKeyByEncryptedKeySha1(eksha1));
    }
}
