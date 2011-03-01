package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Check for padding oracle vulnerability in the WSS processor.
 */
public class PaddingOracleTest {
    @Test
    @Ignore("Currently failing")
    public void testPaddingAttack() throws Exception {
        // Decorate a test message
        final Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        encryptMessageForTarget(doc);
        System.out.println("Decorated: " + XmlUtil.nodeToFormattedString(doc));
        assertTrue("Must be encrypted", doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength() > 0);

        // Save the data reference ID
        Element origDataRefEl = (Element)doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "DataReference").item(0);
        String origDataRefVal = origDataRefEl.getAttribute("URI");


        // Save the encrypted message
        String encryptedDoc = XmlUtil.nodeToString(doc);

        // Now try to attack it.
        // Let's keep replacing the encrypted key with a new one and see if the wss processor reveals when a bad padding error does NOT occur
        int numAttempts = 0;
        int numGeneric = 0;
        int numBadPadding = 0;
        int numOther = 0;
        int numUtf = 0;

        for (int i = 0; i < 2048; ++i) {
            // Create new encrypted key
            final Document stub = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
            encryptMessageForTarget(stub);
            Element newEk = (Element)stub.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedKey").item(0);

            // Rewire the DataReference to point at the old body ID
            Element newDataRefEl = (Element)stub.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "DataReference").item(0);
            newDataRefEl.setAttribute("URI", origDataRefVal);

            // Build attack document and import new EK
            Document attackDoc = XmlUtil.stringToDocument(encryptedDoc);
            newEk = (Element)attackDoc.importNode(newEk, true);

            // Replace old encrypted key with substitute
            Element oldEk = (Element)attackDoc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedKey").item(0);
            oldEk.getParentNode().insertBefore(newEk, oldEk);
            oldEk.getParentNode().removeChild(oldEk);

            //System.out.println("Rewritten doc: " + XmlUtil.nodeToFormattedString(attackDoc));

            // Attempt to decrypt and see what happens
            try {
                numAttempts++;
                attemptDecryption(attackDoc);
                System.out.println("SUCCESSFUL DECRYPT: " + XmlUtil.nodeToString(attackDoc));
                fail("Decryption succeeded with incorrect encrypted key");
            } catch (Exception e) {
                Throwable root = ExceptionUtils.unnestToRoot(e);
                if ("Error decrypting".equals(e.getMessage())) {
                    numGeneric++;

                    boolean badPadding = root.getMessage().contains("adding");
                    if (badPadding) numBadPadding++;

                    // Ok
                } else {
                    System.out.println("Exception: " + ExceptionUtils.getMessage(e));
                    e.printStackTrace(System.out);
                    numOther++;

                    boolean badUtf = root.getMessage().contains("byte UTF-8 sequence");
                    if (badUtf) numUtf++;
                }
            }
        }

        System.out.println("Attempts: " + numAttempts);
        System.out.println("Generic failure messages (\"Error decrypting\"): " + numGeneric);
        System.out.println("   Generic failure messages that were due to bad padding: " + numBadPadding);
        System.out.println("Other weird failure messages: " + numOther);
        System.out.println("    Due to Invalid byte N of N-byte UTF-8: " + numUtf);
    }

    private void attemptDecryption(Document attackDoc) throws Exception {
        WssProcessorImpl processor = new WssProcessorImpl(new Message(attackDoc));
        processor.setSecurityTokenResolver(new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey()));
        processor.processMessage();
    }

    private static void encryptMessageForTarget(Document doc) throws Exception {
        Message req = new Message(doc);
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.addElementToEncrypt(SoapUtil.getBodyElement(doc));
        dreq.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        WssDecorator decorator = new WssDecoratorImpl();
        decorator.decorateMessage(req, dreq);
    }
}
