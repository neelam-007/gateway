package com.l7tech.common.security.xml;

import org.w3c.dom.*;
import com.l7tech.util.SoapUtil;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 10:31:55 AM
 * $Id$
 * <p>
 * Appends or reads nonces, sequence numbers, and session ids from a
 * WS Secure Conversation Token header inside a soap message.
 *
 * <p>
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


    public static void appendNonceToDocument(Document soapmsg, long nonce) {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        writeIdentifierElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(nonce));
    }

    /**
     * Look for a nonce in the soap document in the SecurityContextToken/Identifier element
     * Document is not affected by this method.
     *
     * @param soapmsg the xml document contiaining the soap msg. this document will not be affected by this method
     * @return the nonce in the SecurityContextToken/Identifier element
     * @throws XMLSecurityElementNotFoundException if not present
     */
    public static long readNonceFromDocument(Document soapmsg) throws XMLSecurityElementNotFoundException {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null) {
            throw new XMLSecurityElementNotFoundException(SECURITY_CONTEXT_TOKEN_EL_NAME + " element not present.");
        }
        return Long.parseLong(readIdentifierValueFromSecurityContextToken(secContxTokEl));
    }

    public static void appendSessIdAndSeqNrToDocument(Document soapmsg, long sessId, long seqNr) {
        Element securityCtxTokEl = getOrMakeSecurityContextTokenElement(soapmsg);
        writeIdentifierElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(sessId));
        writeL7SeqElementToSecurityContextTokenElement(securityCtxTokEl, Long.toString(seqNr));
    }

    public static long readSessIdFromDocument(Document soapmsg) throws XMLSecurityElementNotFoundException {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null) {
            throw new XMLSecurityElementNotFoundException(SECURITY_CONTEXT_TOKEN_EL_NAME + " element not present.");
        }
        return Long.parseLong(readIdentifierValueFromSecurityContextToken(secContxTokEl));
    }

    public static long readSeqNrFromDocument(Document soapmsg) throws XMLSecurityElementNotFoundException {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl == null) {
            throw new XMLSecurityElementNotFoundException(SECURITY_CONTEXT_TOKEN_EL_NAME + " element not present.");
        }
        return Long.parseLong(readSeqNrValueFromSecurityContextToken(secContxTokEl));
    }

    /**
     * remove any WS Secure Conversation tokens if present
     */
    public static void consumeSessionInfoFromDocument(Document soapmsg) {
        Element secContxTokEl = getSecurityContextTokenElement(soapmsg);
        if (secContxTokEl != null) secContxTokEl.getParentNode().removeChild(secContxTokEl);
    }

    private static String readSeqNrValueFromSecurityContextToken(Element securityContextTokenEl) throws XMLSecurityElementNotFoundException {
        NodeList listIdentifierElements = securityContextTokenEl.getElementsByTagNameNS(L7_NAMESPACE, SEQ_EL_NAME);
        if (listIdentifierElements.getLength() < 1) {
            throw new XMLSecurityElementNotFoundException(SEQ_EL_NAME + " element not present.");
        }
        Element identifier = (Element)listIdentifierElements.item(0);
        NodeList children = identifier.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                return kid.getNodeValue();
            }
        }
        throw new XMLSecurityElementNotFoundException(SEQ_EL_NAME + " element does not contain value.");
    }

    private static String readIdentifierValueFromSecurityContextToken(Element securityContextTokenEl) throws XMLSecurityElementNotFoundException {
        NodeList listIdentifierElements = securityContextTokenEl.getElementsByTagNameNS(WSU_NAMESPACE, IDENTIFIER_EL_NAME);
        if (listIdentifierElements.getLength() < 1) {
            throw new XMLSecurityElementNotFoundException(IDENTIFIER_EL_NAME + " element not present.");
        }
        Element identifier = (Element)listIdentifierElements.item(0);
        NodeList children = identifier.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node kid = children.item(i);
            if (kid.getNodeType() == Node.TEXT_NODE) {
                return kid.getNodeValue();
            }
        }
        throw new XMLSecurityElementNotFoundException(IDENTIFIER_EL_NAME + " element does not contain value.");
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
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE, SECURITY_CONTEXT_TOKEN_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE2, SECURITY_CONTEXT_TOKEN_EL_NAME);
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
