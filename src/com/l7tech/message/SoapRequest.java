package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.IOException;

import com.l7tech.credential.PrincipalCredentials;

import javax.servlet.http.HttpServletRequest;

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
     * @return a "lazy dom" implementation
     * @throws SAXException if some sort of
     * @throws IOException
     */
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

    /** Returns the PrincipalCredentials associated with this request.  Could be null! */
    public PrincipalCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    /** Assigns a set of PrincipalCredentials to this request. */
    public void setPrincipalCredentials( PrincipalCredentials pc ) {
        _principalCredentials = pc;
    }

    public boolean isAuthenticated() {
        return _authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        _authenticated = authenticated;
    }

    public Object getParameter( Object name ) {
        // TODO: Get from _axisEnvelope?
        return null;
    }

    protected HttpServletRequest _request;
    protected boolean _authenticated;
    protected PrincipalCredentials _principalCredentials;
    protected InputStream _requestStream;
}
