package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapRequest extends XmlMessageAdapter implements SoapMessage, XmlRequest {
    public SoapRequest( InputStream requestStream ) {
        // No document yet
        _requestStream = requestStream;
    }

    public synchronized Document getDocument() throws SAXException, IOException {
        if ( _document == null )
            if ( _requestStream == null )
                throw new IllegalStateException( "No Document or InputStream yet!" );
            else {
                parse( _requestStream );
            }

        return _document;
    }

    public InputStream getRequestStream() {
        return _requestStream;
    }

    protected InputStream _requestStream;
}
