package com.l7tech.message;

import com.l7tech.credential.PrincipalCredentials;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

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
            String xml = getRequestXml();
            if ( xml == null )
                throw new IllegalStateException( "No XML yet!" );
            else
                parse( xml );

        }

        return _document;
    }

   private Reader getRequestReader() throws IOException {
        if ( _transportMetadata instanceof HttpTransportMetadata ) {
            HttpTransportMetadata htm = (HttpTransportMetadata)_transportMetadata;
            return htm.getRequest().getReader();
        } else throw new IllegalStateException( "I don't know how to get a Reader from a non-HTTP TransportMetadata!" );
    }

    public String getRequestXml() throws IOException {
        // TODO: Attachments
        if ( _requestXml == null ) {
            BufferedReader reader = new BufferedReader( getRequestReader() );
            StringBuffer xml = new StringBuffer();
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                xml.append( line );
            }
            _requestXml = xml.toString();
        }
        return _requestXml;
    }

    public void setRequestXml( String xml ) {
        _requestXml = xml;
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

    protected String _requestXml;
    protected PrincipalCredentials _principalCredentials;
}
