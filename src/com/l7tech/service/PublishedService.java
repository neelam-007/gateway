/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.wsdl.WSDLException;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

import org.xml.sax.InputSource;

/**
 * @author alex
 */
public class PublishedService extends NamedEntityImp {
    public Assertion rootAssertion() {
        if ( _rootAssertion == null ) {
            // TODO: Parse the policy
        }
        return _rootAssertion;
    }

    public String getWsdlUrl() {
        return _wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) throws MalformedURLException {
        _wsdlUrl = wsdlUrl;
        _url = new URL( wsdlUrl );
    }

    public String getWsdlXml() throws IOException {
        if ( _wsdlXml == null ) {
            Reader r = new BufferedReader( new InputStreamReader( _url.openStream() ) );
            StringBuffer xml = new StringBuffer();
            char[] buf = new char[4096];
            int num;
            while ( ( num = r.read( buf ) ) != -1 ) {
                xml.append( buf, 0, num );
            }
            _wsdlXml = xml.toString();
        }
        return _wsdlXml;
    }

    public void setWsdlXml( String wsdlXml ) {
        _wsdlXml = wsdlXml;
    }

    public Wsdl parsedWsdl() throws WSDLException {
        if ( _parsedWsdl == null ) {
            try {
                String cachedWsdl = getWsdlXml();
                _parsedWsdl = Wsdl.newInstance( null, new InputSource( new StringReader(cachedWsdl) ) );
            } catch ( IOException ioe ) {
                throw new WSDLException( ioe.getMessage(), ioe.toString(), ioe );
            }
        }
        return _parsedWsdl;
    }

    public String getPolicyXml() {
        return _policyXml;
    }

    public void setPolicyXml( String policyXml ) {
        _policyXml = policyXml;
        // Invalidate stale Root Assertion
        _rootAssertion = null;
    }

    public String getSoapAction() {
        return _soapAction;
    }

    public void setSoapAction(String soapAction) {
        _soapAction = soapAction;
    }

    public String getUrn() {
        return _urn;
    }

    public void setUrn(String urn) {
        _urn = urn;
    }

    public String toString() {
        return "com.l7tech.service.PublishedService _policyXml=" + _policyXml + " _wsdlUrl=" + _wsdlUrl + " _wsdlXml=" + _wsdlXml;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected String _policyXml;
    protected String _wsdlUrl;
    protected String _wsdlXml;
    protected String _soapAction;
    protected String _urn;

    private transient URL _url;
    protected transient Wsdl _parsedWsdl;
    protected transient Assertion _rootAssertion;
}
