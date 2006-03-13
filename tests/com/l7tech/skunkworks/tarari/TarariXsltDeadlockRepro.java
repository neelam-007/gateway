/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks.tarari;

import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.xslt11.Stylesheet;

import java.io.*;

/**
 * Stand-alone reproduction for deadlock in Tarari XSLT.  Uses only JRE and raxj classes.
 */
public class TarariXsltDeadlockRepro {
    private static final ByteArrayInputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(new byte[0]);
    private static final int NUM_THREADS = 2; // 2 is enough to find the deadlock on our quad Opteron
    private static final int NUM_ITERATIONS = 5000; // this is enough to deadlock every time I've tried it

    public static void main(String[] args) throws Exception {
        InputStream req = new FileInputStream("DocSearchResponse.xml");
        InputStream xslt = new FileInputStream("GetDocInfoFull.xsl");

        // Slurp the stylesheet
        final Stylesheet master = Stylesheet.create(new XmlSource(xslt));

        // Slurp the test document
        byte[] buf = new byte[2048];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int got;
        while ((got = req.read(buf)) > 0)
            baos.write(buf, 0, got);
        final byte[] reqBytes = baos.toByteArray();

        // Create the work unit
        final Runnable transformOnce = new Runnable() {
            public void run() {
                RaxDocument fastDoc = null;
                try {
                    fastDoc = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(reqBytes)));
                    final XmlSource xmlSource = new XmlSource(EMPTY_INPUT_STREAM);
                    xmlSource.setData(fastDoc);
                    Stylesheet stylesheet = new Stylesheet(master);
                    stylesheet.setValidate(false);
                    XmlResult xmlResult = new XmlResult(new NullOutputStream());
                    stylesheet.transform(xmlSource, xmlResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    if (fastDoc != null) fastDoc.release();
                }
            }
        };

        // Create the runnable
        final Runnable transformLots = new Runnable() {
            public void run() {
                for (int i = 0; i < NUM_ITERATIONS; ++i)
                    transformOnce.run();
            }
        };

        // Create two threads
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < threads.length; i++)
            threads[i] = new Thread(transformLots);

        // Start them working
        long start = System.currentTimeMillis();
        System.out.println("Starting " + threads.length + " work threads...");
        for (int i = 0; i < threads.length; i++)
            threads[i].start();

        // Wait for them to finish
        System.out.println("Waiting for them to finish...");
        for (int i = 0; i < threads.length; i++) {
            System.out.print(".");
            System.out.flush();
            threads[i].join(5000);
        }
        System.out.println();

        long end = System.currentTimeMillis();
        System.out.println("Total time = " + (end - start) + " ms");
    }

    /** An OutputStream that throws away all output. */
    public static class NullOutputStream extends OutputStream {
        public NullOutputStream() {}
        public void close() throws IOException {}
        public void flush() throws IOException {}
        public void write(int b) throws IOException {}
        public void write(byte b[]) throws IOException {}
        public void write(byte b[], int off, int len) throws IOException {}
    }

}
