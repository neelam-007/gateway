/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks.tarari;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.skunkworks.BenchmarkRunner;
import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.xslt11.Stylesheet;

import java.io.ByteArrayInputStream;
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
    private static BenchmarkRunner br1;
    private static BenchmarkRunner br2;

    public static void main(String[] args) throws Exception {
        final String reqStr = TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/DocSearchResponse.xml");
        final String xsltStr = TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/GetDocInfoFull.xsl");

        final byte[] xsltBytes = xsltStr.getBytes();
        final byte[] requestBytes = reqStr.getBytes();
        final Stylesheet master = Stylesheet.create(new XmlSource(xsltBytes));

        final GlobalTarariContextImpl gtci = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        gtci.compileAllXpaths();
        final TarariMessageContextFactory cfac = TarariLoader.getMessageContextFactory();

        // Make a test that simulates what the SSG has to do
        Runnable xsltTestSsg = new Runnable() {
            public void run() {
                try {
                    TarariMessageContextImpl ctx = (TarariMessageContextImpl)cfac.makeMessageContext(new ByteArrayInputStream(requestBytes));
                    RaxDocument doc = ctx.getRaxDocument();
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
                } catch (Exception e) {
                    e.printStackTrace();
                    stopAll();
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
                } catch (Exception e) {
                    e.printStackTrace();
                    stopAll();
                    throw new RuntimeException(e);
                }
            }
        };

        br1 = new BenchmarkRunner(xsltTestSsg, 10000, "xsltTestSsg");
        br1.setThreadCount(10);
        br1.run();

        br2 = new BenchmarkRunner(xsltTestFast, 10000, "xsltTestFast");
        br2.setThreadCount(10);
        br2.run();
    }

    private static void stopAll() {
        if (br1 != null) br1.stopAll();
        if (br2 != null) br2.stopAll();
    }
}
