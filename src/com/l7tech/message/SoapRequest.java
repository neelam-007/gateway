package com.l7tech.message;

import com.l7tech.common.RequestId;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.identity.User;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Encapsulates a SOAP request. Not thread-safe. Don't forget to call close() when you're done!
 *
 * @version $Revision$
 */
public abstract class SoapRequest extends XmlMessageAdapter implements SoapMessage, XmlRequest {
    public SoapRequest( TransportMetadata metadata ) {
        super( metadata );
        _id = RequestIdGenerator.next();
        MessageProcessor.setCurrentRequest(this);
    }

    /**
     * the new valid xml payload for this request
     */
    public void setDocument(Document doc) {
        _document = doc;
        _requestXml = null;
    }

    public RequestId getId() {return _id;}

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

    public XmlPullParser getPullParser() throws IOException, XmlPullParserException {
        return pullParser( getRequestXml() );
    }

    public String getRequestXml() throws IOException {
        // TODO: Attachments
        if (_requestXml == null && _document != null) {
            // serialize the document
            _requestXml = serializeDoc(_document);
        } else if ( _requestXml == null ) {
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

    public LoginCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    public void setPrincipalCredentials( LoginCredentials pc ) {
        _principalCredentials = pc;
    }

    public boolean isAuthenticated() {
        return _authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        _authenticated = authenticated;
    }

    public User getUser() {
        return _user;
    }

    public void setUser( User user ) {
        _user = user;
    }

    public void setRoutingStatus( RoutingStatus status ) {
        _routingStatus = status;
    }

    public RoutingStatus getRoutingStatus() {
        return _routingStatus;
    }

    /**
     * Closes any resources associated with the request.  If you override this
     * method, you MUST call super.close() in your overridden version!
     */
    public void close() {
        MessageProcessor.setCurrentRequest(null);
        try {
            if ( _requestReader != null ) _requestReader.close();
        } catch (IOException e) {
        }
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    protected Reader getRequestReader() throws IOException {
        if ( _requestReader == null )
            _requestReader = doGetRequestReader();
        return _requestReader;
    }

    protected abstract Reader doGetRequestReader() throws IOException;

    protected RequestId _id;
    protected boolean _authenticated;
    protected Reader _requestReader;
    protected User _user;
    protected RoutingStatus _routingStatus = RoutingStatus.NONE;

    /** The cached XML document. */
    protected String _requestXml;
    protected LoginCredentials _principalCredentials;
}
