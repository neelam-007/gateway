/*
 * $Id$
 */
package com.l7tech.skunkworks.tarari;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.TarariUtil;
import com.tarari.xml.Node;
import com.tarari.xml.NodeSet;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathCompiler;
import com.tarari.xml.xpath.XPathProcessor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class TarariPlayground {
    public static final String[] ISSOAP_XPATHS = {
        "/*[local-name()=\"Envelope\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]",
        "/*[local-name()=\"Envelope\"]/*[local-name()=\"Body\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]/*[namespace-uri()=\"\"]",
        "/*/*",
    };

    public static final String NS_XPATH_PREFIX = "//*[namespace-uri()=\"";
    public static final String NS_XPATH_SUFFIX = "\"]";

    public static final String[] ISSOAP1_2 = {
        "/*[local-name()=\"Envelope\" and namespace-uri()=\"http://www.w3.org/2003/05/soap-envelope\"]",
        "/*/*[local-name()=\"Header\" and namespace-uri()=\"http://www.w3.org/2003/05/soap-envelope\"]",
        "/*/*/*[local-name()=\"Body\" and namespace-uri()=\"http://www.w3.org/2003/05/soap-envelope\"]",
    };
    public static final String[] ISSOAP1_1 = {
        "/*[local-name()=\"Envelope\" and namespace-uri()=\"http://schemas.xmlsoap.org/soap/envelope/\"]",
        "/*/*[local-name()=\"Header\" and namespace-uri()=\"http://schemas.xmlsoap.org/soap/envelope/\"]",
        "/*/*[local-name()=\"Body\" and namespace-uri()=\"http://schemas.xmlsoap.org/soap/envelope/\"]",
    };

    public static final String[] ISFAULT = {
        "//*[local-name()=\"Fault\" and namespace-uri()=\"http://schemas.xmlsoap.org/soap/envelope/\"]",
        "//*[local-name()=\"Fault\" and namespace-uri()=\"http://schemas.xmlsoap.org/soap/envelope/\"]/*[local-name()=\"faultcode\"]/text()",
    };

    public static void main(String[] args) throws Exception {
        if (!TarariUtil.isTarariPresent()) throw new IllegalStateException("No Tarari card present");

        ArrayList xpaths0 = new ArrayList();
        xpaths0.addAll(Arrays.asList(ISSOAP_XPATHS));
        int ursStart = xpaths0.size() + 1; // 1-based arrays
        int[] uriIndices = new int[SoapUtil.ENVELOPE_URIS.size()];
        for (int i = 0; i < SoapUtil.ENVELOPE_URIS.size(); i++) {
            String uri = (String)SoapUtil.ENVELOPE_URIS.get(i);
            String nsXpath = NS_XPATH_PREFIX + uri + NS_XPATH_SUFFIX;
            xpaths0.add(nsXpath);
            uriIndices[i] = i + ursStart;
        }

        XPathCompiler.compile(xpaths0, 0);

        InputStream docis = TestDocuments.getInputStream(TestDocuments.DOTNET_USERNAME_TOKEN);
//        docis = new ByteArrayInputStream("<foo/>".getBytes("UTF-8"));
        XMLDocument doc = new XMLDocument(docis);
        XPathProcessor proc = new XPathProcessor(doc);
        RAXContext context = proc.processXPaths();

        System.out.print("Message is ");
        if (!context.isSoap(uriIndices))
            System.out.print("NOT ");
        System.out.println("SOAP!!!!11!!!!~~~one");

        int matched = context.getMatchedXPathCount();
        if (matched <= 0)
            throw new Exception("No matches");
        else
            System.out.println(matched + " expressions matched");

        int found = context.getCount(1);
        System.out.println("Found " + found + " nodes");

        NodeSet ns = context.getNodeSet(1);
        Node node = ns.getFirstNode();
        for (int i = 0; node != null; i++) {
            System.out.println("Node " + i + " = " + node.getPrefix() + ":" + node.getLocalName() + " (" + context.getNamespaceByPrefix(node, node.getPrefix()) + ")" );
            node = ns.getNextNode();
        }
    }
}
