/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.message.Request;

import javax.wsdl.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.InputSource;

/**
 * @author alex
 */
public class PublishedService extends NamedEntityImp {
    public synchronized Assertion getRootAssertion() {
        if ( _rootAssertion == null ) _rootAssertion = WspReader.parse( getPolicyXml() );
        return _rootAssertion;
    }

    public String getWsdlUrl() {
        return _wsdlUrl;
    }

    public synchronized void setWsdlUrl( String wsdlUrl ) throws MalformedURLException {
        if ( _wsdlUrl != null && !_wsdlUrl.equals(wsdlUrl) ) _wsdlXml = null;
        _wsdlUrl = wsdlUrl;
        new URL( wsdlUrl );
    }

    public synchronized String getWsdlXml() throws IOException {
        if ( _wsdlXml == null ) {
            URL url = null;
            try {
                url = new URL(_wsdlUrl);
            } catch ( MalformedURLException mue ) {
                throw new IOException(mue.toString());
            }

            Reader r = null;
            try {
                r = new BufferedReader( new InputStreamReader( url.openStream() ) );
                StringBuffer xml = new StringBuffer();
                char[] buf = new char[4096];
                int num;
                while ( ( num = r.read( buf ) ) != -1 ) {
                    xml.append( buf, 0, num );
                }
                _wsdlXml = xml.toString();
            } finally {
                if ( r != null ) r.close();
            }
        }
        return _wsdlXml;
    }

    public synchronized void setWsdlXml( String wsdlXml ) {
        _wsdlXml = wsdlXml;
        _parsedWsdl = null;
    }

    public synchronized Wsdl getParsedWsdl() throws WSDLException {
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

    public synchronized Port getWsdlPort( Request request ) throws WSDLException {
        // TODO: Get the right port for this request, rather than just the first one!
        if ( _wsdlPort == null ) {
            Iterator services = getParsedWsdl().getServices().iterator();
            Service wsdlService;
            Port wsdlPort = null;

            while ( wsdlPort == null && services.hasNext() ) {
                wsdlService = (Service)services.next();
                Map ports = wsdlService.getPorts();
                if ( ports == null ) continue;

                Iterator portKeys = ports.keySet().iterator();
                if ( portKeys.hasNext() ) wsdlPort = (Port)portKeys.next();
            }
            _wsdlPort = wsdlPort;
        }

        return _wsdlPort;
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

    protected transient Wsdl _parsedWsdl;
    protected transient Port _wsdlPort;
    protected transient Assertion _rootAssertion;
}
