package com.l7tech.message;

import org.w3c.dom.Document;

import java.io.*;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapResponse extends XmlMessageAdapter implements SoapMessage, XmlResponse {
    public SoapResponse( TransportMetadata tm ) {
        super(tm);
    }

    public Reader getResponseReader() {
        return _responseReader;
    }

    public Document getDocument() {
        return null;
    }

    protected Reader _responseReader;
}
