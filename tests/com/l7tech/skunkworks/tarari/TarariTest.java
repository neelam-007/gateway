/*
 * $Id$
 */
package com.l7tech.skunkworks.tarari;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.server.tarari.ServerTarariContextImpl;
import com.l7tech.common.xml.tarari.ServerTarariContext;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class TarariTest {
    public static void main(String[] args) throws Exception {
        ServerTarariContext tarariContext = new ServerTarariContextImpl();
        String docName = args[0];

        List xpathList = new ArrayList(Arrays.asList(args));
        xpathList.remove(0); // Get rid of filename
        String[] xpathArray = (String[])xpathList.toArray(new String[0]);
        for (Iterator i = xpathList.iterator(); i.hasNext();) {
            String xpath = (String)i.next();
            tarariContext.add(xpath);
        }

        tarariContext.remove("//");
        tarariContext.add("//*[local-name()='Envelope']");
        tarariContext.compile();

        InputStream is = TestDocuments.getInputStream(docName);
        byte[] bytes = HexUtils.slurpStream(is);
        ByteArrayInputStream docStream = new ByteArrayInputStream(bytes);
        RAXContext context = getProcessedContext(docStream);

        System.out.print("Document is ");
        if (!context.isSoap(tarariContext.getSoapNamespaceUriIndices())) {
            System.out.print("NOT ");
        }
        System.out.println("SOAP");

        for (int i = 0; i < xpathArray.length; i++) {
            int index = tarariContext.getIndex(xpathArray[i]);
            System.out.print("XPath " + xpathArray[i]);
            if (index < 1) {
                System.out.println(" could not be compiled");
            } else {
                System.out.println(" matched " + context.getCount(index) + " nodes");
            }
        }
    }

    private static RAXContext getProcessedContext(InputStream docStream) throws XMLDocumentException, XPathProcessorException {
        XMLDocument doc = new XMLDocument(docStream);
        XPathProcessor proc = new XPathProcessor(doc);
        RAXContext context = proc.processXPaths();
        return context;
    }


}
