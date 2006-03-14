package com.l7tech.skunkworks.tarari;

import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.xslt11.Stylesheet;

import java.io.*;
import java.util.Date;

/**
 * Stand-alone reproduction for apparent deadlock in Tarari XSLT which depends only JRE and raxj classes.
 */
public class TarariXsltDeadlockRepro {
    private static final int NUM_THREADS = 20; // this is enough to find the deadlock on our quad Opteron
    private static final int NUM_ITERATIONS = 100; // per-thread.  this is almost always enough to see the deadlock

    // OutputStream for where XSLT results are sent.  Is first System.out, then NullOutputStream for the parallel run.
    private static OutputStream results;

    private static ThreadLocal tlSource = new ThreadLocal() {
        protected Object initialValue() {
            return new XmlSource(new byte[0]);
        }
    };

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
                    // SSG creates initial RaxDocument from the servlet InputStream:
                    raxDocument = RaxDocument.createDocument(new XmlSource(new ByteArrayInputStream(reqBytes)));
                    // (Not shown here: SSG then calls XPathProcessor.processXPaths())

                    // XSLT assertion reuses the already-created RaxDocument
                    XmlSource xmlSource = (XmlSource)tlSource.get();
                    xmlSource.setData(raxDocument);
                    Stylesheet stylesheet = new Stylesheet(master);
                    stylesheet.setValidate(false);

                    // In the SSG, output is to (essentially) a ByteArrayOutputStream
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

        // Run once to stdout to make sure it's working properly
        results = System.out;
        transformOnce.run();
        System.out.println();

        // Turn off results printing for the benchmark
        results = new NullOutputStream();

        // Create the worker threads
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < threads.length; i++)
            threads[i] = new Thread(transformLots, "XsltThread" + i);

        // Start them working.  (Our actual benchmark uses a barrier here so they all start at once.)
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads.length; i++)
            threads[i].start();
        System.out.println(new Date() + ": starting " + threads.length +
                " work threads to do " + NUM_ITERATIONS + " XSLT transformations each");

        // Wait for them to finish
        System.out.println("Waiting for them to finish...");
        for (int i = 0; i < threads.length; i++) {
            for (;;) {
                threads[i].join(2000);
                if (!threads[i].isAlive()) {
                    System.out.println("\nThread finished: " + threads[i].getName());
                    break;
                }
                System.out.print("."); // perk some dots.  When these stop appearing, it's locked up.
                System.out.flush();
            }
        }
        System.out.println();

        long end = System.currentTimeMillis();
        System.out.println("Total time = " + (end - start) + " ms");
    }

    /** An OutputStream that throws away all output. */
    private static class NullOutputStream extends OutputStream {
        private NullOutputStream() {}
        public void close() throws IOException {}
        public void flush() throws IOException {}
        public void write(int b) throws IOException {}
        public void write(byte b[]) throws IOException {}
        public void write(byte b[], int off, int len) throws IOException {}
    }
}
