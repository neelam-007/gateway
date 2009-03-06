package com.l7tech.common.io;

import com.l7tech.common.TestDocuments;
import com.l7tech.test.BenchmarkRunner;
import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class XmlUtilSerializerBenchmark {
    @Test
    public void testXss4JPerformance() throws Exception {
        testSerializer(new Ser() {
            public void serialize(Document doc, OutputStream os) throws IOException {
                XmlUtil.nodeToOutputStreamWithXss4j(doc, os);
            }
        });
    }

    @Test
    public void testXMLSerializerPerformance() throws Exception {
        testSerializer(new Ser() {
            public void serialize(Document doc, OutputStream os) throws IOException {
                XmlUtil.nodeToOutputStreamWithXMLSerializer(doc, os);
            }
        });
    }

    private interface Ser {
        void serialize(Document doc, OutputStream os) throws Exception;
    }

    private static String doser(Ser ser, Document doc) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ser.serialize(doc, baos);
            return baos.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testSerializer(final Ser ser) throws Exception {
        final AtomicLong count = new AtomicLong(0);

        new BenchmarkRunner(new Runnable() {
            public void run() {
                final Document doc;
                try {
                    doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_WITH_MAJESTY);
                    for (int i = 0; i < 1000; ++i)
                        count.addAndGet(doser(ser, doc).length());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 100, ser.getClass().getSimpleName()).run();
        System.out.println("Serialized " + count.get() + " characters total");
    }
}
