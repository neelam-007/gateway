package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Appends and parses out xml security session information in/out of soap messages.
 * The specifications used here are WS-Secure Conversation and WS-Trust versions 1.1.
 *
 * This is not yet plugged in and will eventually phase out the class SecureConversationTokenHandler.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 10, 2004<br/>
 * $Id$
 */
public class SecurityContextTokenHandler {
    private static final int SESSION_BYTES = 16;
    private static final SecurityContextTokenHandler INSTANCE = new SecurityContextTokenHandler();

    private SecurityContextTokenHandler() {}

    public static SecurityContextTokenHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a new random session id of 16 bytes.
     * @return
     */
    public byte[] generateNewSessionId() {
        byte[] sessionid = new byte[SESSION_BYTES];
        rand.nextBytes(sessionid);
        return sessionid;
    }

    /**
     * Converts raw session id to its URI representation.
     * @param rawSessionId a session id 16 bytes long
     * @return URI representation (looks like uuid:base64edrawsessionid)
     */
    public String sessionIdToURI(byte[] rawSessionId) {
        // the sanity check
        if (rawSessionId == null || rawSessionId.length != SESSION_BYTES) {
            throw new IllegalArgumentException("Session ID is the wrong length");
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return URI_PREFIX + encoder.encode(rawSessionId);
    }

    /**
     * Creates a soap message that contains a RequestSecurityToken request
     * following the WS-Trust 1.1 specification (april 2004). This is
     * intended to be used by a client who wants to get a new Security Context
     * (session) from a SSG server. This message needs to be signed by the
     * requestor before it is sent to a server.
     *
     * @return a document containing the soap message with a RequestSecurityToken body
     */
    public Document createNewRequestSecurityToken() throws IOException, SAXException {
        return XmlUtil.stringToDocument(REQUESTSECURITYTOKEN_SOAPMSG);
    }

    /**
     * does the reverse of sessionIdToURI
     * @param uri URI representation (looks like uuid:base64edrawsessionid)
     * @return a 16 bytes session id
     */
    public byte[] URIToSessionId(String uri) throws IOException {
        if (uri == null || !uri.startsWith(URI_PREFIX)) {
            throw new IllegalArgumentException("This is not a proper sessionid URI (" + uri + ")");
        }
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(uri.substring(URI_PREFIX.length()));
    }

    /**
     * This appends the sessionid to the message using ws-sc lingo.
     * Same as other appendSessionInfoToSoapMessage except this one also includes a creation timestamp. This
     * extra parameter should be used when a sessionid is 'suggested' to a SSG for the first time.
     * TODO: should we strip any existing SecurityContextToken from the SecurityHeader?
     */
    public void appendSessionInfoToSoapMessage(Document soapmsg, byte[] sessionid, long seqnumber,
                                                      long creationtimestamp) throws SoapUtil.MessageNotSoapException {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        if ( securityCtxTokEl == null ) throw new SoapUtil.MessageNotSoapException("Can't append session info to non-SOAP message");
        appendIDElement(securityCtxTokEl, sessionid);
        setMessageNumber(securityCtxTokEl, new Long(seqnumber));
        appendCreationElement(securityCtxTokEl, creationtimestamp);
    }

    /**
     * Attempts to extract the session id from a wsse:Security/wsc:SecurityContextToken/wsc:Identifier element
     * @return null if not present
     * @throws IOException if the URI is formatted incorrectly
     * @throws XmlUtil.MultipleChildElementsException if there is more than one SOAP header, security header,
     *                                                security context token, or token ID.
     */
    public byte[] getSessionIdFromWSCToken(Document soapMsg) throws IOException, XmlUtil.MultipleChildElementsException {
        // get the element
        Element token = getSecurityContextTokenElement(soapMsg);
        Element idel = XmlUtil.findOnlyOneChildElementByName(token, WSC_NAMESPACE, SCTOKEN_ID_ELNAME);
        String childText = XmlUtil.findFirstChildTextNode(idel);
        return URIToSessionId(childText);
    }

    /**
     * Get the SecurityContextToken element from the specified SOAP message, or null if there isn't one.
     * @param doc the SOAP envelope to examine
     * @return the SecurityContextToken element from the SOAP security header, or null
     * @throws XmlUtil.MultipleChildElementsException if there was more than one SOAP header, security header,
     *                                                or SecurityContextToken
     */
    public Element getSecurityContextTokenElement(Document doc) throws XmlUtil.MultipleChildElementsException {
        String soapns = doc.getDocumentElement().getNamespaceURI();
        Element header = XmlUtil.findOnlyOneChildElementByName(doc.getDocumentElement(), soapns, SoapUtil.HEADER_EL_NAME);
        if (header == null)
           return null;
        Element security = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.SECURITY_NAMESPACE, SoapUtil.SECURITY_EL_NAME);
        if (security == null)
            security = XmlUtil.findOnlyOneChildElementByName(header, SoapUtil.SECURITY_NAMESPACE2, SoapUtil.SECURITY_EL_NAME);
        if (security == null)
            return null;
        Element token = XmlUtil.findOnlyOneChildElementByName(security, WSC_NAMESPACE, SCTOKEN_ELNAME);
        return token;
    }

