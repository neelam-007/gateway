package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import org.w3c.dom.*;

/**
 * Appends or reads nonces, sequence numbers, and session ids from a
 * WS Secure Conversation Token header inside a soap message.
 *
 * @deprecated this will be replaced with SecurityContextTokenHandler by 3.0
 *
 * Format for requests :
 * <PRE>
 * &lt;S:Envelope xmlns:S="..." xmlns:ds="..." xmlns:wsse="..."&gt;
 *       &lt;S:Header&gt;
 *               &lt;wsse:Security&gt;
 *                       &lt;wsse:SecurityContextToken&gt;
 *                               &lt;wsu:Identifier&gt;sessionid&lt;/wsu:Identifier&gt;
 *                               &lt;l7:SeqNr xmlns:l7="http://l7tech.com/ns/msgid"&gt;seqNr&lt;/wsu:SeqNr&gt;
 *                       &lt;/wsse:SecurityContextToken&gt;
 *                       &lt;ds:Signature&gt;
 *                               ...
 *                       &lt;/ds:Signature&gt;
 *               &lt;/wsse:Security&gt;
 *       &lt;/S:Header&gt;
 *       &lt;S:Body&gt;
 *               &lt;tru:StockSymbol xmlns:tru="http://fabrikam123.com/payloads"&gt;
 *                       QQQ
 *               &lt;/tru:StockSymbol&gt;
 *       &lt;/S:Body&gt;
 * &lt;/S:Envelope&gt;
 * </PRE>
 *
 * Format for responses :
 * <PRE>
 * &lt;S:Envelope xmlns:S="..." xmlns:ds="..." xmlns:wsse="..."&gt;
 *       &lt;S:Header&gt;
 *               &lt;wsse:Security&gt;
 *                       &lt;wsse:SecurityContextToken&gt;
 *                               &lt;wsu:Identifier&gt;noncenr&lt;/wsu:Identifier&gt;
 *                       &lt;/wsse:SecurityContextToken&gt;
 *                       &lt;ds:Signature&gt;
 *                               ...
 *                       &lt;/ds:Signature&gt;
 *               &lt;/wsse:Security&gt;
 *       &lt;/S:Header&gt;
 *       &lt;S:Body&gt;
 *               &lt;tru:StockSymbol xmlns:tru="http://fabrikam123.com/payloads"&gt;
 *                       QQQ
 *               &lt;/tru:StockSymbol&gt;
 *       &lt;/S:Body&gt;
 * &lt;/S:Envelope&gt;
 * </PRE>
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 28, 2003<br/>
 * Time: 10:31:55 AM<br/>
 * $Id$
 */
public class SecureConversationTokenHandler {

    public static final String L7_NAMESPACE = "http://l7tech.com/ns/msgseqnr";
    public static final String L7_NAMESPACE_PREFIX = "l7";
    public static final String SEQ_EL_NAME = "SeqNr";

    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";

    public static final String WSU_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSU_NAMESPACE_PREFIX = "wsu";

    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";

    public static final String SECURITY_CONTEXT_TOKEN_EL_NAME = "SecurityContextToken";

    public static final String IDENTIFIER_EL_NAME = "Identifier";


