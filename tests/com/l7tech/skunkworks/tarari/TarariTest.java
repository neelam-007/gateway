/*
 * $Id$
 */
package com.l7tech.skunkworks.tarari;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.tarari.TarariUtil;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class TarariTest {
    public static void main(String[] args) throws Exception {
        String docName = args[0];

        List xpathList = Arrays.asList(args);
        xpathList.remove(0); // Get rid of filename
        String[] xpathArray = (String[])xpathList.toArray(new String[0]);
        int[] indices = TarariUtil.setupIsSoap(xpathArray);

        InputStream is = TestDocuments.getInputStream(docName);
        byte[] bytes = HexUtils.slurpStream(is);
        ByteArrayInputStream docStream = new ByteArrayInputStream(bytes);
        RAXContext context = getProcessedContext(docStream);

        System.out.print("Document is ");
        if (!context.isSoap(TarariUtil.getUriIndices())) {
            System.out.print("NOT ");
        }
        System.out.println("SOAP");

        for (int i = 0; i < indices.length; i++) {
            System.out.println("XPath " + args[i+1] + " matched " + context.getCount(indices[i]) + " nodes");
        }
    }

    private static RAXContext getProcessedContext(InputStream docStream) throws XMLDocumentException, XPathProcessorException {
        XMLDocument doc = new XMLDocument(docStream);
        XPathProcessor proc = new XPathProcessor(doc);
        RAXContext context = proc.processXPaths();
        return context;
    }


}
