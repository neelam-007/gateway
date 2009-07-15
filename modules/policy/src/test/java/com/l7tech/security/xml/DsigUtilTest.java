package com.l7tech.security.xml;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.SignatureException;

/**
 *
 */
public class DsigUtilTest {
    @Test
    @BugNumber(7526)
    public void testHmacOutputLength_notpresent() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"urn:blah\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "       <z:HMACOutputLengths>1</z:HMACOutputLengths>\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "</foo>"));
    }

    @Test(expected = SignatureException.class)
    @BugNumber(7526)
    public void testHmacOutputLength_present() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"urn:blah\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "       <z:HMACOutputLength>1</z:HMACOutputLength>\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "</foo>"));
    }

    @Test
    @BugNumber(7526)
    public void testHmacOutputLength_outside() throws Exception {
        DsigUtil.precheckSigElement(getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"urn:blah\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "<z:HMACOutputLength>1</z:HMACOutputLength>\n" +
                "</foo>"));
    }

    @Ignore("Enable to ensure that dsig prechecks have negligible impact on performance")
    @Test
    public void testCheckPerformance() throws Exception {
        final Element sigEl = getSigElement(
                "<foo xmlns:z=\"urn:qwer\">\n" +
                "<x:Signature xmlns:x=\"urn:blah\">\n" +
                "   <y:blorg xmlns:y=\"urn:asdf\">\n" +
                "       <z:HMACOutputLengths>1</z:HMACOutputLengths>\n" +
                "   </y:blorg>\n" +
                "</x:Signature>\n" +
                "</foo>");
        // Ensure DOM is fully parsed
        XmlUtil.nodeToOutputStream(sigEl.getOwnerDocument(), new NullOutputStream());

        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    DsigUtil.precheckSigElement(sigEl);
                } catch (SignatureException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 1000000, 10, "Dsig prechecks").run();
    }

    private static Element getSigElement(String testDoc) {
        Document doc = XmlUtil.stringAsDocument(
                testDoc);

        Element sig = XmlUtil.findFirstChildElement(doc.getDocumentElement());
        assertEquals("Signature", sig.getLocalName());
        return sig;
    }

}
