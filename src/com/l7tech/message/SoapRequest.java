package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;

import com.l7tech.credential.PrincipalCredentials;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapRequest extends XmlMessageAdapter implements SoapMessage, XmlRequest {
    public SoapRequest( TransportMetadata metadata ) throws IOException {
        super( metadata );
    }

    /**
     * Returns a DOM Document, parsing the requestStream if necessary.  If the requestStream has not already been parsed, this method will begin parsing it and return a "lazy DOM" implementation.
     * @return a "lazy dom" implementation -- hopefully
     * @throws SAXException if some sort of
     * @throws IOException
     */
    public synchronized Document getDocument() throws SAXException, IOException {
        if ( _document == null ) {
            InputStream requestStream = getRequestStream();
            if ( requestStream == null )
                throw new IllegalStateException( "No Document or Reader yet!" );
            else
                parse( requestStream );

        }

        return _document;
    }

    /**
     * Returns a Reader for the request. Could be null!
     *
     * @return The Reader from the request, if any.
     * @throws IOException
     */
    public InputStream getRequestStream() throws IOException {
        if ( _requestStream == null ) {
            if ( _transportMetadata instanceof HttpTransportMetadata ) {
                HttpTransportMetadata htm = (HttpTransportMetadata)_transportMetadata;
                _requestStream = htm.getRequest().getInputStream();
            }
        }
        return _requestStream;
    }

    /** Returns the PrincipalCredentials associated with this request.  Could be null! */
    public PrincipalCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    /** Assigns a set of PrincipalCredentials to this request. */
    public void setPrincipalCredentials( PrincipalCredentials pc ) {
        _principalCredentials = pc;
    }

    /**
     * Returns true if this request has been authenticated.
     * @return true if this request has been authenticated.
     */
    public boolean isAuthenticated() {
        return _authenticated;
    }

    /**
     * Sets this request's authenticated property.
     * @param authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        _authenticated = authenticated;
    }

    public synchronized boolean isRouted() {
        return _routed;
    }

    public synchronized void setRouted(boolean routed) {
        _routed = routed;
    }

    protected boolean _authenticated;
    protected boolean _routed;

    protected PrincipalCredentials _principalCredentials;
    protected InputStream _requestStream;
}
