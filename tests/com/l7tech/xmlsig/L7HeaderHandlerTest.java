package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import javax.xml.parsers.DocumentBuilder;

import com.ibm.xml.dsig.util.DOMParserNS;
import com.ibm.xml.sax.StandardErrorHandler;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 10:49:04 AM
 * $Id$
 *
 *
 */
public class L7HeaderHandlerTest {

    public static void main(String[] args) throws Exception {
        L7HeaderHandlerTest toto = new L7HeaderHandlerTest();
        L7HeaderHandler tata = new L7HeaderHandler();
        toto.testAppendNonceToDocument(tata);
    }

    public void testAppendNonceToDocument(L7HeaderHandler testee) throws Exception {
        Document doc = readDocFromString(simpleDoc);
        System.out.println("Original doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        testee.appendNonceToDocument(doc, 2345432);
        System.out.println("Doc with nonce");
        System.out.println(serializeDocWithXMLSerializer(doc));
    }

    private Document readDocFromString(String docStr)  throws Exception {
        DocumentBuilder builder = DOMParserNS.createBuilder();
        builder.setErrorHandler(new StandardErrorHandler());
        return builder.parse(new InputSource(new StringReader(docStr)));
    }

    public String serializeDocWithXMLSerializer(Document doc) throws Exception {
        final StringWriter sw = new StringWriter();
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        try {
            OutputFormat of = new OutputFormat();
            of.setIndent(4);
            xmlSerializer.setOutputFormat(of);
            xmlSerializer.serialize(doc);
        } catch (Exception e) {}
        return sw.toString();
    }

    private String simpleDoc = "<?xml version=\"1.0\" encoding=\"utf-8\"?><S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\"><S:Body><tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">QQQ</tru:StockSymbol></S:Body></S:Envelope>";
}
