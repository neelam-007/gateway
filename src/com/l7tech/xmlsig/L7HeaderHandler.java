package com.l7tech.xmlsig;

import org.w3c.dom.Document;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 10:31:55 AM
 * $Id$
 *
 * Appends and reads l7 headers from soap messages.
 *
 */
public class L7HeaderHandler {
    public static final String NAMESPACE = "http://l7tech.com/ns/msgid";
    public static final String PREFIX = "l7";

    public static final String SESS_ID_EL_NAME = "SessId";
    public static final String SEQ_EL_NAME = "SeqNr";

    public static final String NONCE_EL_NAME = "Nonce";


    public void appendNonceToDocument(Document soapmsg) {
        // todo
    }

    public long readNonceFromDocument(Document soapmsg) {
        // todo
        return -1;
    }

    public void appendSessIdAndSeqNrToDocument(Document soapmsg) {
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
