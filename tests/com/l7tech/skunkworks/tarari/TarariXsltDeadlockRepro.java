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
import java.util.Date;

/**
 * Stand-alone reproduction for deadlock in Tarari XSLT.  Uses only JRE and raxj classes.
 */
public class TarariXsltDeadlockRepro {
    private static final ByteArrayInputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(new byte[0]);
    private static final int NUM_THREADS = 5; // this is enough to find the deadlock on our quad Opteron
    private static final int NUM_ITERATIONS = 1000; // this is enough to see the deadlock

    private static OutputStream results;

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
                RaxDocument raxDocument = null;
                try {
                    raxDocument = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(reqBytes)));
                    final XmlSource xmlSource = new XmlSource(EMPTY_INPUT_STREAM);
                    xmlSource.setData(raxDocument);
                    Stylesheet stylesheet = new Stylesheet(master);
                    stylesheet.setValidate(false);
                    XmlResult xmlResult = new XmlResult(results);
                    stylesheet.transform(xmlSource, xmlResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    if (raxDocument != null)
                        raxDocument.release();
                }
            }
        };

        // Create the runnable
        final Runnable transformLots = new Runnable() {
            public void run() {
                System.out.println("\nThread starting: " + Thread.currentThread().getName());
                for (int i = 0; i < NUM_ITERATIONS; ++i)
                    transformOnce.run();
            }
        };

        // Run once to stdout to make sure it work properly
        results = System.out;
        transformOnce.run();
        System.out.println();

        // Turn off results printing for the benchmark
        results = new NullOutputStream();

        // Create two threads
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < threads.length; i++)
            threads[i] = new Thread(transformLots, "XsltThread" + i);

        // Start them working
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++)
            threads[i].start();
        System.out.println(new Date() + ": starting " + threads.length +
                " work threads doing " + NUM_ITERATIONS + " XSLT transformations each");

        // Wait for them to finish
        System.out.println("Waiting for them to finish...");
        for (int i = 0; i < threads.length; i++) {
            for (;;) {
                threads[i].join(2000);
                if (!threads[i].isAlive()) {
                    System.out.println("\nThread finished: " + threads[i].getName());
                    break;
                }
                System.out.print(".");
                System.out.flush();
            }
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
