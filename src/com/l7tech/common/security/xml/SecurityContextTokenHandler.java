package com.l7tech.common.security.xml;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

import java.security.SecureRandom;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import com.l7tech.common.util.SoapUtil;

/**
 * Appends and parses out xml security session information in/out of soap messages.
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

    /**
     * Generates a new random session id of 16 bytes.
     * @return
     */
    public static byte[] generateNewSessionId() {
        byte[] sessionid = new byte[16];
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
        if (rawSessionId == null || rawSessionId.length != 16) {
            throw new IllegalArgumentException("This is not a proper sessionid");
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return URI_PREFIX + encoder.encode(rawSessionId);
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
     */
    public static void appendSessionInfoToSoapMessage(Document soapmsg, byte[] sessionid, long seqnumber) {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        String currentwscPrefix = securityCtxTokEl.getPrefix();
        Element idElement = securityCtxTokEl.getOwnerDocument().createElementNS(WSC_NAMESPACE, SCTOKEN_ID_ELNAME);
        idElement.setPrefix(currentwscPrefix);
        Text valNode = securityCtxTokEl.getOwnerDocument().createTextNode(sessionIdToURI(sessionid));
        idElement.appendChild(valNode);
        securityCtxTokEl.insertBefore(idElement, null);
        // todo, the sequence number
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

    public static final String WSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String SCTOKEN_ELNAME = "SecurityContextToken";
    public static final String SCTOKEN_ID_ELNAME = "Identifier";
    public static final String DEF_WSC_NAMESPACE_PREFIX = "wsc";
}
