package com.l7tech.security.xml;

import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
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
 * Test XencUtil AES-GCM support.
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
    public void testDecryptGcmFromNcipherNcoreApi() throws Exception {
        // This test attempts to decrypt some ciphertext that was encrypted with AES-GCM (128 bit key, 12 byte IV, 128 bit auth tag)
        // by a standalone test program using nCipher's nCore API.

        FlexKey flexKey = new FlexKey(new byte[]{ -17, 71, 87, 73, 2, -3, 66, 52, 44, 90, -23, 57, -92, -10, -4, 57 });
        flexKey.setAlgorithm(FlexKey.AES128);
        Document doc = XmlUtil.parse("<foo><EncryptedData Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\"><EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\"/><CipherData><CipherValue>" +
                "G32GbjZOOMy23789" +
                "c5KS6eLexPUoII93+YRA1t2mB9Lh9AkC6Uaco2ek8KuaB5OQXMufZ5wQZpMa2yFRBygZXRPYghoJ\n" +
                "KdVwX7Ht07+ovBLqC6TUweCDVdavNsP6XIeXmqYvNOLz2QrrvFwVggUdY7igJY1aYKe8mig6NUof\n" +
                "gLkrHkf6o1jE915ZWhgST996WKQplLPk8YXDpa2dUrYq9XkMJYkB1wsbjCCh6z15Tdz97NbEhWCc\n" +
                "/xLnWAvrHTIVCVV3oAWYrDX28WnV20BT/5rAjssaW+kQ/mnJeyz7JV79thw60kxmngpdN7/uYxaq\n" +
                "jLRam7AlB5VbiPykNdFRWyLezdbBeDag6zcTbYzEQc5Q3OXCVbmYW7sOND0j9plvzXE80KbMYf/W\n" +
                "T/JYqSTxVDtjxtmFdRCIU77hMBn/2aaoRLwA/+xQ4z7GI+R06a2RqTSj3/1yXiNKuhEWezskMwts\n" +
                "3szl5n9V9SN1AFVVIIuCavStcTtkFbbbAOoS8iY7XyjPgwDDpdUoxRHIcW+IfAagarKRaALvfDSj\n" +
                "ZMbh6j3/0Jt8srfpGF67N5IuxqbOhDVKtDZsNE1ejQgOQzIX2Y1hUpgWf4VVUBgFP9s7VeS91B4V\n" +
                "aawmpw9e5G06MBYnUegSl4aserH/H76zs3WQOfdqKxdkPR5ItfGVzEc/6u6M2Cokw0SHkAW7odac" +
                "</CipherValue></CipherData></EncryptedData></foo>");

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

        assertEquals("<foo><tag>Test of some stuff to encrypt that should be decrypted OK.  Long blah blah blah more long blah.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk.\n" +
                "Extra stuff to make it longer than one channel chunk. END of stuff</tag></foo>", XmlUtil.nodeToString(doc, false));
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
                    fail("error handler should not have been invoked for failed GCM authentication (since alwaysSucceed should be ignored in GCM mode, where it isn't necessary or desirable)");
                }
            });
            fail("expected exception not thrown");
        } catch (Throwable t) {
            //noinspection ThrowableResultOfMethodCallIgnored
            final String rootMessage = ExceptionUtils.unnestToRoot(t).getMessage();

            boolean expectedMessage = false;
            for (String message : GCM_AUTH_FAILED_MESSAGES) {
                if (rootMessage.contains(message)) {
                    expectedMessage = true;
                }
            }

            if (!expectedMessage) {
                t.printStackTrace(System.err);
                fail("expected exception not thrown: saw this instead: " + t.getMessage());
            }
        }
    }

    private static final String[] GCM_AUTH_FAILED_MESSAGES = {
            // Bouncy Castle
            "mac check in GCM failed",

            // Crypto-J
            "GCM Authentication failed"
    };
}
