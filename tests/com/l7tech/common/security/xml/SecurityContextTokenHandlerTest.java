package com.l7tech.common.security.xml;

import junit.framework.TestCase;
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
 * Tests for SecurityContextTokenHandler class.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 10, 2004<br/>
 * $Id$
 */
public class SecurityContextTokenHandlerTest extends TestCase {

    public void testAppendAndReadSession() throws Exception {
        Document doc = readDocFromString(simpleDoc);
        System.out.println("Original doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        byte[] sessionid = SecurityContextTokenHandler.generateNewSessionId();
        SecurityContextTokenHandler.appendSessionInfoToSoapMessage(doc, sessionid, 69);
        System.out.println("Modified doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        // test that we can get sesionid out of this.
        byte[] session2 = SecurityContextTokenHandler.getSessionIdFromWSCToken(doc);
        for (int i = 0; i < sessionid.length; i++) {
            assertTrue(sessionid[i] == session2[i]);
        }
        System.out.println("sessionid match");
    }

    public void testAppendAndReadSessionWithCreation() throws Exception {
        Document doc = readDocFromString(simpleDoc);
        System.out.println("Original doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        byte[] sessionid = SecurityContextTokenHandler.generateNewSessionId();
        SecurityContextTokenHandler.appendSessionInfoToSoapMessage(doc, sessionid, 69, System.currentTimeMillis());
        System.out.println("Modified doc");
        System.out.println(serializeDocWithXMLSerializer(doc));
        // test that we can get sesionid out of this.
        byte[] session2 = SecurityContextTokenHandler.getSessionIdFromWSCToken(doc);
        for (int i = 0; i < sessionid.length; i++) {
            assertTrue(sessionid[i] == session2[i]);
        }
        System.out.println("sessionid match");
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

    private String simpleDoc = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
                                    "<S:Body>" +
                                        "<tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">" +
                                            "QQQ" +
                                        "</tru:StockSymbol>" +
                                    "</S:Body>" +
                                "</S:Envelope>";
}
