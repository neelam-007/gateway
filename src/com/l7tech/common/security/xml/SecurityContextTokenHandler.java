package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

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

    private SecurityContextTokenHandler() {}

    /**
     * Generates a new random session id of 16 bytes.
     * @return
     */
    public static byte[] generateNewSessionId() {
        byte[] sessionid = new byte[SESSION_BYTES];
        rand.nextBytes(sessionid);
        return sessionid;
    }

    /**
     * Converts raw session id to its URI representation.
     * @param rawSessionId a session id 16 bytes long
     * @return URI representation (looks like uuid:base64edrawsessionid)
     */
    public static String sessionIdToURI(byte[] rawSessionId) {
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
    public static Document createNewRequestSecurityToken() throws IOException, SAXException {
        return XmlUtil.stringToDocument(REQUESTSECURITYTOKEN_SOAPMSG);
    }

    /**
     * does the reverse of sessionIdToURI
     * @param uri URI representation (looks like uuid:base64edrawsessionid)
     * @return a 16 bytes session id
     */
    public static byte[] URIToSessionId(String uri) throws IOException {
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
    public static void appendSessionInfoToSoapMessage(Document soapmsg, byte[] sessionid, long seqnumber,
                                                      long creationtimestamp) {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        appendIDElement(securityCtxTokEl, sessionid);
        appendSeqElement(securityCtxTokEl, seqnumber);
        appendCreationElement(securityCtxTokEl, creationtimestamp);
    }

    /**
     * Attempts to extract the session id from a wsse:Security/wsc:SecurityContextToken/wsc:Identifier element
     * @return null if not present
     */
    public static byte[] getSessionIdFromWSCToken(Document soapMsg) throws IOException {
        // get the element
        NodeList listIdElements = soapMsg.getElementsByTagNameNS(WSC_NAMESPACE, SCTOKEN_ID_ELNAME);
        if (listIdElements.getLength() < 1) {
            return null;
        }
        Element idel = (Element)listIdElements.item(0);
        // get its text child
        StringBuffer childText = new StringBuffer();
        NodeList children = idel.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                childText.append(kid.getNodeValue());
            }
        }
        // convert back to session id
        return URIToSessionId(childText.toString());
    }

    public static long getSequenceNumberFromWSCToken(Document soapMsg) {
        // todo
        return -1;
    }

    private static void appendSeqElement(Element securityCtxTokenEl, long seqnumber) {
        Document doc = securityCtxTokenEl.getOwnerDocument();
        Element messageNumberEl = doc.createElementNS(L7_NAMESPACE, MESSAGE_NUMBER_ELNAME);
        messageNumberEl.setAttribute("xmlns:" + L7_NAMESPACE_PREFIX, L7_NAMESPACE);
        messageNumberEl.setPrefix(L7_NAMESPACE_PREFIX);
        Text messageNumberText = doc.createTextNode(String.valueOf(seqnumber));
        messageNumberEl.appendChild(messageNumberText);
        securityCtxTokenEl.insertBefore(messageNumberEl, null);
    }

    private static void appendCreationElement(Element securityCtxTokenEl, long creationTimeStamp) {
        Element createdEl = securityCtxTokenEl.getOwnerDocument().createElementNS(WSU_NAMESPACE, CREATED_ELNAME);
        createdEl.setAttribute("xmlns:" + DEF_WSU_PREFIX, WSU_NAMESPACE);
        createdEl.setPrefix(DEF_WSU_PREFIX);
        String stamp = ISO8601Local.format(new Date(creationTimeStamp));
        Text valNode = securityCtxTokenEl.getOwnerDocument().createTextNode(stamp);
        createdEl.appendChild(valNode);
        securityCtxTokenEl.insertBefore(createdEl, null);
    }

    private static void appendIDElement(Element securityCtxTokenEl, byte[] sessionid) {
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
    private static Element getOrMakeSecurityContextTokenElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(WSC_NAMESPACE, SCTOKEN_ELNAME);
        if (listSecurityElements.getLength() < 1) {
            // element does not exist
            Element securityEl = SoapUtil.getOrMakeSecurityElement(soapMsg);
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
