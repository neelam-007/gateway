package com.l7tech.message;

import org.w3c.dom.Document;

import java.io.OutputStream;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapResponse extends XmlMessageAdapter implements SoapMessage, XmlResponse {
    public SoapResponse( TransportMetadata tm ) {
        super(tm);
    }

    public OutputStream getResponseStream() {
        return _responseStream;
    }

    public Document getDocument() {
        return null;
    }

    protected OutputStream _responseStream;
}
