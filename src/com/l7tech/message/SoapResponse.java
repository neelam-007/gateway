package com.l7tech.message;

import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Encapsulates a SOAP response.  Not thread-safe.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class SoapResponse extends XmlMessageAdapter implements SoapMessage, Response {
    public static final String ENCODING = "UTF-8";

    public SoapResponse( TransportMetadata tm ) {
        super(tm);
        MessageProcessor.setCurrentResponse( this );
    }

    public synchronized void setXml( String xml ) {
        _responseXml = xml;
        _document = null;
    }

    // TODO check handling of attachments; perhaps return stream here instead of String
    public synchronized String getXml() throws IOException {
        if (_responseXml == null && _document != null) {
            // serialize the document
            _responseXml = XmlUtil.nodeToString(_document);
        } else if ( _responseXml == null ) {
            // TODO: Encoding?
            InputStream protectedResponseStream = getInputStream();
            if ( protectedResponseStream != null ) {
                _responseXml = getMessageXml(protectedResponseStream, null);
            }
        }
        return _responseXml;
    }

    public void setDocument(Document doc) {
        _document = doc;
        _responseXml = null;
    }

    public synchronized Document getDocument() throws IOException, SAXException {
        if (_document == null) {
            String xml = getXml();
            if (xml == null || xml.length() == 0) {
                throw new NoDocumentPresentException();
            } else {
                _document = XmlUtil.stringToDocument(xml);
            }
        }
        return _document;
    }

    public synchronized void addResult( AssertionResult result ) {
        if ( _assertionResults.isEmpty() ) _assertionResults = new LinkedList();
        _assertionResults.add( result );
    }

    public synchronized Iterator resultsWithStatus( AssertionStatus status ) {
        return resultsWithStatus( new AssertionStatus[] { status } );
    }

    public synchronized Iterator resultsWithStatus( AssertionStatus[] statuses ) {
        List results = new LinkedList();
        AssertionResult result;
        for ( int i = 0; i < _assertionResults.size(); i++ ) {
            result = (AssertionResult)_assertionResults.get(i);

            for ( int j = 0; j < statuses.length; j++ )
                if ( statuses[j] == result.getStatus() ) results.add( result );

        }
        return Collections.unmodifiableList(results).iterator();
    }

    public synchronized Iterator results() {
        return Collections.unmodifiableList( _assertionResults ).iterator();
    }

    public boolean isAuthenticationMissing() {
        return _authMissing;
    }

    public void setAuthenticationMissing( boolean authMissing ) {
        _authMissing = authMissing;
        if ( authMissing ) _policyViolated = authMissing;
    }

    public synchronized void close() {
        super.close();
        MessageProcessor.setCurrentResponse(null);
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public boolean isPolicyViolated() {
        return _policyViolated;
    }

    public void setPolicyViolated(boolean policyViolated ) {
        _policyViolated = policyViolated;
    }

    /** @return WSS decoration requirements for this response, or null if there aren't any. */
    public DecorationRequirements getDecorationRequirements() {
        return _decorationRequirements;
    }

    /** Set the decoration requirements for this response.  Set to null to prevent WSS decoration. */
    public void setDecorationRequirements(DecorationRequirements requirements) {
        _decorationRequirements = requirements;
    }

    /** @return new or existing WSS decoration requirements for this message.  never null. */
    public DecorationRequirements getOrMakeDecorationRequirements() {
        if (_decorationRequirements != null) return _decorationRequirements;
        return _decorationRequirements = new DecorationRequirements();
    }

    /**
     * Specify the fault details for an error that occured in this request.
     */
    public void setFaultDetail(SoapFaultDetail sfd) {
        fault = sfd;
    }

    /**
     * Get the soap fault details if applicable.
     */
    public SoapFaultDetail getFaultDetail() {
        return fault;
    }

    private List _assertionResults = Collections.EMPTY_LIST;
    private String _responseXml;
    private boolean _authMissing;
    private boolean _policyViolated;
    private DecorationRequirements _decorationRequirements = null;

    private SoapFaultDetail fault = null;
}
