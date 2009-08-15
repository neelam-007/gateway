package com.l7tech.security.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.message.Message;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class WssDecryptionBenchmark {
    private static final int DISK_STASH_THRESHOLD = 1048576;
    private static final AtomicLong uniqueLong = new AtomicLong();
    private static X509Certificate clientCert;
    private static PrivateKey clientKey;
    private static X509Certificate serverCert;
    private static PrivateKey serverKey;
    private static String decoratedXml;

    @BeforeClass
    public static void setUp() throws Exception {
        Pair<X509Certificate, PrivateKey> c = new TestCertificateGenerator().generateWithKey();
        clientCert = c.left;
        clientKey = c.right;

        Pair<X509Certificate,PrivateKey> s = new TestCertificateGenerator().generateWithKey();
        serverCert = s.left;
        serverKey = s.right;

        Element body = SoapUtil.createSoapEnvelopeAndGetBody(SoapVersion.SOAP_1_1);
        Element foo = XmlUtil.createAndAppendElementNS(body, "foo", "urn:foons", "f");
        for (int i = 0; i < 512; ++i) {
            Element part = XmlUtil.createAndAppendElementNS(foo, "part", foo.getNamespaceURI(), foo.getPrefix());
            part.appendChild(XmlUtil.createTextNode(part, kilobyte()));
        }

        WssDecoratorImpl decorator = new WssDecoratorImpl();
        Message message = new Message(body.getOwnerDocument());
        DecorationRequirements req = new DecorationRequirements();
        req.setSenderMessageSigningCertificate(clientCert);
        req.setSenderMessageSigningPrivateKey(clientKey);
        req.setRecipientCertificate(serverCert);
        req.setIncludeTimestamp(true);
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.getElementsToEncrypt().add(body);
        decorator.decorateMessage(message, req);

        decoratedXml = XmlUtil.nodeToString(message.getXmlKnob().getDocumentReadOnly());
        //assertTrue(decoratedXml.length() > DISK_STASH_THRESHOLD * 1.2 && decoratedXml.length() < DISK_STASH_THRESHOLD * 2);
        assertNotNull(XmlUtil.stringToDocument(decoratedXml));
    }

    @Test
    public void testLargeEncryptedRequest() throws Exception {
        Runnable processor = new Runnable() {
            @Override
            public void run() {
                HybridStashManager stashManager = null;
                Message req = null;
                try {
                    stashManager = new HybridStashManager(DISK_STASH_THRESHOLD, new File("."), "decryptionBenchmark-" + uniqueLong.incrementAndGet());
                    req = new Message(stashManager, ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(decoratedXml.getBytes("UTF-8")));

                    WssProcessorImpl wssProcessor = new WssProcessorImpl(req);
                    wssProcessor.setSecurityTokenResolver(new SimpleSecurityTokenResolver(serverCert, serverKey));
                    ProcessorResult result = wssProcessor.processMessage();
                    assertEquals(1, result.getElementsThatWereEncrypted().length);
                    assertEquals(2, result.getElementsThatWereSigned().length);
                    System.out.println("Finished a request");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (req != null) req.close();
                    ResourceUtils.closeQuietly(stashManager);
                }
            }
        };

        BenchmarkRunner br = new BenchmarkRunner(processor, 2000, 200, "largeEncryptedSignedRequest180Concurrent");
        br.run();
    }

    private static String kilobyte() {
        String ate = "This is 1024 bytes of plain text that will be used to bulk up the body.  Each line of it should be a convenient 128 chars long.\n";
        String kilobyte = ate + ate + ate + ate + ate + ate + ate + ate;
        assertEquals(1024, kilobyte.length());
        return kilobyte;
    }
}
