package com.l7tech.message;

import org.w3c.dom.Document;

import java.io.*;
import java.util.*;

import com.l7tech.policy.assertion.AssertionResult;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapResponse extends XmlMessageAdapter implements SoapMessage, XmlResponse {
    public SoapResponse( TransportMetadata tm ) {
        super(tm);
    }

    public Reader getResponseReader() {
        return _responseReader;
    }

    public Document getDocument() {
        return null;
    }

    public void addResult( AssertionResult result ) {
        if ( _assertionResults.isEmpty() ) _assertionResults = new LinkedList();
        _assertionResults.add( result );
    }

    public Iterator results() {
        return Collections.unmodifiableList( _assertionResults ).iterator();
    }

    protected List _assertionResults = Collections.EMPTY_LIST;
    protected Reader _responseReader;
}
