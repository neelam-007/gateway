package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapResponse extends XmlMessageAdapter implements SoapMessage, XmlResponse {
    public static final String ENCODING = "UTF-8";

    public SoapResponse( TransportMetadata tm ) {
        super(tm);
    }

    public synchronized InputStream getProtectedResponseStream() {
        return _responseStream;
    }

    public synchronized void setProtectedResponseStream( InputStream is ) {
        _responseStream = is;
    }


    public synchronized void setResponseXml( String xml ) {
        _responseXml = xml;
    }

    public synchronized String getResponseXml() throws IOException {
        if ( _responseXml == null ) {
            // TODO: Encoding?
            BufferedReader br = new BufferedReader( new InputStreamReader( getProtectedResponseStream(), ENCODING ) );
            StringBuffer result = new StringBuffer();
            String line;
            while ( ( line = br.readLine() ) != null ) {
                result.append( line );
            }
            _responseXml = result.toString();
        }
        return _responseXml;
    }

    public synchronized Document getDocument() throws IOException, SAXException {
        parse( getResponseXml() );
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
        _policyViolated = authMissing;
    }

    public synchronized void runOnClose( Runnable runMe ) {
        if ( _runOnClose == Collections.EMPTY_LIST ) _runOnClose = new ArrayList();
        _runOnClose.add( runMe );
    }

    public synchronized void close() {
        Runnable runMe;
        Iterator i = _runOnClose.iterator();
        while ( i.hasNext() ) {
            runMe = (Runnable)i.next();
            runMe.run();
            i.remove();
        }
    }

    public void finalize() {
        close();
    }

    public boolean isPolicyViolated() {
        return _policyViolated;
    }

    public void setPolicyViolated(boolean policyViolated ) {
        _policyViolated = policyViolated;
    }

    protected List _assertionResults = Collections.EMPTY_LIST;
    protected InputStream _responseStream;
    protected String _responseXml;
    protected boolean _authMissing;
    protected boolean _policyViolated;

    protected List _runOnClose = Collections.EMPTY_LIST;
}
