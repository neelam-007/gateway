package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import javax.xml.parsers.DocumentBuilder;

import com.ibm.xml.dsig.util.DOMParserNS;
import com.ibm.xml.sax.StandardErrorHandler;
import com.l7tech.common.security.xml.SecureConversationTokenHandler;

import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

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
public class SecureConversationTokenHandlerTest extends TestCase {
    public SecureConversationTokenHandlerTest() {
        testSubject = new SecureConversationTokenHandler();
    }

    public void testAppendAndReadNonce() throws Exception {
        Document doc = readDocFromString(simpleDoc);
        System.out.println("Original doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        long nonce = 2345432;
        testSubject.appendNonceToDocument(doc, nonce);
        System.out.println("Doc with nonce");
        System.out.println(serializeDocWithXMLSerializer(doc));
        long nonce2 = testSubject.readNonceFromDocument(doc);
        assertTrue("Read nonce same as append one", nonce == nonce2);
    }

    public void testAppendAndReadSessIdAndSeqNrToDocument() throws Exception {
        Document doc = readDocFromString(simpleDoc);
        System.out.println("Original doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        long sessId = 2345432;
        long seqNr = 25;
        testSubject.appendSessIdAndSeqNrToDocument(doc, sessId, seqNr);
        System.out.println("Doc with nonce");
        System.out.println(serializeDocWithXMLSerializer(doc));
        long sessId2 = testSubject.readSessIdFromDocument(doc);
        long seqNr2 = testSubject.readSeqNrFromDocument(doc);
        assertTrue("Read sessId same as append one", sessId == sessId2);
        assertTrue("Read seqNr same as append one", seqNr == seqNr2);
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
    private SecureConversationTokenHandler testSubject = null;
}
