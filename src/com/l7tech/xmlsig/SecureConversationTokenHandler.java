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
 *
 * Appends or reads nonces, sequence numbers, and session ids from a
 * WS Secure Conversation Token header inside a soap message.
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
