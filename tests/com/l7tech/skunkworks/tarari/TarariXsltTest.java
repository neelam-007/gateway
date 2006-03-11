/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks.tarari;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.util.HexUtils;
import com.l7tech.skunkworks.BenchmarkRunner;
import com.tarari.xml.XmlSource;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.XmlResult;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.xslt11.Stylesheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * Test harness for Tarari XSLT.
 */
public class TarariXsltTest {
    private static final ThreadLocal xmlSource = new ThreadLocal() {
        protected Object initialValue() {
            return new XmlSource(new EmptyInputStream());
        }
    };

    public static void main(String[] args) throws Exception {
        final String reqStr = TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/DocSearchResponse.xml");
        final String xsltStr = TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/GetDocInfoFull.xsl");

        final byte[] xsltBytes = xsltStr.getBytes();
        final byte[] requestBytes = reqStr.getBytes();
        final Stylesheet master = Stylesheet.create(new XmlSource(xsltBytes));

        // Make a test that simulates what the SSG has to do
        Runnable xsltTestSsg = new Runnable() {
            public void run() {
                try {
                    RaxDocument doc = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(requestBytes)));
                    //final XPathProcessor xpathProcessor = new XPathProcessor(doc);
                    //XPathResult xpathResult = xpathProcessor.processXPaths();
                    XmlSource source = (XmlSource)xmlSource.get();
                    source.setData(doc);
                    Stylesheet transformer = new Stylesheet(master);
                    transformer.setValidate(false);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    XmlResult result = new XmlResult(output);
                    transformer.transform(source, result);
                    byte[] transformedmessage = output.toByteArray();
                    HexUtils.copyStream(new ByteArrayInputStream(transformedmessage), new NullOutputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (XmlConfigException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // Make a test that tries to measure raw Tarari XSLT speed alone
        final RaxDocument fastDoc = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(requestBytes)));
        final XmlSource fastSource = new XmlSource(new EmptyInputStream());
        fastSource.setData(fastDoc);
        Runnable xsltTestFast = new Runnable() {
            public void run() {
                try {
                    Stylesheet transformer = new Stylesheet(master);
                    transformer.setValidate(false);
                    XmlResult result = new XmlResult(new NullOutputStream());
                    transformer.transform(fastSource, result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (XmlConfigException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        BenchmarkRunner br1 = new BenchmarkRunner(xsltTestSsg, 10000, "xsltTestSsg");
        br1.setThreadCount(10);
        br1.run();

        BenchmarkRunner br2 = new BenchmarkRunner(xsltTestFast, 10000, "xsltTestFast");
        br2.setThreadCount(10);
        br2.run();
    }
}
