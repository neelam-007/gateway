package com.l7tech.message;

import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.RequestIdGenerator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;

/**
 * Encapsulates a SOAP request. Not thread-safe. Don't forget to call close() when you're done!
 *
 * @version $Revision$
 */
public abstract class SoapRequest extends XmlMessageAdapter implements SoapMessage, XmlRequest {
    public SoapRequest( TransportMetadata metadata ) throws IOException {
        super( metadata );
        _id = RequestIdGenerator.next();
        MessageProcessor.setCurrentRequest(this);
    }

    /**
     * Returns a DOM Document, getting the requestXml and thereby consuming the
     * requestStream if necessary.
     *
     * @return a "lazy dom" implementation -- hopefully
     * @throws SAXException if an exception occurs during parsing
     * @throws IOException if the whole document can't be read due to an IOException
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
        _document = null;
    }

    public PrincipalCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    public void setPrincipalCredentials( PrincipalCredentials pc ) {
        _principalCredentials = pc;
    }

    public boolean isAuthenticated() {
        return _authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        _authenticated = authenticated;
    }

    public synchronized boolean isRouted() {
        return _routed;
    }

    public synchronized void setRouted(boolean routed) {
        _routed = routed;
    }

    /**
     * Closes any resources associated with the request.  If you override this method, you MUST call super.close() in your overridden version!
     */
    public void close() {
        MessageProcessor.setCurrentRequest(null);
        try {
            if ( _requestReader != null ) _requestReader.close();
        } catch (IOException e) {
        }
    }

    public void finalize() {
        close();
    }

    protected Reader getRequestReader() throws IOException {
        if ( _requestReader == null )
            _requestReader = doGetRequestReader();
        return _requestReader;
    }

    protected abstract Reader doGetRequestReader() throws IOException;

    protected BigInteger _id;
    protected boolean _authenticated;
    protected boolean _routed;
    protected Reader _requestReader;

    /** The cached XML document. */
    protected String _requestXml;
    protected PrincipalCredentials _principalCredentials;
}
