package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.xml.soap.SoapUtil;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 *
 */
public class WssRoundTripPerformanceTester {
    private static final Logger logger = Logger.getLogger(WssRoundTripPerformanceTester.class.getName());

    static SignerInfo SENDER_ECC;
    static SignerInfo SENDER_RSA;
    static SignerInfo RECIP_ECC;
    static SignerInfo RECIP_RSA;
    static String PLACEORDER_CLEARTEXT;

    @BeforeClass
    public static void createKeys() throws Exception {
        logger.info("Using crypto provider: " + JceProvider.getInstance().getDisplayName());
        new SecureRandom().nextInt(); // Ensure RNG is initialized early
        SENDER_ECC = new SignerInfo(new TestCertificateGenerator().curveName("secp384r1").generateWithKey());
        SENDER_RSA = new SignerInfo(new TestCertificateGenerator().keySize(1024).generateWithKey());
        RECIP_ECC = new SignerInfo(new TestCertificateGenerator().curveName("secp384r1").generateWithKey());
        RECIP_RSA = new SignerInfo(new TestCertificateGenerator().keySize(1024).generateWithKey());

        PLACEORDER_CLEARTEXT = XmlUtil.nodeToString(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT_ONELINE));
    }

    void benchmark(String name, final int iters, final int threads, final Callable code) throws InterruptedException {
        logger.info("Doing BURN-IN run of: "+ name);
        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    code.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, iters, threads, name).run();

        logger.info("Doing ACTUAL run of: " + name);
        new BenchmarkRunner(new Runnable() {
            @Override
            public void run() {
                try {
                    code.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, iters, threads, name).run();
    }

    @Test
    public void testDoNoDecoration() throws Exception {
        benchmark("DoNoDecoration", 100000, 20, new Callable() {
            @Override
            public Object call() throws Exception {
                Document doc = newSoapMessage();
                SoapUtil.getBodyElement(doc);
                return null;
            }
        });
    }

    @Test
    public void testSigningPerformanceRsa() throws Exception {
        benchmark("SigningPerformanceRsa", 10000, 10, new Callable() {
            @Override
            public Object call() throws Exception {
                Document doc = newSoapMessage();
                WssDecoratorImpl decorator = new WssDecoratorImpl();
                DecorationRequirements dr = new DecorationRequirements();
                dr.setSenderMessageSigningCertificate(SENDER_RSA.getCertificate());
                dr.setSenderMessageSigningPrivateKey(SENDER_RSA.getPrivate());
                dr.setSignTimestamp();
                dr.getElementsToSign().add(SoapUtil.getBodyElement(doc));
                Message mess = new Message(doc);
                decorator.decorateMessage(mess, dr);
                return mess;
            }
        });
    }

    @Test
    public void testVerifyPerformanceRsa() throws Exception {
        final String signedMess = toString(newSignedRequest(SENDER_RSA));

        benchmark("VerifyPerformanceRsa", 10000, 10, new Callable() {
            @Override
            public Object call() throws Exception {
                WssProcessorImpl processor = new WssProcessorImpl(new Message(XmlUtil.stringToDocument(signedMess)));
                ProcessorResult pr = processor.processMessage();
                SignedElement[] signed = pr.getElementsThatWereSigned();
                assertTrue(signed.length >= 2);
                return signed;
            }
        });
    }

    @Test
    public void testEncryptionPerformanceRsa() throws Exception {
        benchmark("EncryptionPerformanceRsa", 10000, 10, new Callable() {
            @Override
            public Object call() throws Exception {
                Document doc = newSoapMessage();
                WssDecoratorImpl decorator = new WssDecoratorImpl();
                DecorationRequirements dr = new DecorationRequirements();
                dr.setRecipientCertificate(RECIP_RSA.getCertificate());
                dr.getElementsToEncrypt().add(SoapUtil.getBodyElement(doc));
                Message mess = new Message(doc);
                decorator.decorateMessage(mess, dr);
                return mess;
            }
        });
    }

    @Test
    public void testDecryptionPerformanceRsa() throws Exception {
        final String encryptedMess = toString(newEncryptedRequest(RECIP_RSA));
        final SimpleSecurityTokenResolver tokenResolver = new SimpleSecurityTokenResolver(null, new SignerInfo[]{RECIP_RSA});

        benchmark("DecryptionPerformanceRsa", 10000, 10, new Callable() {
            @Override
            public Object call() throws Exception {
                WssProcessorImpl processor = new WssProcessorImpl(new Message(XmlUtil.stringToDocument(encryptedMess)));
                processor.setSecurityTokenResolver(tokenResolver);
                ProcessorResult pr = processor.processMessage();
                EncryptedElement[] encrypted = pr.getElementsThatWereEncrypted();
                assertTrue(encrypted.length > 0);
                return encrypted;
            }
        });
    }

    Document newSoapMessage() throws IOException, SAXException {
        return XmlUtil.stringToDocument(PLACEORDER_CLEARTEXT);
    }

    String toString(Message msg) throws IOException, SAXException {
        return XmlUtil.nodeToString(msg.getXmlKnob().getDocumentReadOnly());
    }

    Message newEncryptedRequest(SignerInfo recipient) throws Exception {
        Document doc = newSoapMessage();
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        DecorationRequirements dr = new DecorationRequirements();
        dr.setRecipientCertificate(recipient.getCertificate());
        dr.getElementsToEncrypt().add(SoapUtil.getBodyElement(doc));
        Message mess = new Message(doc);
        decorator.decorateMessage(mess, dr);
        return mess;
    }

    Message newSignedRequest(SignerInfo sender) throws Exception {
        Document doc = newSoapMessage();
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        DecorationRequirements dr = new DecorationRequirements();
        dr.setSenderMessageSigningCertificate(sender.getCertificate());
        dr.setSenderMessageSigningPrivateKey(sender.getPrivate());
        dr.setSignTimestamp();
        dr.getElementsToSign().add(SoapUtil.getBodyElement(doc));
        Message mess = new Message(doc);
        decorator.decorateMessage(mess, dr);
        return mess;
    }

}
