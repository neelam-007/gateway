package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
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
    public static final String NAMESPACE = "http://l7tech.com/ns/msgid";
    public static final String PREFIX = "l7";

    public static final String SESS_ID_EL_NAME = "SessId";
    public static final String SEQ_EL_NAME = "SeqNr";

    public static final String NONCE_EL_NAME = "Nonce";


    public void appendNonceToDocument(Document soapmsg, long nonce) {
        // todo, refactor to use WSSC tokens
        Element header = SoapUtil.getOrMakeHeader(soapmsg);
        Element nonceEl = soapmsg.createElementNS(NAMESPACE, NONCE_EL_NAME);
        nonceEl.setPrefix(PREFIX);
        nonceEl.setAttribute("xmlns:" + PREFIX, NAMESPACE);
        Text val = soapmsg.createTextNode(Long.toString(nonce));
        nonceEl.appendChild(val);
        header.insertBefore(nonceEl, null);

    }

    public long readNonceFromDocument(Document soapmsg) {
        // todo
        return -1;
    }

    public void appendSessIdAndSeqNrToDocument(Document soapmsg, long sessId, long seqNr) {
        // todo
    }

    public long readSessIdFromDocument(Document soapmsg) {
        // todo
        return -1;
    }

    public long readSeqNrFromDocument(Document soapmsg) {
        // todo
        return -1;
    }

}
