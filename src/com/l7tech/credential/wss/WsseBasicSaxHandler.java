package com.l7tech.credential.wss;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.StringReader;

/**
 * Created by IntelliJ IDEA.
 * User: flascell
 * Date: Aug 11, 2003
 * Time: 4:14:08 PM
 * $Id$
 *
 * SAX Handler for xml document which contains a wsse usernametoken element.
 */
public class WsseBasicSaxHandler implements ContentHandler {
    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equals(ENVELOPE_TAGNAME) && !insideEnvelope) insideEnvelope = true;
        else if (localName.equals(HEADER_TAGNAME) && insideEnvelope) insideHeader = true;
        else if (localName.equals(SECURITY_TAGNAME) && insideEnvelope && insideHeader) insideSecurity = true;
        else if (localName.equals(USERNAMETOKEN_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsernameToken = true;
        else if (localName.equals(USERNAME_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsername = true;
        else if (localName.equals(PASSWORD_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insidePassword = true;
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equals(USERNAME_TAGNAME) && insideUsername) {
            parsedUsername = databuf.toString();
            databuf = new StringBuffer();
            insideUsername = false;
        }
        if (localName.equals(PASSWORD_TAGNAME) && insidePassword) {
            parsedPassword = databuf.toString();
            databuf = null;
            insidePassword = false;
        }
        else if (localName.equals(USERNAMETOKEN_TAGNAME) && insideUsernameToken) insideUsernameToken = false;
        else if (localName.equals(SECURITY_TAGNAME) && insideSecurity) insideSecurity = false;
        else if (localName.equals(HEADER_TAGNAME) && insideHeader) insideHeader = false;
        else if (localName.equals(ENVELOPE_TAGNAME) && insideEnvelope) insideEnvelope = false;
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (insideUsername || insidePassword) {
            if (databuf == null) databuf = new StringBuffer();
            databuf.append(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public String getParsedUsername() {
        return parsedUsername;
    }

    public void setParsedUsername(String parsedUsername) {
        this.parsedUsername = parsedUsername;
    }

    private String parsedUsername = null;

    public String getParsedPassword() {
        return parsedPassword;
    }

    public void setParsedPassword(String parsedPassword) {
        this.parsedPassword = parsedPassword;
    }

    // todo, move this to a test class
    public static void main(String[] args) throws Exception {
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        XMLReader reader = XMLReaderFactory.createXMLReader();
        WsseBasicSaxHandler handler = new WsseBasicSaxHandler();
        reader.setContentHandler(handler);
        reader.parse(new InputSource("/home/flascell/dev/wssebasic.xml"));
        System.out.println("Parsed username: " + handler.getParsedUsername());
        System.out.println("Parsed password: " + handler.getParsedPassword());
    }

    private String parsedPassword = null;
    private StringBuffer databuf = null;
    private boolean insideEnvelope = false;
    private boolean insideHeader = false;
    private boolean insideSecurity = false;
    private boolean insideUsernameToken = false;
    private boolean insideUsername = false;
    private boolean insidePassword = false;
    private static final String ENVELOPE_TAGNAME = "Envelope";
    private static final String HEADER_TAGNAME = "Header";
    private static final String SECURITY_TAGNAME = "Security";
    private static final String USERNAMETOKEN_TAGNAME = "UsernameToken";
    private static final String USERNAME_TAGNAME = "Username";
    private static final String PASSWORD_TAGNAME = "Password";
}

