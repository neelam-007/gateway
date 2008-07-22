/*
 * $Id$
 */
package com.l7tech.xml.tarari;

import com.l7tech.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
//import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TarariPlayground {
    private static GlobalTarariContext tarariContext;

    public static void main(String[] args) throws Exception {
        GlobalTarariContextImpl gtc = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        if (gtc != null) gtc.compileAllXpaths();

        if (args.length > 0) {
            Boolean soap = args.length == 2 ? Boolean.valueOf(args[1]) : Boolean.FALSE;
            run(args[0], soap.booleanValue(), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } else {
            run(null/*TestDocuments.DIR + "tiny.xml"*/, false, 5000, 1);
            run(null/*TestDocuments.PLACEORDER_CLEARTEXT*/, true, 5000, 1);
            run(null/*TestDocuments.DOTNET_USERNAME_TOKEN*/, true, 5000, 1);
            run(null/*TestDocuments.DOTNET_SIGNED_REQUEST*/, true, 5000, 1);
            run(null/*TestDocuments.PLACEORDER_WITH_MAJESTY*/, true, 5000, 1);
        }
    }

    private static void run(String docName, final boolean shouldBeSoap, final int num, int numThreads) throws Exception {
        InputStream is = null;//TestDocuments.getInputStream(docName);
        final byte[] bytes = IOUtils.slurpStream(is);
        long now = System.currentTimeMillis();

        List threads = new ArrayList();
        for (int i = 0; i < numThreads; i++) {
            Runnable runme = new Runnable() {
                public void run() {
                    try {
                        runnit(bytes, num, shouldBeSoap);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Thread t = new Thread(runme);
            threads.add(t);
            t.start();
        }

        for (Iterator i = threads.iterator(); i.hasNext();) {
            Thread t = (Thread)i.next();
            t.join();
        }

        long t = System.currentTimeMillis() - now;
        System.out.println("Ran " + num + " x " + numThreads + "(" + num*numThreads + ") isSoap on " + docName + " (" + bytes.length + " bytes) in " + t + "ms (" + (1000L * num * numThreads)/t + "/s)");
    }

    private static void runnit(byte[] bytes, int num, boolean shouldBeSoap) throws Exception {
        System.out.println("Starting");
        ByteArrayInputStream docStream = new ByteArrayInputStream(bytes);

        docStream.mark(102400);

        int i;
        int numSoap = 0;
        int numSec = 0;
        for (i = 0; i < num; i++) {
            docStream.reset();
            Message request = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, docStream);
            try {
                if (request.isSoap()) {
                    numSoap++;
                    if (request.getSoapKnob().isSecurityHeaderPresent()) {
                        numSec++;
                    }
                }
            } finally {
                request.close();
            }
        }

        if (shouldBeSoap && numSoap != num) {
            System.out.println("Expected " + num + " SOAP messages, got " + numSoap);
            return;
        }

        System.out.println("Done, " + numSoap + " SOAP, " + numSec + " with Security Header");
    }
}