    /**
     * Get the MessageNumber from the specified SecurityContextToken.
     *
     * @param securityContextToken the token to examine
     * @return the MessageNumber, or null if there wasn't one.
     * @throws XmlUtil.MultipleChildElementsException if there is more than one MessageNumber element in this
     *                                                SecurityContextToken.
     */
    public Long getMessageNumber(Element securityContextToken) throws XmlUtil.MultipleChildElementsException {
        if (securityContextToken == null)
            return null;
        Element mn = XmlUtil.findOnlyOneChildElementByName(securityContextToken, L7_NAMESPACE, MESSAGE_NUMBER_ELNAME);
        if (mn == null)
            return null;
        try {
            return Long.valueOf(XmlUtil.findFirstChildTextNode(mn));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Set the MessageNumber element within the specified SecurityContextToken.
     *
     * @param securityContextToken  the token to update.
     * @param messageNumber  the new message number to set.  Set it to null to delete any existing MessageNumber.
     */
    private void setMessageNumber(Element securityContextToken, Long messageNumber) {
        Document doc = securityContextToken.getOwnerDocument();

        // Remove any existing message number(s)
        XmlUtil.removeChildElementsByName(securityContextToken, L7_NAMESPACE, MESSAGE_NUMBER_ELNAME);

        if (messageNumber == null)
            return;

        // Create a new one
        Element messageNumberEl = doc.createElementNS(L7_NAMESPACE, MESSAGE_NUMBER_ELNAME);
        messageNumberEl.setAttribute("xmlns:" + L7_NAMESPACE_PREFIX, L7_NAMESPACE);
        messageNumberEl.setPrefix(L7_NAMESPACE_PREFIX);
        Text messageNumberText = doc.createTextNode(String.valueOf(messageNumber));
        messageNumberEl.appendChild(messageNumberText);
        securityContextToken.insertBefore(messageNumberEl, null);
    }

    private void appendCreationElement(Element securityCtxTokenEl, long creationTimeStamp) {
        Element createdEl = securityCtxTokenEl.getOwnerDocument().createElementNS(WSU_NAMESPACE, CREATED_ELNAME);
        createdEl.setAttribute("xmlns:" + DEF_WSU_PREFIX, WSU_NAMESPACE);
        createdEl.setPrefix(DEF_WSU_PREFIX);
        String stamp = ISO8601Local.format(new Date(creationTimeStamp));
        Text valNode = securityCtxTokenEl.getOwnerDocument().createTextNode(stamp);
        createdEl.appendChild(valNode);
        securityCtxTokenEl.insertBefore(createdEl, null);
    }

    private void appendIDElement(Element securityCtxTokenEl, byte[] sessionid) {
        String currentwscPrefix = securityCtxTokenEl.getPrefix();
        Element idElement = securityCtxTokenEl.getOwnerDocument().createElementNS(WSC_NAMESPACE, SCTOKEN_ID_ELNAME);
        idElement.setPrefix(currentwscPrefix);
        Text valNode = securityCtxTokenEl.getOwnerDocument().createTextNode(sessionIdToURI(sessionid));
        idElement.appendChild(valNode);
        securityCtxTokenEl.insertBefore(idElement, null);
    }

    /**
     * Gets the WSC:SecurityContextToken element out of the message. If not present, creates it.
     */
    private Element getOrMakeSecurityContextTokenElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(WSC_NAMESPACE, SCTOKEN_ELNAME);
        if (listSecurityElements.getLength() < 1) {
            // element does not exist
            Element securityEl = SoapUtil.getOrMakeSecurityElement(soapMsg);
            if ( securityEl == null ) {
                // Probably not SOAP
                return null;
            }
            Element securityContxTokEl = soapMsg.createElementNS(WSC_NAMESPACE, SCTOKEN_ELNAME);
            securityContxTokEl.setAttribute("xmlns:" + DEF_WSC_NAMESPACE_PREFIX, WSC_NAMESPACE);
            securityContxTokEl.setPrefix(DEF_WSC_NAMESPACE_PREFIX);
            securityEl.insertBefore(securityContxTokEl, null);
            return securityContxTokEl;
        } else {
            return (Element)listSecurityElements.item(0);
        }
    }

    private static final SecureRandom rand = new SecureRandom();
    public static final String URI_PREFIX = "uuid:";
    private static DateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static {
        ISO8601Local.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String L7_NAMESPACE = "http://www.layer7tech.com/ws/security";
    private static final String L7_NAMESPACE_PREFIX = "l7";
    private static final String MESSAGE_NUMBER_ELNAME = "MessageNumber";
    public static final String WSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String SCTOKEN_ELNAME = "SecurityContextToken";
    public static final String SCTOKEN_ID_ELNAME = "Identifier";
    public static final String DEF_WSC_NAMESPACE_PREFIX = "wsc";
    public static final String WSU_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    // Note, the new ws-trust and ws-secure conversation use a different namespace but it looks stupid:
    // "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    public static final String DEF_WSU_PREFIX = "wsu";
    public static final String CREATED_ELNAME = "Created";
    public static final String REQUESTSECURITYTOKEN_SOAPMSG =
            "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<S:Body>" +
                   "<wst:RequestSecurityToken xmlns:wst=\"http://schemas.xmlsoap.org/ws/2004/04/trust\">" +
                       "<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>" +
                   "</wst:RequestSecurityToken>" +
                "</S:Body>" +
            "</S:Envelope>";
}
