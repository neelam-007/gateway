package com.l7tech.credential.wss;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;
import com.l7tech.util.SAXParsingCompleteException;

/**
 * User: flascell
 * Date: Aug 11, 2003
 * Time: 4:14:08 PM
 * $Id$
 *
 * SAX Handler for xml document which contains a wsse usernametoken element.
 *
 * Extracts a username and password from an xml document with the following format:
 * <?xml version="1.0" encoding="utf-8"?>
 * <S:Envelope xmlns:S="http://www.w3.org/2001/12/soap-envelope" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
 *         <S:Header>
 *                 <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/04/secext">
 *                         <wsse:UsernameToken Id="MyID">
 *                                 <wsse:Username>Zoe</wsse:Username>
 *                                 <Password Type="wsse:PasswordText">ILoveLlamas</Password>
 *                         </wsse:UsernameToken>
 *                 </wsse:Security>
 *         </S:Header>
 *         <S:Body Id="MsgBody">
 *                 <tru:StockSymbol xmlns:tru="http://fabrikam123.com/payloads">QQQ</tru:StockSymbol>
 *         </S:Body>
 * </S:Envelope>
 *
 * This handler will throw a SAXParsingCompleteException to indicate that all the information was extracted
 * out of the document. This stops parsing early and makes this operation faster.
 */
public class WsseBasicSaxHandler implements ContentHandler {

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equals(USERNAME_TAGNAME) && insideUsername) {
            parsedUsername = databuf.toString();
            databuf = new StringBuffer();
            insideUsername = false;
            // stop parsing if we got all
            if (parsedPassword != null) throw new SAXParsingCompleteException();
        }
        if (localName.equals(PASSWORD_TAGNAME) && insidePassword) {
            parsedPassword = databuf.toString();
            databuf = null;
            insidePassword = false;
            // stop parsing if we got all
            if (parsedUsername != null) throw new SAXParsingCompleteException();
        }
        else if (localName.equals(USERNAMETOKEN_TAGNAME) && insideUsernameToken) insideUsernameToken = false;
        else if (localName.equals(SECURITY_TAGNAME) && insideSecurity) insideSecurity = false;
        else if (localName.equals(HEADER_TAGNAME) && insideHeader) insideHeader = false;
        else if (localName.equals(ENVELOPE_TAGNAME) && insideEnvelope) insideEnvelope = false;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equals(ENVELOPE_TAGNAME) && !insideEnvelope) insideEnvelope = true;
        else if (localName.equals(HEADER_TAGNAME) && insideEnvelope) insideHeader = true;
        else if (localName.equals(SECURITY_TAGNAME) && insideEnvelope && insideHeader) insideSecurity = true;
        else if (localName.equals(USERNAMETOKEN_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsernameToken = true;
        else if (localName.equals(USERNAME_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsername = true;
        else if (localName.equals(PASSWORD_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insidePassword = true;
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (insideUsername || insidePassword) {
            if (databuf == null) databuf = new StringBuffer();
            databuf.append(ch, start, length);
        }
    }

    public String getParsedUsername() {
        return parsedUsername;
    }

    public void setParsedUsername(String parsedUsername) {
        this.parsedUsername = parsedUsername;
    }

    public String getParsedPassword() {
        return parsedPassword;
    }

    public void setParsedPassword(String parsedPassword) {
        this.parsedPassword = parsedPassword;
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {}
    public void processingInstruction(String target, String data) throws SAXException {}
    public void skippedEntity(String name) throws SAXException {}
    public void setDocumentLocator(Locator locator) {}
    public void startDocument() throws SAXException {}
    public void endDocument() throws SAXException {}
    public void startPrefixMapping(String prefix, String uri) throws SAXException {}
    public void endPrefixMapping(String prefix) throws SAXException {}

    // todo, move this to a test class
    public static void main(String[] args) throws Exception {
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        XMLReader reader = XMLReaderFactory.createXMLReader();
        WsseBasicSaxHandler handler = new WsseBasicSaxHandler();
        reader.setContentHandler(handler);
        try {
            reader.parse(new InputSource("/home/flascell/dev/wssebasic.xml"));
        } catch (SAXParsingCompleteException e) {
            System.out.println("parsing complete");
        }
        System.out.println("Parsed username: " + handler.getParsedUsername());
        System.out.println("Parsed password: " + handler.getParsedPassword());
    }

    private String parsedPassword = null;
    private String parsedUsername = null;

    protected StringBuffer databuf = null;
    protected boolean insideEnvelope = false;
    protected boolean insideHeader = false;
    protected boolean insideSecurity = false;
    protected boolean insideUsernameToken = false;
    protected boolean insideUsername = false;
    protected boolean insidePassword = false;
    protected static final String ENVELOPE_TAGNAME = "Envelope";
    protected static final String HEADER_TAGNAME = "Header";
    protected static final String SECURITY_TAGNAME = "Security";
    protected static final String USERNAMETOKEN_TAGNAME = "UsernameToken";
    protected static final String USERNAME_TAGNAME = "Username";
    protected static final String PASSWORD_TAGNAME = "Password";
}

