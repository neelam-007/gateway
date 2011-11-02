package com.l7tech.security.xml;

import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 *
 */
public class XencGcmTest {
    private static final Random random = new Random(1717171L);

    @Test
    @BugNumber(11320)
    public void testEncryptGcm() throws Exception {
        Document doc = XmlUtil.parse("<foo><bar/></foo>");
        org.w3c.dom.Element barEl = XmlUtil.findExactlyOneChildElement(doc.getDocumentElement());

        byte[] secretKey = new byte[16];
        random.nextBytes(secretKey);

        XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(XencUtil.AES_128_GCM, secretKey);
        Element encryptedEl = XencUtil.encryptElement(barEl, encKey, false);
        assertTrue(XmlUtil.nodeToString(encryptedEl).contains("<CipherValue>"));

        System.out.println("Secret key: " + HexUtils.hexDump(secretKey));
        System.out.println("Encrypted XML: " + XmlUtil.nodeToString(doc, false));
    }

    @Test
    @BugNumber(11320)
    public void testDecryptGcm() throws Exception {
        FlexKey flexKey = new FlexKey(HexUtils.unHexDump("e65f2b0f06f70e4e3d35dd52aecffa19"));
        Document doc = XmlUtil.parse("<foo><EncryptedData Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\"><EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\"/><CipherData><CipherValue>+LxnyrMYfYVu+2miRsZcG7d2bLiuLB7zyHKKnpOWEWFjPMLEGBLf</CipherValue></CipherData></EncryptedData></foo>");
        Element encryptedDataEl = XmlUtil.findExactlyOneChildElement(doc.getDocumentElement());

        final DecryptionContext dc = new DecryptionContext();
        final List<String> algorithm = new ArrayList<String>();
        AlgorithmFactoryExtn af = new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, algorithm);
        Provider symmetricProvider = JceProvider.getInstance().getProviderFor("Cipher.AES");
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataEl, EncryptedData.ELEMENT, null, null);

        XencUtil.decryptAndReplaceUsingKey(encryptedDataEl, flexKey, dc, new Functions.UnaryVoid<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });

        assertEquals("<foo><bar/></foo>", XmlUtil.nodeToString(doc, false));
    }

    @Test
    @BugNumber(11320)
    public void testDecryptGcmModifiedCiphertext() throws Exception {
        FlexKey flexKey = new FlexKey(HexUtils.unHexDump("e65f2b0f06f70e4e3d35dd52aecffa19"));
        Document doc = XmlUtil.parse("<foo><EncryptedData Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\"><EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\"/><CipherData><CipherValue>+LxnyrMYfYVu+2miRsZcG7d2bLiuLB7zyHKKnpOWEWFjPMLEGBLg</CipherValue></CipherData></EncryptedData></foo>");
        Element encryptedDataEl = XmlUtil.findExactlyOneChildElement(doc.getDocumentElement());

        final DecryptionContext dc = new DecryptionContext();
        final List<String> algorithm = new ArrayList<String>();
        AlgorithmFactoryExtn af = new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, algorithm);
        Provider symmetricProvider = JceProvider.getInstance().getProviderFor("Cipher.AES");
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataEl, EncryptedData.ELEMENT, null, null);

        try {
            XencUtil.decryptAndReplaceUsingKey(encryptedDataEl, flexKey, dc, new Functions.UnaryVoid<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
            fail("expected exception not thrown");
        } catch (Throwable t) {
            if (!t.getMessage().contains("GCM Authentication failed")) {
                t.printStackTrace(System.err);
                fail("expected exception not thrown: saw this instead: " + t.getMessage());
            }
        }
    }

}
