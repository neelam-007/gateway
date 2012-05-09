package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import javax.crypto.BadPaddingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Check for padding oracle vulnerability in the WSS processor.
 */
public class PaddingOracleTest {
    private static final boolean log = ConfigFactory.getBooleanProperty( "com.l7tech.test.log", false );

    private static boolean SAVE_TIMING_DATA = SyspropUtil.getBoolean("com.l7tech.test.saveTimingData", false);
    private static boolean SHOW_TIMING_HISTOGRAM = SyspropUtil.getBoolean("com.l7tech.test.showTimingHistogram", false);
    private static boolean RUN_FULL_TEST = SyspropUtil.getBoolean("com.l7tech.test.runFullPaddingOracleTest", false);

    private static final String SHORT_SOAP_MSG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope\n" +
                    "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                    "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "    <soapenv:Body>" +
                    "<a>This is a long-ish body to test decryption of multiple blocks of ciphertext, since it is suspected that this will make it easier to time things.</a>" +
                    "</soapenv:Body>\n" +
                    "</soapenv:Envelope>";

    @After
    public void cleanup() {
        System.clearProperty(XencKeyBlacklist.PROP_XENC_KEY_BLACKLIST_ENABLED);
        ConfigFactory.clearCachedConfig();
    }

    @Test
    @BugNumber(9946)
    public void testPaddingAttack() throws Exception {
        System.setProperty(XencKeyBlacklist.PROP_XENC_KEY_BLACKLIST_ENABLED, "false");
        System.setProperty(XencUtil.PROP_DECRYPTION_ALWAYS_SUCCEEDS, "true");
        ConfigFactory.clearCachedConfig();

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

        Collection<Pair<Long,Integer>> times = new ArrayList<Pair<Long,Integer>>();

        final int numIterations = RUN_FULL_TEST ? 4096 : 256;

        for (int i = 0; i < numIterations; ++i) {
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
            int code;
            long beforeTime = System.nanoTime();
            long afterTime;
            try {
                numAttempts++;
                attackDoc = attemptDecryption(attackDoc);
                if (log) System.out.println("SUCCESSFUL DECRYPT: " + XmlUtil.nodeToString(attackDoc));
                numSuccesses++;
                code = 0;
            } catch (Exception e) {
                @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"}) Throwable root = ExceptionUtils.unnestToRoot(e);
                boolean badPadding = root instanceof BadPaddingException;
                boolean malformedByteSequence = root instanceof org.apache.xerces.impl.io.MalformedByteSequenceException;
                boolean saxParseException = root instanceof SAXParseException;

                if ("Error decrypting".equals(e.getMessage())) {
                    numGeneric++;

                    if (badPadding) {
                        code = 1;
                        numBadPadding++;
                    } else if (malformedByteSequence) {
                        code = 2;
                        numMalformedByteSequence++;
                    } else if (saxParseException) {
                        code = 3;
                        numSaxParse++;
                    } else {
                        code = 7;
                        System.out.println("New generic exception: " + ExceptionUtils.getMessage(e));
                        e.printStackTrace(System.out);
                    }

                } else {
                    numOther++;

                    if (malformedByteSequence) {
                        code = 4;
                        e.printStackTrace(System.err);
                        fail("Saw a non-Generic MalformedByteSequenceException");
                    } else if (saxParseException) {
                        code = 5;
                        e.printStackTrace(System.err);
                        fail("Saw a non-Generic SAXParseException");
                    } else {
                        code = 6;
                        System.out.println("New non-generic exception: " + ExceptionUtils.getMessage(e));
                        e.printStackTrace(System.out);
                        fail("Saw unknown non-Generic exception");
                    }
                }
            } finally {
                afterTime = System.nanoTime();
            }

            long nanos = afterTime - beforeTime;
            if (i > 10) // Ignore first couple of rounds, due to JIT warmup
                times.add(new Pair<Long, Integer>(nanos, code));
        }

        System.out.println("Attempts: " + numAttempts);
        System.out.println("\"Successful\" decryptions (of syntactically-valid gibberish): " + numSuccesses);
        System.out.println("Generic failure messages (\"Error decrypting\"): " + numGeneric);
        System.out.println("    Due to BadPaddingException: " + numBadPadding);
        System.out.println("Other weird failure messages: " + numOther);
        System.out.println("    Due to MalformedByteSequenceException (UTF-8 decoding): " + numMalformedByteSequence);
        System.out.println("    Due to SAXParseException (XML fragment parsing): " + numSaxParse);

        assertTrue("All decryption errors must be reported with the generic exception message 'Error decrypting'", 0 == numOther);

        if (SAVE_TIMING_DATA)
            saveTimingData(times);

        if (SHOW_TIMING_HISTOGRAM)
            showTimingHistogram(times);

        // To resist attack, either all attempts must result in "successful" decryption, or all attempts must result in generic "Error decrypting"
        assertTrue("Either all attempts must succeed, or all attempts must report generic failure messages", numAttempts == numSuccesses || numAttempts == numGeneric);
    }

    private void saveTimingData(Collection<Pair<Long, Integer>> times) throws IOException {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream("decryptiontimes.csv"));
            for (Pair<Long, Integer> time : times) {
                ps.printf("%d,\n", time.left);
            }
        } finally {
            ResourceUtils.closeQuietly(ps);
        }
    }

    private void showTimingHistogram(Collection<Pair<Long, Integer>> times) {
        long max = Long.MIN_VALUE;
        for (Pair<Long, Integer> time : times) if (time.left > max) max = time.left;

        int numBuckets = (int) (max / 10000L);
        long bucketSize = max / numBuckets;
        int countPerBucket[][] = new int[numBuckets + 1][20];
        int maxCount = 0;
        for (Pair<Long, Integer> time : times) {
            int bucketNum = (int)(time.left / bucketSize);
            countPerBucket[bucketNum][time.right]++;
            int count = ++countPerBucket[bucketNum][19];
            if (count > maxCount) maxCount = count;
        }

        char[] symbol = new char[] { '@','*','O','+','=','#','X','%' };

        System.out.println("Each row represents an additional " + bucketSize + " nanoseconds");
        for (int row = 0; row < countPerBucket.length; row++) {
            System.out.printf("%12d:", (bucketSize * row));
            System.out.printf(" %9d ", countPerBucket[row][19]);
            for (int j = 0; j < 10; ++j) {
                int count = countPerBucket[row][j];
                for (int k = 0; k < count; ++k) {
                    System.out.print(symbol[j]);
                }
            }
            System.out.println();
        }
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
