package com.l7tech.skunkworks.crypto;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * ECC Ws-Security perf test.
 */
public class TestWssEcSigningPerformance {
    private static final Random random = new Random(442342L);
    private static Pair<X509Certificate, PrivateKey> k;

    public static void main(String[] args) throws Exception {
        //System.setProperty("com.l7tech.common.security.jceProviderEngine", "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine");
        System.out.println("Using JceProvider: " + JceProvider.getInstance().getDisplayName());

        k = TestKeys.getCertAndKey("RSA_2048");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    doTest();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        new BenchmarkRunner(r, 1, 1, "Sign and verify message").run();
    }

    private static void doTest() throws Exception {
        byte[] signedBytes = getDecoratedRequest();

        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(signedBytes),0);

        WssProcessor processor = new WssProcessorImpl();
//        Document doc = mess.getXmlKnob().getDocumentWritable();
//        SoapUtil.getPayloadElement(doc).appendChild(doc.createTextNode("broke your sig"));
        ProcessorResult dr = processor.undecorateMessage(mess, null, null);
        assertTrue(dr.getElementsThatWereSigned().length > 0);
    }

    private static byte[] getDecoratedRequest() throws Exception {
        Message request = null;
        InputStream is = null;
        try {
        Document d = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        SoapUtil.getPayloadElement(d).appendChild(d.createTextNode("Test data: " + random.nextLong() ));
        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderMessageSigningCertificate(k.left);
        dreq.setSenderMessageSigningPrivateKey(k.right);
        dreq.getElementsToSign().add(SoapUtil.getPayloadElement(d));
        request = new Message(d,0);
        decorator.decorateMessage(request, dreq);
        is = request.getMimeKnob().getEntireMessageBodyAsInputStream();
        return IOUtils.slurpStream(is);
        } finally {
            ResourceUtils.closeQuietly(is);
            ResourceUtils.closeQuietly(request);
        }
    }

    
}
