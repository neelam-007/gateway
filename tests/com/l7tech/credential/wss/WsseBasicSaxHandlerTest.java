package com.l7tech.credential.wss;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import com.l7tech.util.SAXParsingCompleteException;

/**
 * User: flascell
 * Date: Aug 12, 2003
 * Time: 9:11:50 AM
 * $Id$
 *
 * Test the sax handler against a document
 */
public class WsseBasicSaxHandlerTest {

    public static void main(String[] args) throws Exception {
        String fileToParse = "/home/flascell/dev/wssebasic.xml";
        if (args.length > 0) fileToParse = args[0];

        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");

        XMLReader reader = XMLReaderFactory.createXMLReader();
        WsseBasicSaxHandler handler = new WsseBasicSaxHandler();
        reader.setContentHandler(handler);
        try {
            reader.parse(new InputSource(fileToParse));
        } catch (SAXParsingCompleteException e) {
            System.out.println("parsing complete");
        }
        System.out.println("Parsed username: " + handler.getParsedUsername());
        System.out.println("Parsed password: " + handler.getParsedPassword());
        System.out.println("Parsed password type: " + handler.getPasswdType());
    }
}
