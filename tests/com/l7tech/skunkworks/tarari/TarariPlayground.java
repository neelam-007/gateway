/*
 * $Id$
 */
package com.l7tech.skunkworks.tarari;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.server.tarari.GlobalTarariContextImpl;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TarariPlayground {
    private static GlobalTarariContext tarariContext;

    public static void main(String[] args) throws Exception {
        tarariContext = new GlobalTarariContextImpl();
        tarariContext.compile();

        if (args.length > 0) {
            Boolean soap = args.length == 2 ? Boolean.valueOf(args[1]) : Boolean.FALSE;
            run(args[0], soap.booleanValue());
        } else {
            run(TestDocuments.DIR + "tiny.xml", false);
            run(TestDocuments.PLACEORDER_CLEARTEXT, true);
            run(TestDocuments.DOTNET_USERNAME_TOKEN, true);
            run(TestDocuments.DOTNET_SIGNED_REQUEST, true);
            run(TestDocuments.PLACEORDER_WITH_MAJESTY, true);
        }
    }

    private static void run(String docName, boolean shouldBeSoap) throws Exception {
        InputStream is = TestDocuments.getInputStream(docName);
        byte[] bytes = HexUtils.slurpStream(is);
        ByteArrayInputStream docStream = new ByteArrayInputStream(bytes);

        docStream.mark(102400);
        long now = System.currentTimeMillis();
        int i;
        for (i = 0; i < 40000; i++) {
            docStream.reset();
            RAXContext context = getProcessedContext(docStream);

            if (context.isSoap(tarariContext.getSoapNamespaceUriIndices()) != shouldBeSoap) {
                System.out.println("BAD");
            }
        }
        long t = System.currentTimeMillis() - now;
        System.out.println("Ran " + i + " isSoap on " + docName + " (" + bytes.length + " bytes) in " + t + "ms (" + (1000L * i)/t + "/s)");
    }

    private static RAXContext getProcessedContext(InputStream docStream) throws XMLDocumentException, XPathProcessorException {
        XMLDocument doc = new XMLDocument(docStream);
        XPathProcessor proc = new XPathProcessor(doc);
        RAXContext context = proc.processXPaths();
        return context;
    }


}
