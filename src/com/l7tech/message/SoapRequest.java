package com.l7tech.message;

import com.l7tech.cluster.DistributedMessageIdManager;
import com.l7tech.common.RequestId;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.util.MessageIdManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

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

    // todo why is this here and not in one of the superinterfaces?  why would there be a "SoapRequest" instance that wasn't soap?
    public boolean isSoap() {
        if ( soap == null ) {
            Element docEl = null;

            boolean ok;
            try {
                docEl = getDocument().getDocumentElement();
                ok = true;
            } catch ( Exception e ) {
                ok = false;
            }

            ok = ok && docEl.getNodeName().equals(SoapUtil.BODY_EL_NAME);
            if ( ok ) {
                String docUri = docEl.getNamespaceURI();

                // Check that envelope is one of the recognized namespaces
                for ( Iterator i = SoapUtil.ENVELOPE_URIS.iterator(); i.hasNext(); ) {
                    String envUri = (String)i.next();
                    if (envUri.equals(docUri)) ok = true;
                }
            }

            soap = ok ? Boolean.TRUE : Boolean.FALSE;
        }

        return soap.booleanValue();
    }


    public MessageIdManager getMessageIdManager() {
        return DistributedMessageIdManager.getInstance();
    }

    public WssProcessor.ProcessorResult getWssProcessorOutput() {
        return wssRes;
    }

    public void setWssProcessorOutput(WssProcessor.ProcessorResult res) {
        wssRes = res;
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
            _requestXml = XmlUtil.nodeToString(_document);
        } else if ( _requestXml == null ) {
            // multipart/related; type="text/xml"; boundary="----=Multipart-SOAP-boundary=----"
            _requestXml = getMessageXml(getRequestReader());
        }
        return _requestXml;
    }

    public Map getRequestAttachments() throws IOException {

        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
         return multipartReader.getMessageAttachments();
    }

    public Part getRequestAttachment(int position) throws IOException {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
        return multipartReader.getMessagePart(position);
    }

    public Part getSoapPart() throws IOException {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
        return multipartReader.getMessagePart(0);
    }
    
    public String getMultipartBoundary() {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
        return multipartReader.multipartBoundary;
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

    public boolean isMultipart() {
        return multipart;
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

//    private static final MessageIdManager _messageIdManager = SingleNodeMessageIdManager.getInstance();

    protected abstract Reader doGetRequestReader() throws IOException;

    protected RequestId _id;
    protected boolean _authenticated;
    protected Boolean soap = null;
    protected Reader _requestReader;
    protected User _user;
    protected RoutingStatus _routingStatus = RoutingStatus.NONE;

    /** The cached XML document. */
    protected String _requestXml;
    protected LoginCredentials _principalCredentials;
    
    protected WssProcessor.ProcessorResult wssRes = null;

}
