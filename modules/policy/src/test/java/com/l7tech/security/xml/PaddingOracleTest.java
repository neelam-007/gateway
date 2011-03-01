package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import javax.crypto.BadPaddingException;

import static org.junit.Assert.assertTrue;

/**
 * Check for padding oracle vulnerability in the WSS processor.
 */
public class PaddingOracleTest {
    private static final boolean log = SyspropUtil.getBoolean("com.l7tech.test.log", false);

    private static final String SHORT_SOAP_MSG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope\n" +
                    "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                    "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "    <soapenv:Body>" +
                    "<a/>" +
                    "</soapenv:Body>\n" +
                    "</soapenv:Envelope>";

    @Test
    @BugNumber(9964)
    @Ignore("Currently failing")
    public void testPaddingAttack() throws Exception {
        // Decorate a test message
        final Document doc = XmlUtil.stringAsDocument(SHORT_SOAP_MSG);
        encryptMessageForTarget(doc);
        if (log) System.out.println("Decorated: " + XmlUtil.nodeToFormattedString(doc));
        assertTrue("Must be encrypted", doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "EncryptedData").getLength() > 0);

        // Save the data reference ID
        Element origDataRefEl = (Element)doc.getElementsByTagNameNS(SoapUtil.XMLENC_NS, "DataReference").item(0);
        String origDataRefVal = origDataRefEl.getAttribute("URI");

        // Save the encrypted message
        String encryptedDoc = XmlUtil.nodeToString(doc);

        // Now try to attack it.
        // Let's keep replacing the encrypted key with a new one and see if the wss processor reveals when a bad padding error does NOT occur
        int numAttempts = 0;
        int numSuccesses = 0;
        int numGeneric = 0;
        int numBadPadding = 0;
        int numOther = 0;
        int numMalformedByteSequence = 0;
        int numSaxParse = 0;

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
                attackDoc = attemptDecryption(attackDoc);
                if (log) System.out.println("SUCCESSFUL DECRYPT: " + XmlUtil.nodeToString(attackDoc));
                numSuccesses++;
            } catch (Exception e) {
                Throwable root = ExceptionUtils.unnestToRoot(e);
                if ("Error decrypting".equals(e.getMessage())) {
                    numGeneric++;

                    boolean badPadding = root instanceof BadPaddingException;

                    if (badPadding) {
                        numBadPadding++;
                    } else {
                        System.out.println("New generic exception: " + ExceptionUtils.getMessage(e));
                        e.printStackTrace(System.out);
                    }

                } else {
                    numOther++;

                    boolean malformedByteSequence = root instanceof org.apache.xerces.impl.io.MalformedByteSequenceException;
                    boolean saxParseException = root instanceof SAXParseException;

                    if (malformedByteSequence) {
                        numMalformedByteSequence++;
                    } else if (saxParseException) {
                        numSaxParse++;
                    } else {
                        System.out.println("New non-generic exception: " + ExceptionUtils.getMessage(e));
                        e.printStackTrace(System.out);
                    }
                }
            }
        }

        System.out.println("Attempts: " + numAttempts);
        System.out.println("\"Successful\" decryptions (of syntactically-valid gibberish): " + numSuccesses);
        System.out.println("Generic failure messages (\"Error decrypting\"): " + numGeneric);
        System.out.println("    Due to BadPaddingException: " + numBadPadding);
        System.out.println("Other weird failure messages: " + numOther);
        System.out.println("    Due to MalformedByteSequenceException (UTF-8 decoding): " + numMalformedByteSequence);
        System.out.println("    Due to SAXParseException (XML fragment parsing): " + numSaxParse);

        // To resist attack, either all attempts must result in "successful" decryption, or all attempts must result in generic "Error decrypting"
        assertTrue("Either all attempts must succeed, or all attempts must report generic failure messages",
                numAttempts == numSuccesses || numAttempts == numGeneric);
    }

    private Document attemptDecryption(Document attackDoc) throws Exception {
        final Message mess = new Message(attackDoc);
        WssProcessorImpl processor = new WssProcessorImpl(mess);
        processor.setSecurityTokenResolver(new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey()));
        processor.processMessage();
        return mess.getXmlKnob().getDocumentReadOnly();
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
