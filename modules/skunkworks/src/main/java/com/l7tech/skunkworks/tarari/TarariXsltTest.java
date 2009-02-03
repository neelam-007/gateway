/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks.tarari;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.xml.tarari.TarariMessageContextImpl;
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
        if (args.length < 2) usage();
        int num = Integer.parseInt(args[0]);
        if (num < 1) throw new IllegalArgumentException("Num must be positive");
        int threads = Integer.parseInt(args[1]);
        if (threads > 5000) System.err.println("Warning: Using very large number of threads (" + threads + ")");
        if (threads < 1) throw new IllegalArgumentException("Threads must be positive");

        boolean noSsg = false;
        if (args.length > 2) noSsg = Boolean.valueOf(args[2]).booleanValue();

        final String reqStr = null;//TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/DocSearchResponse.xml");
        final String xsltStr = null;//TestDocuments.getTestDocumentAsXml(TestDocuments.DIR + "xslt/GetDocInfoFull.xsl");

        final byte[] xsltBytes = xsltStr.getBytes();
        final byte[] requestBytes = reqStr.getBytes();
        final Stylesheet master = Stylesheet.create(new XmlSource(xsltBytes));
        final GlobalTarariContextImpl gtci = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        gtci.compileAllXpaths();

        // Make a test that tries to measure raw Tarari XSLT speed alone
        final RaxDocument fastDoc = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(requestBytes)));
        final XmlSource fastSource = new XmlSource(new EmptyInputStream());
        fastSource.setData(fastDoc);

        // Try once to make sure it works
        Stylesheet transformer = new Stylesheet(master);
        transformer.setValidate(false);
        XmlResult result = new XmlResult(System.out);
        System.out.println("Result of raw transform:");
        transformer.transform(fastSource, result);
        System.out.println("\n");

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

        br2 = new BenchmarkRunner(xsltTestFast, num, "xsltTestFast");
        br2.setThreadCount(threads);
        br2.run();

        if (!noSsg) testSsgSim(requestBytes, master, num, threads);
    }

    private static void testSsgSim(final byte[] requestBytes, final Stylesheet master, int num, int threads) throws InterruptedException {
        // Make a test that simulates what the SSG has to do
        final TarariMessageContextFactory cfac = TarariLoader.getMessageContextFactory();

        Runnable xsltTestSsg = new Runnable() {
            public void run() {
                try {
                    TarariMessageContextImpl ctx = (TarariMessageContextImpl)cfac.makeMessageContext(new ByteArrayInputStream(requestBytes));
                    try {
                        RaxDocument doc = ctx.getRaxDocument();
                        XmlSource source = (XmlSource)xmlSource.get();
                        source.setData(doc);
                        Stylesheet transformer = new Stylesheet(master);
                        transformer.setValidate(false);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        XmlResult result = new XmlResult(output);
                        transformer.transform(source, result);
                        byte[] transformedmessage = output.toByteArray();
                        IOUtils.copyStream(new ByteArrayInputStream(transformedmessage), new NullOutputStream());
                    } finally {
                        ctx.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopAll();
                    throw new RuntimeException(e);
                }
            }
        };

        br1 = new BenchmarkRunner(xsltTestSsg, num, "xsltTestSsg");
        br1.setThreadCount(threads);
        br1.run();
    }

    private static void usage() {
        System.err.println("Usage: TarariXsltTest <num> <threads>");
        System.exit(1);
    }

    private static void stopAll() {
        if (br1 != null) br1.stopAll();
        if (br2 != null) br2.stopAll();
    }
}
