package com.l7tech.server.policy.assertion.credential.wss;

import org.xml.sax.*;
import com.l7tech.common.util.SAXParsingCompleteException;
import com.l7tech.common.util.SoapUtil;

/**
 * SAX Handler for xml document which contains a wsse usernametoken element.
 *
 * Extracts a username and password from an xml document with the following format:
 * <pre>
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
 * </pre>
 * This handler will throw a SAXParsingCompleteException to indicate that all the information was extracted
 * out of the document. This stops parsing early and makes this operation faster. If no username and password info
 * is found passwd the soap header, the handler will also throw a SAXParsingCompleteException but the password and
 * username properties would then be null.
 *
 * The optional password type is captured in the passwdType property. If not present it is set to the
 * DEFAULT_PASSWORD_TYPE constant.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 11, 2003<br/>
 * $Id$
 */
public class WsseBasicSaxHandler implements ContentHandler {

    /**
     * type of password that is cleartext password.
     * query passwdType property and make sure you are getting a cleartext password.
     */
    public static final String DEFAULT_PASSWORD_TYPE = "wsse:PasswordText";

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
        else if (localName.equals(SoapUtil.SECURITY_EL_NAME) && insideSecurity) insideSecurity = false;
        else if (localName.equals(SoapUtil.HEADER_EL_NAME) && insideHeader) {
            insideHeader = false;
            // passed that, we are not going to find what we are seeking
            throw new SAXParsingCompleteException();
        }
        else if (localName.equals(SoapUtil.ENVELOPE_EL_NAME) && insideEnvelope) insideEnvelope = false;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equals(SoapUtil.ENVELOPE_EL_NAME) && !insideEnvelope) insideEnvelope = true;
        else if (localName.equals(SoapUtil.HEADER_EL_NAME) && insideEnvelope) insideHeader = true;
        else if (localName.equals(SoapUtil.SECURITY_EL_NAME) && insideEnvelope && insideHeader) insideSecurity = true;
        else if (localName.equals(USERNAMETOKEN_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsernameToken = true;
        else if (localName.equals(USERNAME_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) insideUsername = true;
        else if (localName.equals(PASSWORD_TAGNAME) && insideEnvelope && insideHeader && insideSecurity) {
            // capture the password type
            for (int i = 0; i < atts.getLength(); i++) {
                if (atts.getLocalName(i).equals(PASSWORD_TYPE_ATTRIBUTE_NAME)) {
                    passwdType = atts.getValue(i);
                    break;
                }
            }
            if (passwdType == null) passwdType = DEFAULT_PASSWORD_TYPE;
            insidePassword = true;
        }
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

    public String getParsedPassword() {
        return parsedPassword;
    }

    public String getPasswdType() {
        return passwdType;
    }


    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {}
    public void processingInstruction(String target, String data) throws SAXException {}
    public void skippedEntity(String name) throws SAXException {}
    public void setDocumentLocator(Locator locator) {}
    public void startDocument() throws SAXException {}
    public void endDocument() throws SAXException {}
    public void startPrefixMapping(String prefix, String uri) throws SAXException {}
    public void endPrefixMapping(String prefix) throws SAXException {}

    private String parsedPassword = null;
    private String parsedUsername = null;
    private String passwdType = null;

    protected StringBuffer databuf = null;
    protected boolean insideEnvelope = false;
    protected boolean insideHeader = false;
    protected boolean insideSecurity = false;
    protected boolean insideUsernameToken = false;
    protected boolean insideUsername = false;
    protected boolean insidePassword = false;

    protected static final String USERNAMETOKEN_TAGNAME = "UsernameToken";
    protected static final String USERNAME_TAGNAME = "Username";
    protected static final String PASSWORD_TAGNAME = "Password";
    protected static final String PASSWORD_TYPE_ATTRIBUTE_NAME = "Type";
}
