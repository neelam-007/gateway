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
    public SoapResponse( TransportMetadata tm ) {
        super(tm);
    }

    public synchronized void setProtectedResponseReader(Reader reader) {
        _responseReader = reader;
    }

    public synchronized Reader getProtectedResponseReader() {
        return _responseReader;
    }

    public synchronized Document getDocument() throws IOException, SAXException {
        parse( getProtectedResponseReader() );
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
    }

    protected List _assertionResults = Collections.EMPTY_LIST;
    protected Reader _responseReader;
    protected Writer _responseWriter;
    protected boolean _authMissing;
}