    public static void appendNonceToDocument(Document soapmsg, long nonce) throws MessageNotSoapException {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        if ( securityCtxTokEl == null ) throw new MessageNotSoapException("Can't add nonce to non-SOAP message");
        writeIdentifierElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(nonce));
    }

    /**
     * Look for a nonce in the soap document in the SecurityContextToken/Identifier element, and if it is found, remove the SecurityContextToken.
     * Document IS affected by this method.
     *
     * @param soapmsg the xml document contiaining the soap msg. this document will not be affected by this method
     * @return the nonce in the SecurityContextToken/Identifier element
     */
    public static Long takeNonceFromDocument(Document soapmsg) {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null)
            return null;

        String nonceValue = readIdentifierValueFromSecurityContextToken(secContxTokEl);

        secContxTokEl.getParentNode().removeChild( secContxTokEl );

        return nonceValue == null ? null : new Long(nonceValue);
    }

    public static void appendSessIdAndSeqNrToDocument(Document soapmsg, long sessId, long seqNr) throws MessageNotSoapException {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        if ( securityCtxTokEl == null ) throw new MessageNotSoapException("Can't add sequence number to non-SOAP message");
        writeIdentifierElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(sessId));
        writeL7SeqElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(seqNr));
    }

    /**
     * @return null if the document contains no XML Security Element(s)
     */
    public static Long readSessIdFromDocument(Document soapmsg) {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null) {
            return null;
        }
        String val = readIdentifierValueFromSecurityContextToken(secContxTokEl);
        return val == null ? null : new Long(val);
    }

    /**
     * @return null if the document
     */
    public static Long readSeqNrFromDocument(Document soapmsg) {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null) {
            return null;
        }
        String val = readSeqNrValueFromSecurityContextToken(secContxTokEl);
        return val == null ? null : new Long(val);
    }

    /**
     * remove any WS Secure Conversation tokens if present
     */
    public static void consumeSessionInfoFromDocument(Document soapmsg) {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl != null) secContxTokEl.getParentNode().removeChild(secContxTokEl);
    }

    /**
     * @return null if the document does not contain any Security element(s)
     */
    private static String readSeqNrValueFromSecurityContextToken(Element securityContextTokenEl) {
        NodeList listIdentifierElements = securityContextTokenEl.getElementsByTagNameNS(L7_NAMESPACE, SEQ_EL_NAME);
        if (listIdentifierElements.getLength() < 1)
            return null;
        Element identifier = (Element)listIdentifierElements.item(0);
        NodeList children = identifier.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                return kid.getNodeValue();
            }
        }
        return null;
    }

    /**
     * @param securityContextTokenEl
     * @return null if the document contains no XML Security Element
     */
    private static String readIdentifierValueFromSecurityContextToken(Element securityContextTokenEl) {
        NodeList listIdentifierElements = securityContextTokenEl.getElementsByTagNameNS(WSU_NAMESPACE, IDENTIFIER_EL_NAME);
        if (listIdentifierElements.getLength() < 1)
            return null;

        Element identifier = (Element)listIdentifierElements.item(0);
        NodeList children = identifier.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                return kid.getNodeValue();
            }
        }
        return null;
    }

    private static void writeIdentifierElementToSecurityContextTokenElement(Element securityContextTokenEl, String value) {
        Element idElement = securityContextTokenEl.getOwnerDocument().createElementNS(WSU_NAMESPACE, IDENTIFIER_EL_NAME);
        idElement.setPrefix(WSU_NAMESPACE_PREFIX);
        idElement.setAttribute("xmlns:" + WSU_NAMESPACE_PREFIX, WSU_NAMESPACE);
        Text valNode = securityContextTokenEl.getOwnerDocument().createTextNode(value);
        idElement.appendChild(valNode);
        securityContextTokenEl.insertBefore(idElement, null);
    }

    private static void writeL7SeqElementToSecurityContextTokenElement(Element securityContextTokenEl, String value) {
        Element idElement = securityContextTokenEl.getOwnerDocument().createElementNS(L7_NAMESPACE, SEQ_EL_NAME);
        idElement.setPrefix(L7_NAMESPACE_PREFIX);
        idElement.setAttribute("xmlns:" + L7_NAMESPACE_PREFIX, L7_NAMESPACE);
        Text valNode = securityContextTokenEl.getOwnerDocument().createTextNode(value);
        idElement.appendChild(valNode);
        securityContextTokenEl.insertBefore(idElement, null);
    }

    private static Element getSecurityContextTokenElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE,
                                                                       SECURITY_CONTEXT_TOKEN_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE2,
                                                                  SECURITY_CONTEXT_TOKEN_EL_NAME);
        }

        if (listSecurityElements.getLength() < 1) {
            return null;
        } else {
            return (Element)listSecurityElements.item(0);
        }
    }

    private static Element getOrMakeSecurityContextTokenElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE, SECURITY_CONTEXT_TOKEN_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE2, SECURITY_CONTEXT_TOKEN_EL_NAME);
        }
        if (listSecurityElements.getLength() < 1) {
            // element does not exist
            Element securityEl = SoapUtil.getOrMakeSecurityElement(soapMsg);
            if ( securityEl == null ) {
                // Probably not SOAP
                return null;
            }

            Element securityContxTokEl = soapMsg.createElementNS(SoapUtil.SECURITY_NAMESPACE, SECURITY_CONTEXT_TOKEN_EL_NAME);
            // use same prefix as parent
            securityContxTokEl.setPrefix(securityEl.getPrefix());
            securityEl.insertBefore(securityContxTokEl, null);
            return securityContxTokEl;
        } else {
            return (Element)listSecurityElements.item(0);
        }
    }
}
